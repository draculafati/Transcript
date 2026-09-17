package com.transcript.core.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * BroadcastReceiver listening for device screen and unlock transitions.
 * Implements ADR-0002 Mandatory Rule 1 (Screen State Binding).
 */
class ScreenStateReceiver(
    private val onScreenOff: () -> Unit,
    private val onUserPresent: () -> Unit,
    private val onScreenOn: (() -> Unit)? = null
) : BroadcastReceiver() {

    private var isRegistered = false

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
            Intent.ACTION_USER_PRESENT -> onUserPresent()
            Intent.ACTION_SCREEN_ON -> onScreenOn?.invoke()
        }
    }

    fun register(context: Context) {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(this, filter)
        isRegistered = true
    }

    fun unregister(context: Context) {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRegistered = false
        }
    }
}
