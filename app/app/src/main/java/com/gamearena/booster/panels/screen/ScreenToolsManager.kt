package com.gamearena.booster.panels.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.gamearena.booster.model.ScreenToolsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenToolsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(ScreenToolsState())
    val state: StateFlow<ScreenToolsState> = _state.asStateFlow()

    private var recordingTimerJob: kotlinx.coroutines.Job? = null

    fun setBrightness(value: Int) {
        _state.value = _state.value.copy(brightness = value, isAutoBrightness = false)
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value.coerceIn(0, 255)
            )
        } catch (e: Exception) {
            // Permission not granted
        }
    }

    fun setAutoBrightness(enabled: Boolean) {
        _state.value = _state.value.copy(isAutoBrightness = enabled)
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } catch (e: Exception) {
            // Permission not granted
        }
    }

    fun toggleOrientationLock() {
        val locked = !_state.value.isOrientationLocked
        _state.value = _state.value.copy(isOrientationLocked = locked)
    }

    fun takeScreenshot(activity: Activity) {
        try {
            val view = activity.window.decorView.rootView
            view.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false

            val filename = "GameArena_${System.currentTimeMillis()}.png"
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GameArena")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(it, contentValues, null, null)
                }
            }

            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRecording() {
        _state.value = _state.value.copy(
            isRecording = true,
            recordingStartTime = System.currentTimeMillis()
        )
        recordingTimerJob = scope.launch {
            while (_state.value.isRecording) {
                delay(1000)
            }
        }
    }

    fun stopRecording() {
        _state.value = _state.value.copy(
            isRecording = false,
            recordingStartTime = null
        )
        recordingTimerJob?.cancel()
    }

    fun getRecordingDuration(): Long {
        val startTime = _state.value.recordingStartTime ?: return 0L
        return System.currentTimeMillis() - startTime
    }

    fun formatRecordingDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 60000) % 60
        val hours = (millis / 3600000)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
