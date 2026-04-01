package com.whawe.guitartuner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.whawe.guitartuner.audio.AudioRecordFactory
import com.whawe.guitartuner.tuning.AccuracyBand
import com.whawe.guitartuner.tuning.FrequencySmoother
import com.whawe.guitartuner.tuning.NoteMapper
import com.whawe.guitartuner.tuning.NoteMatch
import com.whawe.guitartuner.tuning.PitchDetector
import com.whawe.guitartuner.tuning.TuningFeedback
import com.whawe.guitartuner.tuning.TuningFeedbackEvaluator
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var noteTextView: TextView
    private lateinit var frequencyTextView: TextView
    private lateinit var accuracyTextView: TextView
    private lateinit var scalePracticeButton: Button
    private lateinit var statusTextView: TextView

    private lateinit var tunerDial: View
    private lateinit var needleIndicator: View

    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isListening = false

    private var recordingThread: Thread? = null
    private val frequencySmoother = FrequencySmoother()

    private data class FeedbackStyle(
        val labelRes: Int,
        @ColorInt val accuracyColor: Int,
        @ColorInt val noteColor: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupNavigation()
        renderIdleState()
    }

    override fun onStart() {
        super.onStart()
        beginTuningIfPossible()
    }

    override fun onStop() {
        if (isListening) {
            stopTuning()
        }
        super.onStop()
    }

    private fun initializeViews() {
        noteTextView = findViewById(R.id.noteTextView)
        frequencyTextView = findViewById(R.id.frequencyTextView)
        accuracyTextView = findViewById(R.id.accuracyTextView)
        scalePracticeButton = findViewById(R.id.scalePracticeButton)
        statusTextView = findViewById(R.id.statusTextView)
        tunerDial = findViewById(R.id.tunerDial)
        needleIndicator = findViewById(R.id.needleIndicator)
    }

    private fun setupNavigation() {
        scalePracticeButton.setOnClickListener {
            startActivity(Intent(this, ScalePracticeActivity::class.java))
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

    private fun beginTuningIfPossible() {
        if (checkAudioPermission()) {
            startTuning()
        } else {
            requestAudioPermission()
        }
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
        statusTextView.text = getString(R.string.status_listening)

        try {
            val recordResult = AudioRecordFactory.create()
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
                AudioRecordFactory.release(record)
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
                            if (consecutiveNoSignal == 6) {
                                // Fire exactly once per silence event rather than every
                                // frame, avoiding redundant smoother clears and UI posts.
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

        audioRecord?.let { AudioRecordFactory.release(it) }
        audioRecord = null

        frequencySmoother.clear()
        renderIdleState()
    }

    private fun renderIdleState() {
        noteTextView.text = getString(R.string.app_name)
        noteTextView.setTextColor(getColorCompat(R.color.text_primary))
        frequencyTextView.text = getString(R.string.frequency_placeholder)
        accuracyTextView.text = ""
        statusTextView.text = getString(R.string.status_idle)
        resetTunerDial()
    }

    private fun renderNoSignalState() {
        frequencySmoother.clear()
        noteTextView.text = getString(R.string.app_name)
        noteTextView.setTextColor(getColorCompat(R.color.text_primary))
        frequencyTextView.text = getString(R.string.no_signal)
        accuracyTextView.text = ""
        statusTextView.text = getString(R.string.status_listening)
        resetTunerDial()
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

        updateTunerDial(feedback, style.accuracyColor)
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

    private fun updateTunerDial(feedback: TuningFeedback, @ColorInt needleColor: Int) {
        val applyNeedlePosition = {
            val travelRange = (((tunerDial.width - needleIndicator.width) / 2f) -
                dpToPx(DIAL_EDGE_PADDING_DP)).coerceAtLeast(0f)
            val targetTranslation = travelRange * feedback.needleOffset

            needleIndicator.visibility = View.VISIBLE
            needleIndicator.setBackgroundColor(needleColor)
            needleIndicator.animate()
                .translationX(targetTranslation)
                .setDuration(NEEDLE_ANIMATION_DURATION_MS)
                .start()
        }

        if (tunerDial.width == 0) {
            tunerDial.post { applyNeedlePosition() }
        } else {
            applyNeedlePosition()
        }
    }

    private fun resetTunerDial() {
        needleIndicator.animate().cancel()
        needleIndicator.translationX = 0f
        needleIndicator.visibility = View.GONE
    }

    @ColorInt
    private fun getColorCompat(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(this, colorRes)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
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
            statusTextView.text = getString(R.string.permission_required)
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
        const val DIAL_EDGE_PADDING_DP = 42f
        // Longer than one audio frame (~93 ms at 44100 Hz / 4096 samples) so each new
        // update arrives while an animation is still in progress. ViewPropertyAnimator
        // starts from the current position on each call, producing continuous smooth
        // motion instead of the snap-then-hold pattern caused by a 60 ms duration.
        const val NEEDLE_ANIMATION_DURATION_MS = 150L
        const val TAG = "GuitarTuner"
    }
}
