package com.gamearena.booster.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.ByteArrayOutputStream

class ScreenCaptureManager(
    private val context: Context,
    private val onFrameCaptured: (Bitmap) -> Unit
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (isCapturing) {
                captureFrame()
                handler.postDelayed(this, CAPTURE_INTERVAL_MS)
            }
        }
    }

    fun startCapture(projection: MediaProjection) {
        if (isCapturing) return
        mediaProjection = projection

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels / 2
        val height = metrics.heightPixels / 2
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2
        )

        virtualDisplay = projection.createVirtualDisplay(
            "GameArena_Capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, handler
        )

        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                if (cropped != bitmap) bitmap.recycle()

                onFrameCaptured(cropped)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
        }, handler)

        isCapturing = true
        handler.postDelayed(captureRunnable, CAPTURE_INTERVAL_MS)
    }

    fun stopCapture() {
        isCapturing = false
        handler.removeCallbacks(captureRunnable)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    private fun captureFrame() {
        imageReader?.acquireLatestImage()?.close()
    }

    companion object {
        const val CAPTURE_INTERVAL_MS = 5000L
        const val REQUEST_MEDIA_PROJECTION = 1001
    }
}
