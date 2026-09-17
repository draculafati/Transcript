package com.transcript.core.inference

import kotlinx.coroutines.flow.Flow

interface ISpeechRecognizer {
    fun transcribe(audioBuffer: ByteArray): Flow<String>
    fun release()
}
