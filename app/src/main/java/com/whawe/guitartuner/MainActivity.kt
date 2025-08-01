package com.whawe.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var noteTextView: TextView
    private lateinit var frequencyTextView: TextView
    private lateinit var accuracyTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView
    
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var recordingThread: Thread? = null
    
    private val TAG = "GuitarTuner"
    
    // Guitar string frequencies (E2, A2, D3, G3, B3, E4)
    private val guitarNotes = mapOf(
        "E2" to 82.41f,
        "A2" to 110.00f,
        "D3" to 146.83f,
        "G3" to 196.00f,
        "B3" to 246.94f,
        "E4" to 329.63f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupButtonListeners()
        requestAudioPermission()
    }
    
    private fun initializeViews() {
        noteTextView = findViewById(R.id.noteTextView)
        frequencyTextView = findViewById(R.id.frequencyTextView)
        accuracyTextView = findViewById(R.id.accuracyTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
    }
    
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun startTuning() {
        if (isListening) return
        
        try {
            // Check if we're on emulator (no real microphone)
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Emulator detected - use demo mode
                startDemoMode()
                return
            }
            
            audioRecord?.startRecording()
            
            recordingThread = Thread {
                val buffer = ShortArray(bufferSize)
                
                while (isListening) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // Simple pitch detection using zero-crossing rate
                        val frequency = detectPitch(buffer, sampleRate)
                        if (frequency > 0) {
                            val note = freqToNote(frequency)
                            val accuracy = calculateAccuracy(frequency, note)
                            
                            runOnUiThread {
                                updateUI(frequency, note, accuracy)
                            }
                        }
                    }
                }
            }
            recordingThread?.start()
            
            isListening = true
            updateButtonStates()
            statusTextView.text = "Listening for guitar notes..."
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting tuning", e)
            // Fall back to demo mode if audio fails
            startDemoMode()
        }
    }
    
    private fun startDemoMode() {
        isListening = true
        updateButtonStates()
        statusTextView.text = "Demo mode - simulating guitar notes..."
        
        recordingThread = Thread {
            val demoFrequencies = listOf(82.41f, 110.00f, 146.83f, 196.00f, 246.94f, 329.63f)
            var index = 0
            
            while (isListening) {
                val frequency = demoFrequencies[index % demoFrequencies.size]
                val note = freqToNote(frequency)
                val accuracy = calculateAccuracy(frequency, note)
                
                runOnUiThread {
                    updateUI(frequency, note, accuracy)
                }
                
                Thread.sleep(2000) // Change note every 2 seconds
                index++
            }
        }
        recordingThread?.start()
    }
    
    private fun detectPitch(buffer: ShortArray, sampleRate: Int): Float {
        // Improved pitch detection using autocorrelation
        val bufferSize = buffer.size
        if (bufferSize < 1024) return 0f
        
        // Calculate RMS to detect if there's enough signal
        var rms = 0.0
        for (i in buffer.indices) {
            rms += buffer[i] * buffer[i]
        }
        rms = Math.sqrt(rms / bufferSize)
        
        // Only process if signal is strong enough
        if (rms < 1000) return 0f
        
        // Use autocorrelation for better pitch detection
        val correlation = FloatArray(bufferSize / 2)
        var maxCorrelation = 0f
        var maxIndex = 0
        
        for (lag in 1 until bufferSize / 2) {
            var sum = 0f
            for (i in 0 until bufferSize - lag) {
                sum += buffer[i] * buffer[i + lag]
            }
            correlation[lag] = sum
            
            if (sum > maxCorrelation) {
                maxCorrelation = sum
                maxIndex = lag
            }
        }
        
        if (maxIndex > 0) {
            val frequency = sampleRate.toFloat() / maxIndex
            // Filter for guitar string frequencies (E2 to E4)
            return if (frequency in 80.0..400.0) frequency else 0f
        }
        
        return 0f
    }
    
    private fun stopTuning() {
        try {
            isListening = false
            recordingThread?.interrupt()
            recordingThread = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            updateButtonStates()
            
            noteTextView.text = "Guitar Tuner"
            frequencyTextView.text = "0.0 Hz"
            accuracyTextView.text = ""
            statusTextView.text = "Tuning stopped"
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tuning", e)
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
    
    private fun updateUI(frequency: Float, note: String, accuracy: Float) {
        noteTextView.text = note
        frequencyTextView.text = "%.1f Hz".format(frequency)
        
        when {
            accuracy < 0.02f -> {
                accuracyTextView.text = "✓ Perfect!"
                accuracyTextView.setTextColor(0xFF00FF00.toInt()) // Green
                noteTextView.setTextColor(0xFF00FF00.toInt()) // Green note when perfect
            }
            accuracy < 0.05f -> {
                accuracyTextView.text = "✓ Very Good"
                accuracyTextView.setTextColor(0xFF00FF00.toInt()) // Green
                noteTextView.setTextColor(0xFFFFFFFF.toInt()) // White
            }
            accuracy < 0.1f -> {
                accuracyTextView.text = "✓ Good"
                accuracyTextView.setTextColor(0xFFFFFF00.toInt()) // Yellow
                noteTextView.setTextColor(0xFFFFFFFF.toInt()) // White
            }
            accuracy < 0.2f -> {
                accuracyTextView.text = "→ Adjust"
                accuracyTextView.setTextColor(0xFFFFA500.toInt()) // Orange
                noteTextView.setTextColor(0xFFFFA500.toInt()) // Orange note
            }
            else -> {
                accuracyTextView.text = "→ Way off"
                accuracyTextView.setTextColor(0xFFFF0000.toInt()) // Red
                noteTextView.setTextColor(0xFFFF0000.toInt()) // Red note
            }
        }
        
        // Add cents deviation for more precise feedback
        val targetFreq = guitarNotes.values.find { abs(frequency - it) < 50 } ?: frequency
        val cents = 1200 * ln(frequency / targetFreq) / ln(2.0)
        if (abs(cents) < 50) {
            frequencyTextView.text = "%.1f Hz (%.0f cents)".format(frequency, cents)
        }
    }
    
    private fun freqToNote(freq: Float): String {
        val A4 = 440.0
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val noteNumber = (12 * (ln(freq / A4) / ln(2.0))).roundToInt() + 69
        val noteName = noteNames[noteNumber % 12]
        val octave = noteNumber / 12 - 1
        return "$noteName$octave"
    }
    
    private fun calculateAccuracy(frequency: Float, detectedNote: String): Float {
        // Find the closest guitar note
        var closestNote = ""
        var minDifference = Float.MAX_VALUE
        
        for ((note, noteFreq) in guitarNotes) {
            val difference = abs(frequency - noteFreq)
            if (difference < minDifference) {
                minDifference = difference
                closestNote = note
            }
        }
        
        // Calculate accuracy as percentage difference
        val targetFreq = guitarNotes[closestNote] ?: frequency
        val accuracy = abs(frequency - targetFreq) / targetFreq
        
        // Log for debugging
        Log.d(TAG, "Frequency: $frequency Hz, Detected: $detectedNote, Target: $closestNote, Accuracy: $accuracy")
        
        return accuracy
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission is required for tuning", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTuning()
    }
}

