# Implementation Tasks: Transcript (Android P0)

## Phase 1: Project Setup & Baseline Scaffolding
- [x] Task 1.1: Project Initialization (compileSdk = 35, minSdk = 29, Compose enabled).
- [x] Task 1.2: Dependency Configuration (TFLite, NNAPI Delegate, ONNX Runtime Mobile, ML Kit).

## Phase 2: Floating Overlay & Lifecycle Management
- [x] Task 2.1: Floating Window Service (WindowManager Compose overlay with edge-docking pill).
- [x] Task 2.2: Demand-Driven Power Controller (BroadcastReceiver for ACTION_SCREEN_OFF).

## Phase 3: Audio Pipeline & Voice Activity Detection
- [x] Task 3.1: Internal Audio Recorder (AudioPlaybackCapture at 16kHz Mono).
- [x] Task 3.2: Silero VAD Integration (30ms frame gating).

## Phase 4: On-Device Speech Recognition & Translation
- [x] Task 4.1: Quantized Whisper Interpreter (whisper-tiny-int8.tflite via NNAPI).
- [x] Task 4.2: Translation Engine (ML Kit On-Device Translation).

## Phase 5: Accessibility Native Text Extraction
- [x] Task 5.1: Accessibility Service Implementation (View hierarchy text parsing).
- [x] Task 5.2: Tap-to-Translate Action (Double tap edge pill trigger).
