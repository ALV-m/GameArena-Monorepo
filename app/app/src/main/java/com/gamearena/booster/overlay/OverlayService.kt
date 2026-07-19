package com.gamearena.booster.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gamearena.booster.network.GameArenaApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var boosterOverlayManager: BoosterOverlayManager

    @Inject
    lateinit var api: GameArenaApi

    private var wakeLock: PowerManager.WakeLock? = null
    private var edgeGestureManager: EdgeGestureManager? = null
    private var screenCaptureManager: ScreenCaptureManager? = null
    private var scoreOcr: ScoreOcr? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeMatchId: String? = null

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        getSharedPreferences("GameArena_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("overlay_was_running", true).apply()
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, createNotification())
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GameArena::OverlayWakeLock"
        ).also { it.acquire() }

        overlayManager.showOverlay()
        boosterOverlayManager.showOverlay()

        edgeGestureManager = EdgeGestureManager(
            context = this,
            windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager,
            onSwipeIn = {
                overlayManager.setOverlayVisible(true)
                boosterOverlayManager.setOverlayVisible(true)
            }
        )
        edgeGestureManager?.show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCapture()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_CAPTURE -> {
                @Suppress("DEPRECATION")
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                activeMatchId = intent.getStringExtra(EXTRA_MATCH_ID)
                if (resultCode != 0 && data != null) {
                    startScreenCapture(resultCode, data)
                }
            }
            ACTION_STOP_CAPTURE -> {
                stopCapture()
            }
        }
        return START_STICKY
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)

        scoreOcr = ScoreOcr(this)
        screenCaptureManager = ScreenCaptureManager(this) { bitmap ->
            scope.launch {
                try {
                    val result = scoreOcr?.analyzeScreenshot(bitmap)
                    if (result != null && result.success && activeMatchId != null) {
                        Log.d(TAG, "OCR detected score: ${result.player1Score}-${result.player2Score} (confidence: ${result.confidence})")
                        submitOcrResult(activeMatchId!!, result)
                    }
                    bitmap.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Score extraction failed", e)
                }
            }
        }
        screenCaptureManager?.startCapture(projection)
        updateNotification("Auto-capturing match scores")
    }

    private fun stopCapture() {
        screenCaptureManager?.stopCapture()
        screenCaptureManager = null
        activeMatchId = null
        updateNotification("Swipe from left edge to open overlay")
    }

    private suspend fun submitOcrResult(matchId: String, result: OcrScoreResult) {
        try {
            val body = mapOf(
                "player1_score" to (result.player1Score ?: return),
                "player2_score" to (result.player2Score ?: return),
                "confidence" to result.confidence,
                "raw_text" to result.detectedText,
                "source" to "auto_ocr"
            )
            val response = api.submitOcrResult(matchId, body)
            if (response.isSuccessful) {
                Log.d(TAG, "OCR result submitted for match $matchId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit OCR result", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        scope.cancel()
        screenCaptureManager?.stopCapture()
        scoreOcr = null
        getSharedPreferences("GameArena_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("overlay_was_running", false).apply()
        edgeGestureManager?.hide()
        edgeGestureManager = null
        overlayManager.hideOverlay()
        boosterOverlayManager.hideOverlay()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayManager.handleOrientationChange(newConfig.orientation)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GameArena Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GameArena Booster Active")
            .setContentText("Swipe from left edge to open overlay")
            .setSmallIcon(com.gamearena.booster.R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GameArena Booster Active")
            .setContentText(text)
            .setSmallIcon(com.gamearena.booster.R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
        manager.notify(1, notification)
    }

    companion object {
        const val TAG = "OverlayService"
        const val CHANNEL_ID = "GameArena_overlay_channel"
        const val ACTION_STOP = "com.gamearena.booster.ACTION_STOP_OVERLAY"
        const val ACTION_START_CAPTURE = "com.gamearena.booster.ACTION_START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.gamearena.booster.ACTION_STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_MATCH_ID = "match_id"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
