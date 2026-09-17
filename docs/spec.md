# System Specification: Transcript

## 1. Product Overview
Transcript is a battery-efficient, cross-app live translation utility designed to extract, transcribe, and translate spoken audio and screen text in real time from third-party applications (YouTube, Instagram, TikTok, web browsers) without intrusive UI elements or rapid battery drain.

## 2. Core Functional Requirements
- FR-1 (Live Audio Capture): Capture internal device sound from any media-playing app using Android's AudioPlaybackCapture API (Android 10+, API Level 29+).
- FR-2 (Silence Suppression): Filter incoming audio using Silero VAD to discard silence and music before invoking the ASR engine.
- FR-3 (On-Device Speech-to-Text): Transcribe captured speech chunks using a quantized whisper-tiny-int8.tflite model running over the NNAPI/GPU hardware delegate.
- FR-4 (Native Screen Text Extraction): Read focused text elements directly from Android's active accessibility hierarchy using AccessibilityService without running visual OCR.
- FR-5 (Visual Frame OCR Fallback): For non-text UI elements (e.g., video frames), run Google ML Kit On-Device OCR only when triggered by the user or upon detecting significant visual frame divergence.
- FR-6 (Minimalist Floating HUD): Display translated subtitles in a draggable, frosted-glass floating capsule rendered via WindowManager (TYPE_APPLICATION_OVERLAY).
- FR-7 (Battery Safeguards): Terminate all audio recording and ML pipelines when the screen turns off or the overlay is hidden.

## 3. Non-Functional & Performance Targets
- End-to-End Latency: <350ms from audio utterance to overlay subtitle rendering.
- Battery Impact: <6% battery consumption per continuous hour of active playback.
- Memory Footprint: <150MB resident memory footprint in background operation.
- Cost: 100% zero recurring API costs using on-device models and open-source runtimes.

## 4. Platform Support
- Primary (P0): Android 10 (API 29) to Android 15 (API 35).
- Secondary (P1): Windows 10/11 x64 (via WASAPI Loopback and whisper.cpp).
