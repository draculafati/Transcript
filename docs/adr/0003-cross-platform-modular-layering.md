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
