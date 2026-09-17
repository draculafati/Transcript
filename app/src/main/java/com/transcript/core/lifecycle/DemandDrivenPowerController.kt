package com.transcript.core.lifecycle

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enforces the ADR-0002 Demand-Driven Lifecycle Contract:
 * - Rule 1: Screen State Binding (<100ms pipeline cutoff when display goes dark).
 * - Rule 2: Floating Overlay Visibility Coupling (Deep sleep when hidden).
 */
class DemandDrivenPowerController(
    private val context: Context,
    private val onPipelineActivated: () -> Unit,
    private val onPipelineSuspended: () -> Unit
) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private var isScreenInteractive: Boolean = powerManager.isInteractive
    private var isOverlayVisible: Boolean = false
    private var isUserListeningRequested: Boolean = true

    private val _isPipelineActive = MutableStateFlow(false)
    val isPipelineActive: StateFlow<Boolean> = _isPipelineActive.asStateFlow()

    private val screenStateReceiver = ScreenStateReceiver(
        onScreenOff = {
            isScreenInteractive = false
            evaluateState()
        },
        onUserPresent = {
            isScreenInteractive = true
            evaluateState()
        },
        onScreenOn = {
            // Screen turned on but may still be locked; keep interactive check updated
            isScreenInteractive = powerManager.isInteractive
            evaluateState()
        }
    )

    fun startMonitoring() {
        screenStateReceiver.register(context)
        isScreenInteractive = powerManager.isInteractive
        evaluateState()
    }

    fun stopMonitoring() {
        screenStateReceiver.unregister(context)
        forciblySuspend()
    }

    fun setOverlayVisible(visible: Boolean) {
        if (isOverlayVisible != visible) {
            isOverlayVisible = visible
            evaluateState()
        }
    }

    fun setUserListeningRequested(listening: Boolean) {
        if (isUserListeningRequested != listening) {
            isUserListeningRequested = listening
            evaluateState()
        }
    }

    @Synchronized
    private fun evaluateState() {
        // Strict gating policy: ALL must be true for pipeline to draw power
        val shouldRun = isScreenInteractive && isOverlayVisible && isUserListeningRequested

        if (_isPipelineActive.value != shouldRun) {
            _isPipelineActive.value = shouldRun
            if (shouldRun) {
                onPipelineActivated()
            } else {
                forciblySuspend()
            }
        }
    }

    private fun forciblySuspend() {
        _isPipelineActive.value = false
        onPipelineSuspended()
    }
}
