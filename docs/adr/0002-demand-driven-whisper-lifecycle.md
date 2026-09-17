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
