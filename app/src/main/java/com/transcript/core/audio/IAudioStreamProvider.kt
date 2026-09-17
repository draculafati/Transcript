package com.transcript.core.audio

import kotlinx.coroutines.flow.Flow

interface IAudioStreamProvider {
    fun startCapture(): Flow<ByteArray>
    fun stopCapture()
    val isCapturing: Boolean
}
