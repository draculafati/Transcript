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
