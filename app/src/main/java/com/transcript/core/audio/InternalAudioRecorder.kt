package com.transcript.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-efficiency internal device audio capture targeting 16kHz mono 16-bit PCM.
 * Uses Android 10+ AudioPlaybackCaptureConfiguration (ADR-0001).
 */
class InternalAudioRecorder(
    private val mediaProjection: MediaProjection? = null
) : IAudioStreamProvider {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    @Volatile
    private var _isCapturing = false
    override val isCapturing: Boolean
        get() = _isCapturing

    @SuppressLint("MissingPermission")
    override fun startCapture(): Flow<ByteArray> = flow {
        if (_isCapturing) return@flow

        val sampleRate = SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        // Frame size: 30ms chunk at 16kHz = 480 samples = 960 bytes
        val frameBytes = (sampleRate * FRAME_DURATION_MS / 1000) * BYTES_PER_SAMPLE
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, frameBytes * 4)

        val record: AudioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setAudioPlaybackCaptureConfig(captureConfig)
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        }

        audioRecord = record

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return@flow
        }

        record.startRecording()
        _isCapturing = true

        val buffer = ByteArray(frameBytes)

        try {
            while (_isCapturing && currentCoroutineContext().isActive) {
                var bytesRead = 0
                while (bytesRead < frameBytes && _isCapturing) {
                    val read = record.read(buffer, bytesRead, frameBytes - bytesRead)
                    if (read > 0) {
                        bytesRead += read
                    } else if (read < 0) {
                        break
                    }
                }

                if (bytesRead == frameBytes) {
                    emit(buffer.clone())
                }
            }
        } finally {
            stopCapture()
        }
    }.flowOn(Dispatchers.IO)

    override fun stopCapture() {
        _isCapturing = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
        }
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val FRAME_DURATION_MS = 30 // 30ms frame for Silero VAD gating
        const val BYTES_PER_SAMPLE = 2 // 16-bit mono PCM = 2 bytes per sample
        const val CHUNK_SIZE_BYTES = (SAMPLE_RATE * FRAME_DURATION_MS / 1000) * BYTES_PER_SAMPLE // 960 bytes
    }
}
