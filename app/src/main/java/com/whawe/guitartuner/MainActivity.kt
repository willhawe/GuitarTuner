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
    
    // Tuner dial indicators
    private lateinit var leftIndicator: View
    private lateinit var rightIndicator: View
    private lateinit var centerIndicator: View
    
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var recordingThread: Thread? = null
    
    // Frequency smoothing
    private var lastFrequency = 0f
    private var frequencyCount = 0
    private val frequencyHistory = mutableListOf<Float>()
    
    private val TAG = "GuitarTuner"
    
    // Comprehensive musical note frequencies (C0 to C8)
    private val allNotes = mapOf(
        // C octaves
        "C0" to 16.35f, "C1" to 32.70f, "C2" to 65.41f, "C3" to 130.81f, "C4" to 261.63f, "C5" to 523.25f, "C6" to 1046.50f, "C7" to 2093.00f, "C8" to 4186.01f,
        // C#/Db octaves
        "C#0" to 17.32f, "C#1" to 34.65f, "C#2" to 69.30f, "C#3" to 138.59f, "C#4" to 277.18f, "C#5" to 554.37f, "C#6" to 1108.73f, "C#7" to 2217.46f,
        "Db0" to 17.32f, "Db1" to 34.65f, "Db2" to 69.30f, "Db3" to 138.59f, "Db4" to 277.18f, "Db5" to 554.37f, "Db6" to 1108.73f, "Db7" to 2217.46f,
        // D octaves
        "D0" to 18.35f, "D1" to 36.71f, "D2" to 73.42f, "D3" to 146.83f, "D4" to 293.66f, "D5" to 587.33f, "D6" to 1174.66f, "D7" to 2349.32f,
        // D#/Eb octaves
        "D#0" to 19.45f, "D#1" to 38.89f, "D#2" to 77.78f, "D#3" to 155.56f, "D#4" to 311.13f, "D#5" to 622.25f, "D#6" to 1244.51f, "D#7" to 2489.02f,
        "Eb0" to 19.45f, "Eb1" to 38.89f, "Eb2" to 77.78f, "Eb3" to 155.56f, "Eb4" to 311.13f, "Eb5" to 622.25f, "Eb6" to 1244.51f, "Eb7" to 2489.02f,
        // E octaves
        "E0" to 20.60f, "E1" to 41.20f, "E2" to 82.41f, "E3" to 164.81f, "E4" to 329.63f, "E5" to 659.25f, "E6" to 1318.51f, "E7" to 2637.02f,
        // F octaves
        "F0" to 21.83f, "F1" to 43.65f, "F2" to 87.31f, "F3" to 174.61f, "F4" to 349.23f, "F5" to 698.46f, "F6" to 1396.91f, "F7" to 2793.83f,
        // F#/Gb octaves
        "F#0" to 23.12f, "F#1" to 46.25f, "F#2" to 92.50f, "F#3" to 185.00f, "F#4" to 369.99f, "F#5" to 739.99f, "F#6" to 1479.98f, "F#7" to 2959.96f,
        "Gb0" to 23.12f, "Gb1" to 46.25f, "Gb2" to 92.50f, "Gb3" to 185.00f, "Gb4" to 369.99f, "Gb5" to 739.99f, "Gb6" to 1479.98f, "Gb7" to 2959.96f,
        // G octaves
        "G0" to 24.50f, "G1" to 49.00f, "G2" to 98.00f, "G3" to 196.00f, "G4" to 392.00f, "G5" to 783.99f, "G6" to 1567.98f, "G7" to 3135.96f,
        // G#/Ab octaves
        "G#0" to 25.96f, "G#1" to 51.91f, "G#2" to 103.83f, "G#3" to 207.65f, "G#4" to 415.30f, "G#5" to 830.61f, "G#6" to 1661.22f, "G#7" to 3322.44f,
        "Ab0" to 25.96f, "Ab1" to 51.91f, "Ab2" to 103.83f, "Ab3" to 207.65f, "Ab4" to 415.30f, "Ab5" to 830.61f, "Ab6" to 1661.22f, "Ab7" to 3322.44f,
        // A octaves
        "A0" to 27.50f, "A1" to 55.00f, "A2" to 110.00f, "A3" to 220.00f, "A4" to 440.00f, "A5" to 880.00f, "A6" to 1760.00f, "A7" to 3520.00f,
        // A#/Bb octaves
        "A#0" to 29.14f, "A#1" to 58.27f, "A#2" to 116.54f, "A#3" to 233.08f, "A#4" to 466.16f, "A#5" to 932.33f, "A#6" to 1864.66f, "A#7" to 3729.31f,
        "Bb0" to 29.14f, "Bb1" to 58.27f, "Bb2" to 116.54f, "Bb3" to 233.08f, "Bb4" to 466.16f, "Bb5" to 932.33f, "Bb6" to 1864.66f, "Bb7" to 3729.31f,
        // B octaves
        "B0" to 30.87f, "B1" to 61.74f, "B2" to 123.47f, "B3" to 246.94f, "B4" to 493.88f, "B5" to 987.77f, "B6" to 1975.53f, "B7" to 3951.07f
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
        
        // Initialize tuner dial indicators
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
        
        // Add test button for debugging
        findViewById<Button>(R.id.testButton)?.setOnClickListener {
            testAudioInput()
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
            // Use higher sample rate for better accuracy, especially for lower frequencies
            val sampleRate = 44100 // Increased from 22050 for better low frequency detection
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            
            Log.d(TAG, "Buffer size: $bufferSize, Sample rate: $sampleRate")
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 4 // Much larger buffer for better low frequency analysis
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.d(TAG, "AudioRecord not initialized, using demo mode")
                startDemoMode()
                return
            }
            
            Log.d(TAG, "AudioRecord initialized successfully")
            audioRecord?.startRecording()
            
            recordingThread = Thread {
                val buffer = ShortArray(bufferSize * 2) // Larger buffer for analysis
                var consecutiveNoSignal = 0
                
                while (isListening) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // Detect pitch and update UI
                        val frequency = detectPitch(buffer, sampleRate)
                        if (frequency > 0) {
                            consecutiveNoSignal = 0
                            // Smooth frequency detection
                            val smoothedFrequency = smoothFrequency(frequency)
                            val note = freqToNote(smoothedFrequency)
                            val accuracy = calculateAccuracy(smoothedFrequency, note)
                            
                            runOnUiThread {
                                updateUI(smoothedFrequency, note, accuracy)
                                statusTextView.text = "Detecting: ${smoothedFrequency.roundToInt()} Hz"
                            }
                        } else {
                            consecutiveNoSignal++
                            // Show "No signal" when no frequency detected
                            runOnUiThread {
                                if (consecutiveNoSignal > 5) {
                                    noteTextView.text = "Universal Tuner"
                                    frequencyTextView.text = "No signal"
                                    accuracyTextView.text = ""
                                    statusTextView.text = "Listening for any musical note..."
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No audio data read")
                    }
                }
            }
            recordingThread?.start()
            
            isListening = true
            updateButtonStates()
            statusTextView.text = "Listening for any musical note..."
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting tuning", e)
            // Fall back to demo mode if audio fails
            startDemoMode()
        }
    }
    
    private fun startDemoMode() {
        isListening = true
        updateButtonStates()
        statusTextView.text = "Demo mode - simulating musical notes..."
        
        recordingThread = Thread {
            val demoFrequencies = listOf(261.63f, 329.63f, 440.00f, 523.25f, 659.25f, 880.00f) // C4, E4, A4, C5, E5, A5
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
        val bufferSize = buffer.size
        if (bufferSize < 1024) return 0f
        
        // Calculate RMS (Root Mean Square) for signal strength
        var rms = 0.0
        for (i in buffer.indices) {
            rms += buffer[i] * buffer[i]
        }
        rms = Math.sqrt(rms / bufferSize)
        
        // Lower threshold for better sensitivity
        if (rms < 200) { // Even lower threshold
            return 0f
        }
        
        // Try simple peak detection first
        val simpleFreq = detectSimpleFrequency(buffer, sampleRate)
        if (simpleFreq > 0) {
            Log.d(TAG, "Simple detection: $simpleFreq Hz, RMS: $rms")
            return simpleFreq
        }
        
        // Use zero-crossing rate for initial frequency estimation
        var zeroCrossings = 0
        for (i in 1 until bufferSize) {
            if ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0)) {
                zeroCrossings++
            }
        }
        
        // Calculate frequency from zero-crossing rate
        val zeroCrossingFreq = (zeroCrossings * sampleRate) / (2.0 * bufferSize)
        
        // Use autocorrelation for refinement
        val correlation = FloatArray(bufferSize / 2)
        var maxCorrelation = 0f
        var maxIndex = 0
        
        // Calculate autocorrelation
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
        
        var frequency = 0f
        
        if (maxIndex > 0) {
            frequency = sampleRate.toFloat() / maxIndex
        }
        
        // Combine zero-crossing and autocorrelation results
        if (frequency > 0 && zeroCrossingFreq > 0) {
            // Use the more reliable result
            val freqDiff = abs(frequency - zeroCrossingFreq)
            if (freqDiff < 10) { // If they're close, use autocorrelation
                frequency = frequency
            } else {
                // Use zero-crossing for lower frequencies, autocorrelation for higher
                if (frequency < 150) {
                    frequency = zeroCrossingFreq.toFloat()
                }
            }
        } else if (frequency > 0) {
            frequency = frequency
        } else if (zeroCrossingFreq > 0) {
            frequency = zeroCrossingFreq.toFloat()
        }
        
        // Filter for musical frequencies - expanded range for all instruments
        if (frequency in 15.0..4000.0) { // Covers C0 (16.35 Hz) to C8 (4186 Hz) and beyond
            Log.d(TAG, "Detected frequency: $frequency Hz, RMS: $rms, Zero-crossing: $zeroCrossingFreq")
            return frequency
        }
        
        return 0f
    }
    
    private fun detectSimpleFrequency(buffer: ShortArray, sampleRate: Int): Float {
        // Simple peak detection method
        var peakCount = 0
        var lastPeak = 0
        val peaks = mutableListOf<Int>()
        
        // Find peaks in the signal
        for (i in 1 until buffer.size - 1) {
            if (buffer[i] > buffer[i - 1] && buffer[i] > buffer[i + 1] && buffer[i] > 1000) {
                peaks.add(i)
                if (peaks.size > 1) {
                    val period = i - lastPeak
                    if (period > 10 && period < 1000) { // Reasonable period range
                        peakCount++
                    }
                }
                lastPeak = i
            }
        }
        
        if (peakCount > 2 && peaks.size > 3) {
            // Calculate average period
            var totalPeriod = 0
            for (i in 1 until peaks.size) {
                totalPeriod += peaks[i] - peaks[i - 1]
            }
            val avgPeriod = totalPeriod.toFloat() / (peaks.size - 1)
            
            if (avgPeriod > 0) {
                val frequency = sampleRate / avgPeriod
                if (frequency in 15.0..4000.0) { // Expanded range for all musical notes
                    return frequency
                }
            }
        }
        
        return 0f
    }
    
    private fun smoothFrequency(frequency: Float): Float {
        // Add to history
        frequencyHistory.add(frequency)
        
        // Keep only last 5 readings
        if (frequencyHistory.size > 5) {
            frequencyHistory.removeAt(0)
        }
        
        // Calculate median frequency (more stable than average)
        val sortedFrequencies = frequencyHistory.sorted()
        val medianIndex = sortedFrequencies.size / 2
        
        return if (sortedFrequencies.size % 2 == 0) {
            (sortedFrequencies[medianIndex - 1] + sortedFrequencies[medianIndex]) / 2
        } else {
            sortedFrequencies[medianIndex]
        }
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
        
        // Calculate cents deviation for tuner dial
        val targetFreq = allNotes.values.find { abs(frequency - it) < 50 } ?: frequency
        val cents = 1200 * ln(frequency / targetFreq) / ln(2.0)
        
        // Update tuner dial
        updateTunerDial(cents)
        
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
        if (abs(cents) < 50) {
            frequencyTextView.text = "%.1f Hz (%.0f cents)".format(frequency, cents)
        }
    }
    
    private fun updateTunerDial(cents: Double) {
        // Hide all indicators first
        leftIndicator.visibility = View.GONE
        rightIndicator.visibility = View.GONE
        centerIndicator.visibility = View.GONE
        
        when {
            cents < -20 -> {
                // Flat - show left indicator
                leftIndicator.visibility = View.VISIBLE
                leftIndicator.setBackgroundColor(0xFFFF0000.toInt()) // Red
            }
            cents > 20 -> {
                // Sharp - show right indicator
                rightIndicator.visibility = View.VISIBLE
                rightIndicator.setBackgroundColor(0xFFFF0000.toInt()) // Red
            }
            abs(cents) <= 5 -> {
                // Perfect - show center indicator
                centerIndicator.visibility = View.VISIBLE
                centerIndicator.setBackgroundColor(0xFF00FF00.toInt()) // Green
            }
            cents < 0 -> {
                // Slightly flat - show left indicator
                leftIndicator.visibility = View.VISIBLE
                leftIndicator.setBackgroundColor(0xFFFFFF00.toInt()) // Yellow
            }
            else -> {
                // Slightly sharp - show right indicator
                rightIndicator.visibility = View.VISIBLE
                rightIndicator.setBackgroundColor(0xFFFFFF00.toInt()) // Yellow
            }
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
        
        for ((note, noteFreq) in allNotes) {
            val difference = abs(frequency - noteFreq)
            if (difference < minDifference) {
                minDifference = difference
                closestNote = note
            }
        }
        
        // Calculate accuracy as percentage difference
        val targetFreq = allNotes[closestNote] ?: frequency
        val accuracy = abs(frequency - targetFreq) / targetFreq
        
        // Log for debugging
        Log.d(TAG, "Frequency: $frequency Hz, Detected: $detectedNote, Target: $closestNote, Accuracy: $accuracy")
        
        return accuracy
    }
    
    private fun testAudioInput() {
        statusTextView.text = "Testing audio input..."
        
        Thread {
            try {
                val sampleRate = 44100 // Match the main tuning sample rate
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                
                val testAudioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                
                if (testAudioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    testAudioRecord.startRecording()
                    val buffer = ShortArray(bufferSize)
                    
                    for (i in 1..10) {
                        val readSize = testAudioRecord.read(buffer, 0, buffer.size)
                        if (readSize > 0) {
                            var rms = 0.0
                            for (j in buffer.indices) {
                                rms += buffer[j] * buffer[j]
                            }
                            rms = Math.sqrt(rms / bufferSize)
                            
                            // Try to detect frequency
                            val frequency = detectPitch(buffer, sampleRate)
                            
                            runOnUiThread {
                                if (frequency > 0) {
                                    statusTextView.text = "Test $i: RMS = ${rms.roundToInt()}, Freq = ${frequency.roundToInt()} Hz"
                                } else {
                                    statusTextView.text = "Test $i: RMS = ${rms.roundToInt()}, No freq detected"
                                }
                            }
                        }
                        Thread.sleep(500)
                    }
                    
                    testAudioRecord.stop()
                    testAudioRecord.release()
                    
                    runOnUiThread {
                        statusTextView.text = "Audio test complete"
                    }
                } else {
                    runOnUiThread {
                        statusTextView.text = "AudioRecord failed to initialize"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusTextView.text = "Audio test error: ${e.message}"
                }
            }
        }.start()
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

