package com.gamearena.booster.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gamearena.booster.ui.theme.GameArenaTheme

class EdgeGestureManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onSwipeIn: () -> Unit
) {
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun show() {
        if (composeView != null) return

        composeView = ComposeView(context).apply {
            setContent {
                GameArenaTheme {
                    EdgeStrip(onSwipeIn = onSwipeIn)
                }
            }
        }

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner
        owner.performRestore(null)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView?.setViewTreeLifecycleOwner(owner)
        composeView?.setViewTreeSavedStateRegistryOwner(owner)
        composeView?.setViewTreeViewModelStoreOwner(owner)
        composeView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val stripWidthPx = (20 * context.resources.displayMetrics.density).toInt()

        val params = WindowManager.LayoutParams(
            stripWidthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.RGBA_8888
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            composeView = null
            lifecycleOwner = null
        }
    }
}

@Composable
private fun EdgeStrip(onSwipeIn: () -> Unit) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .fillMaxHeight()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 30) {
                            onSwipeIn()
                        }
                    }
                )
            }
    )
}
