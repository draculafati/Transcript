package com.transcript.core.vad

interface IVoiceActivityDetector {
    fun analyzeChunk(pcmChunk: ByteArray): Float
    fun resetState()
}
