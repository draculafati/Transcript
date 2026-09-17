# PROJECT BLUEPRINT: TRANSCRIPT
**Real-Time, Cross-App Battery-Efficient Audio & Screen Translator**
*Target: Android (P0 Priority) | Windows PC (P1 Extensibility)*

---

## 1. AGENT INSTRUCTION & WORKFLOW
Hey Antigravity / AI Agent! This document contains the full architectural design, decision records (ADRs), specifications, contracts, and tasks for building **Transcript**.

Your instructions:
1. Initialize the project directory structure as specified below.
2. Generate and write each documented file into `docs/adr/`, `docs/spec.md`, `docs/tasks.md`, and `docs/contracts.md`.
3. Follow the phase-wise task breakdown starting with **Phase 1: Project Setup & Baseline Scaffolding**.
4. Strictly adhere to the zero-battery-drain rules, silence gating via VAD, and offline/free-tier constraints.

---

## 2. DIRECTORY STRUCTURE TO CREATE
```text
transcript/
├── docs/
│   ├── adr/
│   │   ├── 0001-battery-optimized-hybrid-pipeline.md
│   │   ├── 0002-demand-driven-whisper-lifecycle.md
│   │   └── 0003-cross-platform-modular-layering.md
│   ├── spec.md
│   ├── tasks.md
│   └── contracts.md
├── app/
│   └── src/
│       └── main/
│           ├── java/com/transcript/
│           │   ├── core/
│           │   │   ├── audio/
│           │   │   ├── vad/
│           │   │   ├── inference/
│           │   │   ├── screen/
│           │   │   └── overlay/
│           │   └── service/
│           └── AndroidManifest.xml
```

---

## 3. ARCHITECTURE DECISION RECORDS (ADRs)

### FILE: `docs/adr/0001-battery-optimized-hybrid-pipeline.md`
```markdown
# ADR-0001: Battery-Optimized Real-Time Cross-App Audio & Screen Translation Architecture

## Status
Approved

## Date
2026-09-18

## Context & Problem Statement
The application must perform real-time, cross-app audio and text translation across mobile apps (YouTube, Instagram, Reels, TikTok, Chrome) without degrading device performance. Running continuous, unthrottled on-device automatic speech recognition (ASR) like full Whisper and continuous high-frame-rate OCR causes extreme thermal throttling, processor saturation, and drains a typical smartphone battery in under 45 minutes.

The target requirement is sub-300ms translation latency with an active battery impact of under 6% per hour.

## Decision Drivers
- Power Budget: Preserve mobile battery and stay within cool thermal limits (<6%/hr active drain).
- Latency Target: Stream translated text in near real-time (200ms–400ms end-to-end).
- Sandboxing Constraints: Operate within Android 10+ background execution and media capture boundaries.
- Cost Target: Maintain zero recurring infrastructure cost during personal use and development.

## Considered Options
1. Option A (Pure Local Edge AI): Full on-device Whisper-TFLite continuous capture + periodic MediaProjection OCR frame polling.
2. Option B (Hybrid Demand-Driven Pipeline): Silero Voice Activity Detection (VAD) gatekeeper on-device + quantized INT8 inference / lightweight cloud fallback + Accessibility Node Tree text parsing.
3. Option C (Continuous Cloud Streaming): Direct raw PCM streaming via WebSocket continuously to third-party endpoints.

## Decision Outcome
Chosen Option: Option B (Hybrid Demand-Driven Pipeline)

### Architectural Details
1. Internal Audio Ingestion: Utilize Android's AudioPlaybackCaptureConfiguration targeting media audio playback at 16kHz mono, 16-bit PCM format.
2. Audio Gating via VAD: Integrate Silero VAD (ONNX Runtime mobile engine, CPU footprint <0.5%). Audio buffers are strictly analyzed in 30ms frames; heavy translation models remain dormant until sustained speech energy is confirmed.
3. Zero-OCR Native Text Extraction: The primary text extraction mechanism is Android's AccessibilityService (AccessibilityNodeInfo tree traversal). Text strings are extracted directly from view hierarchies without camera or screenshot computation.
4. Visual Frame Diffing (Secondary Fallback): For non-text views (video frames, burned-in subtitles), frames are polled only when triggered by the user or upon detecting a >40% perceptual hash (pHash) visual delta, then processed via Google ML Kit On-Device OCR.
5. Floating Overlay UI: Render subtitles using a system alert window overlay (TYPE_APPLICATION_OVERLAY) built with Jetpack Compose.

## Consequences & Trade-offs
### Positive
- Minimizes battery consumption to roughly standard video playback levels (~5–7%/hr).
- Zero cost using local ML Kit models and free-tier quantized engines.
- Direct UI tree parsing offers 0ms latency for native app text.

### Negative & Limitations
- Android sandbox blocks audio capture on DRM-protected streams (e.g., Netflix, Prime Video where USAGE_MEDIA capture is disabled).
- Applications setting FLAG_SECURE will obscure visual screen captures.
```

---

### FILE: `docs/adr/0002-demand-driven-whisper-lifecycle.md`
```markdown
# ADR-0002: Demand-Driven Local Inference Lifecycle for Whisper.tflite

## Status
Approved

## Date
2026-09-18

## Context & Problem Statement
Running on-device neural network models (such as Whisper.tflite) continuously in a background Android service leads to thermal runaway and aggressive system kills by Android's low-memory killer (LMK). We need an explicit runtime lifecycle contract ensuring that inference operations execute only when strictly necessary.

## Decision Drivers
- Prevent background battery drain when the screen is dark or the app is idle.
- Prevent unnecessary ASR token generation during silence, sound effects, or background music.
- Maximize inference efficiency using mobile NPU/GPU delegates rather than standard CPU cores.

## Decision Outcome
Adopt a Demand-Driven Lifecycle Architecture with four mandatory system rules:

### 1. Screen State Binding
A dedicated BroadcastReceiver registers for Intent.ACTION_SCREEN_OFF. The instant the display powers down, internal audio recording buffers and ML inference loops are forcibly suspended within 100ms. Operations resume only when Intent.ACTION_USER_PRESENT is broadcast and the overlay was previously active.

### 2. Floating Overlay Visibility Coupling
Audio capture and inference loops are strictly tied to the visible lifecycle of the Floating Overlay:
- Overlay Hidden / Dismissed: Audio pipeline enters deep sleep (0% CPU, 0MB buffer growth).
- Overlay Visible (Listening Mode ON): Audio ingestion and VAD gatekeeper activate.

### 3. VAD Silence Suppression
Silero VAD operates as a gatekeeper. Chunks classified as silence, non-speech ambient noise, or instrumental music are immediately dropped from memory and never passed to the Whisper interpreter.

### 4. Hardware Delegate Offloading
The TensorFlow Lite interpreter must bind directly to NnApiDelegate or GpuDelegate. Running Whisper across standard mobile CPU cores is strictly prohibited in production builds to prevent overheating.

## Consequences & Trade-offs
### Positive
- Zero battery consumption when the device screen is off.
- Whisper runs only when active human voice is detected.
- Dedicated NPU offloading reduces inference power draw by approximately 70% compared to raw CPU processing.

### Negative & Limitations
- Initial wake-up latency when human voice begins after a long pause can introduce a 50–100ms first-token delay.
```

---

### FILE: `docs/adr/0003-cross-platform-modular-layering.md`
```markdown
# ADR-0003: Cross-Platform Modular Layering (Android-First, Windows-Ready)

## Status
Approved

## Date
2026-09-18

## Context & Problem Statement
While mobile (Android) is the primary target (P0), the translation engine architecture must accommodate future deployment to Windows desktop and laptop systems (P1) without requiring a complete rewrite of core translation, state management, and lifecycle logic.

## Decision Drivers
- High code reusability across platforms.
- Clear separation between platform-specific capture drivers and platform-agnostic business logic.
- Support for desktop-specific audio/screen access mechanics (e.g., WASAPI Loopback, Windows UI Automation).

## Decision Outcome
Adopt a Hexagonal / Clean Architecture Pattern separating platform-independent domain logic from operating system capture drivers:
- Core Domain (Cross-Platform): Voice Activity Gatekeeper, Token Aggregator & Streamer, Master Translation State Engine.
- Android Driver (P0): AudioPlaybackCapture API, Accessibility Node Service, Jetpack Compose Overlay, TFLite NNAPI Delegate.
- Windows Driver (P1): WASAPI Loopback Capture, Windows UI Automation API, Transparent Always-on-Top WinUI, whisper.cpp / DirectML ONNX.
```

---

## 4. SYSTEM SPECIFICATION

### FILE: `docs/spec.md`
```markdown
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
```

---

## 5. CONTRACTS & INTERFACES

### FILE: `docs/contracts.md`
```markdown
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
```

---

## 6. IMPLEMENTATION TASKS

### FILE: `docs/tasks.md`
```markdown
# Implementation Tasks: Transcript (Android P0)

## Phase 1: Project Setup & Baseline Scaffolding
- [ ] Task 1.1: Project Initialization (compileSdk = 35, minSdk = 29, Compose enabled).
- [ ] Task 1.2: Dependency Configuration (TFLite, NNAPI Delegate, ONNX Runtime Mobile, ML Kit).

## Phase 2: Floating Overlay & Lifecycle Management
- [ ] Task 2.1: Floating Window Service (WindowManager Compose overlay with edge-docking pill).
- [ ] Task 2.2: Demand-Driven Power Controller (BroadcastReceiver for ACTION_SCREEN_OFF).

## Phase 3: Audio Pipeline & Voice Activity Detection
- [ ] Task 3.1: Internal Audio Recorder (AudioPlaybackCapture at 16kHz Mono).
- [ ] Task 3.2: Silero VAD Integration (30ms frame gating).

## Phase 4: On-Device Speech Recognition & Translation
- [ ] Task 4.1: Quantized Whisper Interpreter (whisper-tiny-int8.tflite via NNAPI).
- [ ] Task 4.2: Translation Engine (ML Kit On-Device Translation).

## Phase 5: Accessibility Native Text Extraction
- [ ] Task 5.1: Accessibility Service Implementation (View hierarchy text parsing).
- [ ] Task 5.2: Tap-to-Translate Action (Double tap edge pill trigger).
```