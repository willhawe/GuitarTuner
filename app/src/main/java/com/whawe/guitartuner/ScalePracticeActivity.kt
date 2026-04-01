package com.whawe.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.whawe.guitartuner.audio.AudioConfig
import com.whawe.guitartuner.audio.AudioRecordFactory
import com.whawe.guitartuner.scale.MusicalScale
import com.whawe.guitartuner.scale.RootNote
import com.whawe.guitartuner.scale.ScaleLibrary
import com.whawe.guitartuner.scale.ScaleStaveView
import com.whawe.guitartuner.scale.ScaleType
import com.whawe.guitartuner.tuning.FrequencySmoother
import com.whawe.guitartuner.tuning.NoteMapper
import com.whawe.guitartuner.tuning.PitchDetector

class ScalePracticeActivity : AppCompatActivity() {
    private lateinit var rootNoteSpinner: Spinner
    private lateinit var scaleTypeSpinner: Spinner
    private lateinit var scaleStaveView: ScaleStaveView
    private lateinit var currentScaleTextView: TextView
    private lateinit var currentNoteTextView: TextView
    private lateinit var playedNotesTextView: TextView
    private lateinit var scaleStatusTextView: TextView
    private lateinit var resetScaleButton: Button

    private var selectedRoot = RootNote.A
    private var selectedScaleType = ScaleType.MINOR_PENTATONIC
    private var activeScale: MusicalScale = ScaleLibrary.buildScale(selectedRoot, selectedScaleType)
    private val highlightedPitchClasses = mutableMapOf<Int, Long>()
    private val frequencySmoother = FrequencySmoother()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentDetectedPitchClass: Int? = null
    private val highlightExpiryRunnable = Runnable {
        pruneExpiredHighlights()
        updatePlayedNotesSummary()
        scaleStaveView.setHighlightState(highlightedPitchClasses.keys, currentDetectedPitchClass)
        scheduleHighlightExpiryRefresh()
    }

    @Volatile
    private var isListening = false

    private var recordingThread: Thread? = null
    private var audioRecord: AudioRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scale_practice)

        initializeViews()
        setupSpinners()
        setupButtons()
        renderScale()
        renderIdleState()
    }

    override fun onStart() {
        super.onStart()
        beginListeningIfPossible()
    }

    override fun onStop() {
        if (isListening) {
            stopListening()
        }
        super.onStop()
    }

    private fun initializeViews() {
        rootNoteSpinner = findViewById(R.id.rootNoteSpinner)
        scaleTypeSpinner = findViewById(R.id.scaleTypeSpinner)
        scaleStaveView = findViewById(R.id.scaleStaveView)
        currentScaleTextView = findViewById(R.id.currentScaleTextView)
        currentNoteTextView = findViewById(R.id.currentNoteTextView)
        playedNotesTextView = findViewById(R.id.playedNotesTextView)
        scaleStatusTextView = findViewById(R.id.scaleStatusTextView)
        resetScaleButton = findViewById(R.id.resetScaleButton)
    }

    private fun setupSpinners() {
        val rootNotes = RootNote.values().toList()
        val scaleTypes = ScaleType.values().toList()

        rootNoteSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            rootNotes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        scaleTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            scaleTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        rootNoteSpinner.setSelection(rootNotes.indexOf(selectedRoot))
        scaleTypeSpinner.setSelection(scaleTypes.indexOf(selectedScaleType))

        rootNoteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedRoot = rootNotes[position]
                updateScaleSelection()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        scaleTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedScaleType = scaleTypes[position]
                updateScaleSelection()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupButtons() {
        resetScaleButton.setOnClickListener {
            clearPlayedNotes()
            if (!isListening) {
                renderIdleState()
            }
        }
    }

    private fun updateScaleSelection() {
        activeScale = ScaleLibrary.buildScale(selectedRoot, selectedScaleType)
        clearPlayedNotes()
        renderScale()
        if (checkAudioPermission() && !isListening) {
            startListening()
        } else if (!isListening) {
            renderIdleState()
        }
    }

    private fun renderScale() {
        scaleStaveView.setScale(activeScale)
        currentScaleTextView.text = getString(
            R.string.scale_current_scale_format,
            activeScale.root.displayName,
            activeScale.type.displayName
        )
        updatePlayedNotesSummary()
    }

    private fun renderIdleState() {
        currentDetectedPitchClass = null
        currentNoteTextView.text = getString(R.string.scale_current_note_idle)
        currentNoteTextView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        scaleStatusTextView.text = getString(R.string.scale_status_idle)
        pruneExpiredHighlights()
        scaleStaveView.setHighlightState(highlightedPitchClasses.keys, null)
    }

    private fun clearPlayedNotes() {
        highlightedPitchClasses.clear()
        mainHandler.removeCallbacks(highlightExpiryRunnable)
        updatePlayedNotesSummary()
        scaleStaveView.setHighlightState(highlightedPitchClasses.keys, currentDetectedPitchClass)
    }

    private fun pruneExpiredHighlights(now: Long = SystemClock.elapsedRealtime()) {
        highlightedPitchClasses.entries.removeAll { it.value <= now }
    }

    private fun markHighlight(pitchClass: Int) {
        highlightedPitchClasses[pitchClass] = SystemClock.elapsedRealtime() + NOTE_HIGHLIGHT_DURATION_MS
        scheduleHighlightExpiryRefresh()
    }

    private fun scheduleHighlightExpiryRefresh() {
        mainHandler.removeCallbacks(highlightExpiryRunnable)
        val nextExpiry = highlightedPitchClasses.values.minOrNull() ?: return
        val delay = (nextExpiry - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        mainHandler.postDelayed(highlightExpiryRunnable, delay)
    }

    private fun refreshScaleHighlights() {
        pruneExpiredHighlights()
        updatePlayedNotesSummary()
        scaleStaveView.setHighlightState(highlightedPitchClasses.keys, currentDetectedPitchClass)
    }

    private fun updatePlayedNotesSummary() {
        pruneExpiredHighlights()
        val playedLabels = activeScale.notes
            .distinctBy { it.pitchClass }
            .mapNotNull { note ->
                if (highlightedPitchClasses.containsKey(note.pitchClass)) {
                    NoteMapper.noteNameForPitchClass(note.pitchClass)
                } else {
                    null
                }
            }

        playedNotesTextView.text = if (playedLabels.isEmpty()) {
            getString(R.string.scale_played_notes_placeholder)
        } else {
            getString(R.string.scale_played_notes_format, playedLabels.joinToString(", "))
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        if (!checkAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun beginListeningIfPossible() {
        if (checkAudioPermission()) {
            startListening()
        } else {
            requestAudioPermission()
        }
    }

    private fun startListening() {
        if (isListening) {
            return
        }

        if (!checkAudioPermission()) {
            requestAudioPermission()
            return
        }

        frequencySmoother.clear()
        isListening = true
        scaleStatusTextView.text = getString(R.string.scale_status_listening)

        try {
            val recordResult = createAudioRecord()
            if (recordResult == null) {
                isListening = false
                scaleStatusTextView.text = getString(R.string.scale_status_audio_error)
                return
            }

            val (record, config) = recordResult
            audioRecord = record
            record.startRecording()

            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                AudioRecordFactory.release(record)
                audioRecord = null
                isListening = false
                scaleStatusTextView.text = getString(R.string.scale_status_audio_error)
                return
            }

            recordingThread = Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(config.analysisSize)
                var consecutiveNoSignal = 0

                while (isListening) {
                    val readSize = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (readSize <= 0) {
                        continue
                    }

                    val detectedFrequency = PitchDetector.detectPitch(buffer, readSize, config.sampleRate)
                    if (detectedFrequency <= 0f) {
                        consecutiveNoSignal++
                        if (consecutiveNoSignal > NO_SIGNAL_THRESHOLD) {
                            runOnUiThread {
                                currentDetectedPitchClass = null
                                currentNoteTextView.text = getString(R.string.scale_current_note_idle)
                                currentNoteTextView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                                scaleStatusTextView.text = getString(R.string.scale_status_listening)
                                refreshScaleHighlights()
                            }
                        }
                        continue
                    }

                    consecutiveNoSignal = 0
                    val smoothedFrequency = frequencySmoother.add(detectedFrequency)
                    val noteMatch = NoteMapper.findClosestNote(smoothedFrequency)
                    val pitchClass = NoteMapper.pitchClassForNoteName(noteMatch.name)

                    runOnUiThread {
                        val inScale = pitchClass != null && activeScale.containsPitchClass(pitchClass)
                        currentDetectedPitchClass = pitchClass
                        if (pitchClass != null && inScale) {
                            markHighlight(pitchClass)
                        }
                        currentNoteTextView.text = getString(
                            R.string.scale_current_note_format,
                            noteMatch.name,
                            smoothedFrequency
                        )
                        currentNoteTextView.setTextColor(
                            ContextCompat.getColor(
                                this,
                                if (inScale) R.color.tuner_green else R.color.tuner_orange
                            )
                        )
                        scaleStatusTextView.text = getString(
                            if (inScale) R.string.scale_status_in_scale else R.string.scale_status_out_of_scale,
                            noteMatch.name
                        )
                        refreshScaleHighlights()
                    }
                }
            }
            recordingThread?.start()
        } catch (_: Exception) {
            isListening = false
            scaleStatusTextView.text = getString(R.string.scale_status_audio_error)
        }
    }

    private fun stopListening() {
        isListening = false
        recordingThread?.interrupt()
        recordingThread = null

        mainHandler.removeCallbacks(highlightExpiryRunnable)
        highlightedPitchClasses.clear()
        audioRecord?.let { AudioRecordFactory.release(it) }
        audioRecord = null
        frequencySmoother.clear()
        renderIdleState()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(): Pair<AudioRecord, AudioConfig>? {
        return AudioRecordFactory.create()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
            startListening()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            scaleStatusTextView.text = getString(R.string.scale_status_permission_required)
        }
    }

    override fun onDestroy() {
        if (isListening) {
            stopListening()
        }
        super.onDestroy()
    }

    private companion object {
        const val AUDIO_PERMISSION_REQUEST_CODE = 101
        const val NO_SIGNAL_THRESHOLD = 5
        const val NOTE_HIGHLIGHT_DURATION_MS = 500L
    }
}
