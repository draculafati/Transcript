package com.transcript.core.vad

import com.transcript.core.audio.IAudioStreamProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Gating pipeline that enforces silence suppression on the raw PCM audio stream.
 * Emits speech segments ready for Whisper inference only when confirmed speech is present.
 */
class VadAudioGater(
    private val audioProvider: IAudioStreamProvider,
    private val vad: IVoiceActivityDetector,
    private val speechThreshold: Float = 0.5f,
    private val preSpeechPaddingChunks: Int = 3,  // ~90ms pre-roll
    private val postSpeechHangoverChunks: Int = 10 // ~300ms hangover
) {

    /**
     * Filters the raw PCM stream from audioProvider, emitting contiguous speech buffers.
     * Non-speech/silence is discarded immediately to satisfy ADR-0001 and ADR-0002.
     */
    fun getSpeechStream(): Flow<ByteArray> = flow {
        val preRollBuffer = ArrayDeque<ByteArray>(preSpeechPaddingChunks)
        val speechAccumulator = mutableListOf<ByteArray>()
        var silenceCounter = 0
        var isSpeaking = false

        audioProvider.startCapture().collect { pcmChunk ->
            val score = vad.analyzeChunk(pcmChunk)
            val isSpeech = score >= speechThreshold

            if (isSpeech) {
                if (!isSpeaking) {
                    isSpeaking = true
                    // Prepend pre-roll buffer to prevent cutting off the initial syllable
                    while (preRollBuffer.isNotEmpty()) {
                        speechAccumulator.add(preRollBuffer.removeFirst())
                    }
                }
                speechAccumulator.add(pcmChunk)
                silenceCounter = 0
            } else {
                if (isSpeaking) {
                    speechAccumulator.add(pcmChunk)
                    silenceCounter++

                    // If silence persists beyond the hangover duration, flush the utterance
                    if (silenceCounter >= postSpeechHangoverChunks) {
                        val fullUtterance = concatenateChunks(speechAccumulator)
                        speechAccumulator.clear()
                        isSpeaking = false
                        silenceCounter = 0
                        vad.resetState()
                        emit(fullUtterance)
                    }
                } else {
                    // Update circular pre-roll buffer during silence
                    if (preRollBuffer.size >= preSpeechPaddingChunks) {
                        preRollBuffer.removeFirst()
                    }
                    preRollBuffer.addLast(pcmChunk)
                    // Discard silence immediately (ADR-0002 Rule 3)
                }
            }
        }
    }

    private fun concatenateChunks(chunks: List<ByteArray>): ByteArray {
        val totalSize = chunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.size)
            offset += chunk.size
        }
        return result
    }
}
