package com.whawe.guitartuner.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlin.math.max

data class AudioConfig(
    val sampleRate: Int,
    val bufferSizeInBytes: Int,
    val analysisSize: Int,
    val audioSource: Int
)

object AudioRecordFactory {
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun create(): Pair<AudioRecord, AudioConfig>? {
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

    fun release(record: AudioRecord) {
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        } catch (_: IllegalStateException) {
            // Ignore records that are already stopping or not started.
        } finally {
            record.release()
        }
    }
}
