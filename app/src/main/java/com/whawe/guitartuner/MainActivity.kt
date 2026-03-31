package com.whawe.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.whawe.guitartuner.tuning.AccuracyBand
import com.whawe.guitartuner.tuning.DialIndicator
import com.whawe.guitartuner.tuning.FrequencySmoother
import com.whawe.guitartuner.tuning.NoteMapper
import com.whawe.guitartuner.tuning.NoteMatch
import com.whawe.guitartuner.tuning.PitchDetector
import com.whawe.guitartuner.tuning.TuningFeedback
import com.whawe.guitartuner.tuning.TuningFeedbackEvaluator
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var noteTextView: TextView
    private lateinit var frequencyTextView: TextView
    private lateinit var accuracyTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView

    private lateinit var leftIndicator: View
    private lateinit var rightIndicator: View
    private lateinit var centerIndicator: View

    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isListening = false

    private var recordingThread: Thread? = null
    private val frequencySmoother = FrequencySmoother()

    private data class AudioConfig(
        val sampleRate: Int,
        val bufferSizeInBytes: Int,
        val analysisSize: Int,
        val audioSource: Int
    )

    private data class FeedbackStyle(
        val labelRes: Int,
        @ColorInt val accuracyColor: Int,
        @ColorInt val noteColor: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupButtonListeners()
        renderIdleState()
    }

    private fun initializeViews() {
        noteTextView = findViewById(R.id.noteTextView)
        frequencyTextView = findViewById(R.id.frequencyTextView)
        accuracyTextView = findViewById(R.id.accuracyTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)
        leftIndicator = findViewById(R.id.leftIndicator)
        rightIndicator = findViewById(R.id.rightIndicator)
        centerIndicator = findViewById(R.id.centerIndicator)
    }

    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            if (checkAudioPermission()) {
                startTuning()
            } else {
                requestAudioPermission()
            }
        }

        stopButton.setOnClickListener {
            stopTuning()
        }
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

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startTuning() {
        if (isListening) {
            return
        }

        if (!checkAudioPermission()) {
            requestAudioPermission()
            return
        }

        frequencySmoother.clear()
        isListening = true
        updateButtonStates()
        statusTextView.text = getString(R.string.status_listening)

        try {
            val recordResult = createAudioRecord()
            if (recordResult == null) {
                Log.d(TAG, "AudioRecord not initialized, using demo mode")
                startDemoMode()
                return
            }

            val (record, config) = recordResult
            audioRecord = record
            Log.d(
                TAG,
                "AudioRecord initialized: rate=${config.sampleRate}, " +
                    "buffer=${config.bufferSizeInBytes} bytes, " +
                    "analysis=${config.analysisSize} samples, " +
                    "source=${config.audioSource}"
            )

            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.d(TAG, "AudioRecord failed to start recording, using demo mode")
                record.release()
                audioRecord = null
                startDemoMode()
                return
            }

            recordingThread = Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val buffer = ShortArray(config.analysisSize)
                var consecutiveNoSignal = 0
                var consecutiveReadErrors = 0

                while (isListening) {
                    val readSize = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (readSize > 0) {
                        consecutiveReadErrors = 0
                        val detectedFrequency = PitchDetector.detectPitch(buffer, readSize, config.sampleRate)

                        if (detectedFrequency > 0f) {
                            consecutiveNoSignal = 0
                            val smoothedFrequency = frequencySmoother.add(detectedFrequency)
                            val noteMatch = NoteMapper.findClosestNote(smoothedFrequency)
                            val centsOff = NoteMapper.calculateCentsOff(
                                smoothedFrequency,
                                noteMatch.targetFrequency
                            )

                            runOnUiThread {
                                renderTuning(smoothedFrequency, noteMatch, centsOff)
                                statusTextView.text = getString(
                                    R.string.status_detecting,
                                    smoothedFrequency.roundToInt()
                                )
                            }
                        } else {
                            consecutiveNoSignal++
                            if (consecutiveNoSignal > 5) {
                                runOnUiThread {
                                    renderNoSignalState()
                                }
                            }
                        }
                    } else {
                        consecutiveReadErrors++
                        Log.d(TAG, "No audio data read: $readSize")
                        if (consecutiveReadErrors >= 3) {
                            runOnUiThread {
                                statusTextView.text = getString(R.string.status_audio_error)
                            }
                        }
                    }
                }
            }
            recordingThread?.start()
        } catch (error: Exception) {
            Log.e(TAG, "Error starting tuning", error)
            if (isListening) {
                startDemoMode()
            }
        }
    }

    private fun startDemoMode() {
        frequencySmoother.clear()
        isListening = true
        updateButtonStates()
        statusTextView.text = getString(R.string.status_demo)

        recordingThread = Thread {
            val demoFrequencies = listOf(82.41f, 110.00f, 146.83f, 196.00f, 246.94f, 329.63f)
            var index = 0

            while (isListening) {
                val frequency = demoFrequencies[index % demoFrequencies.size]
                val noteMatch = NoteMapper.findClosestNote(frequency)
                val centsOff = NoteMapper.calculateCentsOff(frequency, noteMatch.targetFrequency)

                runOnUiThread {
                    renderTuning(frequency, noteMatch, centsOff)
                    statusTextView.text = getString(R.string.status_demo)
                }

                try {
                    Thread.sleep(DEMO_MODE_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }

                index++
            }
        }
        recordingThread?.start()
    }

    private fun stopTuning() {
        isListening = false
        recordingThread?.interrupt()
        recordingThread = null

        audioRecord?.let { releaseAudioRecord(it) }
        audioRecord = null

        frequencySmoother.clear()
        updateButtonStates()
        renderStoppedState()
    }

    private fun releaseAudioRecord(record: AudioRecord) {
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        } catch (error: IllegalStateException) {
            Log.d(TAG, "AudioRecord was not in a stoppable state", error)
        } finally {
            record.release()
        }
    }

    private fun updateButtonStates() {
        if (isListening) {
            startButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
        } else {
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
        }
    }

    private fun renderIdleState() {
        noteTextView.text = getString(R.string.app_name)
        noteTextView.setTextColor(getColorCompat(R.color.text_primary))
        frequencyTextView.text = getString(R.string.frequency_placeholder)
        accuracyTextView.text = ""
        statusTextView.text = getString(R.string.status_idle)
        hideDialIndicators()
    }

    private fun renderStoppedState() {
        renderIdleState()
        statusTextView.text = getString(R.string.status_stopped)
    }

    private fun renderNoSignalState() {
        frequencySmoother.clear()
        noteTextView.text = getString(R.string.app_name)
        noteTextView.setTextColor(getColorCompat(R.color.text_primary))
        frequencyTextView.text = getString(R.string.no_signal)
        accuracyTextView.text = ""
        statusTextView.text = getString(R.string.status_listening)
        hideDialIndicators()
    }

    private fun renderTuning(frequency: Float, noteMatch: NoteMatch, centsOff: Double) {
        val feedback = TuningFeedbackEvaluator.fromCents(centsOff)
        val style = feedbackStyle(feedback.accuracyBand)

        noteTextView.text = noteMatch.name
        noteTextView.setTextColor(style.noteColor)
        accuracyTextView.text = getString(style.labelRes)
        accuracyTextView.setTextColor(style.accuracyColor)
        frequencyTextView.text = getString(
            R.string.frequency_readout,
            frequency,
            centsOff,
            noteMatch.targetFrequency
        )

        updateTunerDial(feedback)
    }

    private fun feedbackStyle(accuracyBand: AccuracyBand): FeedbackStyle {
        return when (accuracyBand) {
            AccuracyBand.PERFECT -> FeedbackStyle(
                labelRes = R.string.accuracy_perfect,
                accuracyColor = getColorCompat(R.color.tuner_green),
                noteColor = getColorCompat(R.color.tuner_green)
            )
            AccuracyBand.VERY_GOOD -> FeedbackStyle(
                labelRes = R.string.accuracy_very_good,
                accuracyColor = getColorCompat(R.color.tuner_green),
                noteColor = getColorCompat(R.color.text_primary)
            )
            AccuracyBand.GOOD -> FeedbackStyle(
                labelRes = R.string.accuracy_good,
                accuracyColor = getColorCompat(R.color.tuner_yellow),
                noteColor = getColorCompat(R.color.text_primary)
            )
            AccuracyBand.ADJUST -> FeedbackStyle(
                labelRes = R.string.accuracy_adjust,
                accuracyColor = getColorCompat(R.color.tuner_orange),
                noteColor = getColorCompat(R.color.tuner_orange)
            )
            AccuracyBand.WAY_OFF -> FeedbackStyle(
                labelRes = R.string.accuracy_way_off,
                accuracyColor = getColorCompat(R.color.tuner_red),
                noteColor = getColorCompat(R.color.tuner_red)
            )
        }
    }

    private fun updateTunerDial(feedback: TuningFeedback) {
        hideDialIndicators()

        when (feedback.dialIndicator) {
            DialIndicator.LEFT_WARNING -> {
                leftIndicator.visibility = View.VISIBLE
                leftIndicator.setBackgroundColor(getColorCompat(R.color.tuner_red))
            }
            DialIndicator.LEFT_CAUTION -> {
                leftIndicator.visibility = View.VISIBLE
                leftIndicator.setBackgroundColor(getColorCompat(R.color.tuner_yellow))
            }
            DialIndicator.CENTER_IN_TUNE -> {
                centerIndicator.visibility = View.VISIBLE
                centerIndicator.setBackgroundColor(getColorCompat(R.color.tuner_green))
            }
            DialIndicator.RIGHT_CAUTION -> {
                rightIndicator.visibility = View.VISIBLE
                rightIndicator.setBackgroundColor(getColorCompat(R.color.tuner_yellow))
            }
            DialIndicator.RIGHT_WARNING -> {
                rightIndicator.visibility = View.VISIBLE
                rightIndicator.setBackgroundColor(getColorCompat(R.color.tuner_red))
            }
        }
    }

    private fun hideDialIndicators() {
        leftIndicator.visibility = View.GONE
        rightIndicator.visibility = View.GONE
        centerIndicator.visibility = View.GONE
    }

    @ColorInt
    private fun getColorCompat(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(this, colorRes)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(): Pair<AudioRecord, AudioConfig>? {
        val sampleRates = intArrayOf(48000, 44100, 32000, 22050, 16000)
        val sources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intArrayOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)
        } else {
            intArrayOf(MediaRecorder.AudioSource.MIC)
        }

        for (source in sources) {
            for (rate in sampleRates) {
                val minBuffer = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuffer <= 0) {
                    continue
                }

                val analysisSize = if (rate >= 44100) 4096 else 2048
                val bufferSizeInBytes = max(minBuffer, analysisSize * 2)

                val record = AudioRecord(
                    source,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    return record to AudioConfig(
                        sampleRate = rate,
                        bufferSizeInBytes = bufferSizeInBytes,
                        analysisSize = analysisSize,
                        audioSource = source
                    )
                }

                record.release()
            }
        }

        return null
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
            startTuning()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        if (isListening) {
            stopTuning()
        }
        super.onDestroy()
    }

    private companion object {
        const val AUDIO_PERMISSION_REQUEST_CODE = 100
        const val DEMO_MODE_INTERVAL_MS = 1500L
        const val TAG = "GuitarTuner"
    }
}
