# Architectural Contracts & Interfaces

## 1. Audio Capture Contract
```kotlin
package com.transcript.core.audio

import kotlinx.coroutines.flow.Flow

interface IAudioStreamProvider {
    fun startCapture(): Flow<ByteArray>
    fun stopCapture()
    val isCapturing: Boolean
}
```

## 2. Voice Activity Detection Contract
```kotlin
package com.transcript.core.vad

interface IVoiceActivityDetector {
    fun analyzeChunk(pcmChunk: ByteArray): Float
    fun resetState()
}
```

## 3. Speech Recognition Engine Contract
```kotlin
package com.transcript.core.inference

import kotlinx.coroutines.flow.Flow

interface ISpeechRecognizer {
    fun transcribe(audioBuffer: ByteArray): Flow<String>
    fun release()
}
```

## 4. Screen Text Extraction Contract
```kotlin
package com.transcript.core.screen

interface ITextExtractor {
    fun extractForegroundText(): List<String>
}
```

## 5. Overlay Display Contract
```kotlin
package com.transcript.core.overlay

interface IOverlayPresenter {
    fun showCapsule()
    fun hideCapsule()
    fun updateSubtitle(token: String)
    fun setListeningState(isListening: Boolean)
}
```
