package com.transcript.core.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete implementation of IOverlayPresenter managing the floating system window
 * lifecycle and Jetpack Compose bridge.
 */
class ComposeOverlayPresenter(
    private val context: Context,
    private val onListeningToggled: ((Boolean) -> Unit)? = null,
    private val onOverlayVisibilityChanged: ((Boolean) -> Unit)? = null,
    private val onTranslateScreenRequested: (() -> Unit)? = null
) : IOverlayPresenter {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val _subtitleText = MutableStateFlow("")
    val subtitleText: StateFlow<String> = _subtitleText.asStateFlow()

    private val _isListening = MutableStateFlow(true)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isCollapsed = MutableStateFlow(false)
    val isCollapsed: StateFlow<Boolean> = _isCollapsed.asStateFlow()

    private var isAttached = false
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun showCapsule() {
        if (isAttached) return

        lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth * 0.1).toInt()
            y = (screenHeight * 0.2).toInt()
        }

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val currentSubtitle by _subtitleText.collectAsState()
                val currentListening by _isListening.collectAsState()
                val currentCollapsed by _isCollapsed.collectAsState()

                OverlayCapsule(
                    subtitleText = currentSubtitle,
                    isListening = currentListening,
                    isCollapsed = currentCollapsed,
                    onDragDelta = { dx, dy ->
                        updateWindowPosition(dx.toInt(), dy.toInt(), screenWidth, screenHeight)
                    },
                    onToggleListening = {
                        val newState = !_isListening.value
                        _isListening.value = newState
                        onListeningToggled?.invoke(newState)
                    },
                    onToggleCollapsed = {
                        _isCollapsed.value = !_isCollapsed.value
                    },
                    onTranslateScreen = {
                        onTranslateScreenRequested?.invoke()
                    },
                    onDismiss = {
                        hideCapsule()
                    }
                )
            }
        }

        composeView = view
        try {
            windowManager.addView(view, layoutParams)
            isAttached = true
            onOverlayVisibilityChanged?.invoke(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun hideCapsule() {
        if (!isAttached || composeView == null) return

        try {
            windowManager.removeView(composeView)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            composeView = null
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
            isAttached = false
            onOverlayVisibilityChanged?.invoke(false)
        }
    }

    override fun updateSubtitle(token: String) {
        _subtitleText.value = token
    }

    override fun setListeningState(isListening: Boolean) {
        _isListening.value = isListening
    }

    private fun updateWindowPosition(
        dx: Int,
        dy: Int,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (!isAttached || composeView == null) return

        layoutParams.x = (layoutParams.x + dx).coerceIn(0, screenWidth - 100)
        layoutParams.y = (layoutParams.y + dy).coerceIn(50, screenHeight - 150)

        try {
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isVisible(): Boolean = isAttached
}
