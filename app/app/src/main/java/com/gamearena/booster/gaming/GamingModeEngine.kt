package com.gamearena.booster.gaming

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.Settings
import com.gamearena.booster.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// State model
// ---------------------------------------------------------------------------

sealed class GamingModeState {
    object Idle : GamingModeState()
    data class Enabling(val progress: Float = 0f, val statusText: String = "Preparing…") : GamingModeState()
    object Active : GamingModeState()
    object Disabling : GamingModeState()
    data class Error(val message: String) : GamingModeState()
}

data class AppInfo(
    val packageName: String,
    val label: String
)

// ---------------------------------------------------------------------------
// Engine — standalone, no shell/root/ADB/Shizuku required
// ---------------------------------------------------------------------------

@Singleton
class GamingModeEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // ---- Public state -------------------------------------------------------

    private val _state = MutableStateFlow<GamingModeState>(GamingModeState.Idle)
    val state: StateFlow<GamingModeState> = _state.asStateFlow()

    companion object {
        private val _isActive = MutableStateFlow(false)
        val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

        internal const val RECOVERY_NOTIFICATION_ID = 3
    }

    // ---- Public API ---------------------------------------------------------

    fun getInstalledUserApps(): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { ai ->
                (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { ai ->
                AppInfo(
                    packageName = ai.packageName,
                    label = pm.getApplicationLabel(ai).toString()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Returns Google apps installed on this device for the whitelist UI.
     */
    fun getGoogleAppsForWhitelist(): List<AppInfo> {
        val pm = context.packageManager
        val googlePackages = listOf(
            "com.google.android.youtube",
            "com.google.android.apps.photos",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.google.android.calendar",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.nbu.files",
            "com.google.android.apps.wellbeing",
            "com.android.chrome"
        )
        return googlePackages.mapNotNull { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    packageName = ai.packageName,
                    label = pm.getApplicationLabel(ai).toString()
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    /**
     * Full Gaming Mode activation — uses only public Android APIs.
     *
     * 1. Kill cached background processes via ActivityManager
     * 2. Enable DND via NotificationManager (if permission granted)
     * 3. Apply per-game settings (volume, brightness, rotation)
     */
    suspend fun enableGamingMode(userWhitelist: Set<String>, activeGamePkg: String? = null) {
        _state.value = GamingModeState.Enabling(0f, "Initializing…")

        val prefs = context.getSharedPreferences("GameArena_settings", Context.MODE_PRIVATE)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var finalWhitelist = userWhitelist

        if (activeGamePkg != null) {
            finalWhitelist = finalWhitelist + activeGamePkg

            // Save original ringtone volume
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            prefs.edit().putInt("orig_ringtone_val", currentVol).apply()

            // Change ringtone volume per game config
            val targetVolPct = settingsRepository.getGameConfigRingtoneVol(activeGamePkg)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val targetVol = (targetVolPct / 100f * maxVol).toInt().coerceIn(0, maxVol)
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0)
            } catch (_: Exception) {}

            // Settings overrides (auto-brightness, auto-rotate)
            val canWrite = Settings.System.canWrite(context)
            if (canWrite) {
                if (settingsRepository.getGameConfigDisableBrightness(activeGamePkg)) {
                    val origBrightnessMode = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    )
                    prefs.edit().putInt("orig_brightness_mode", origBrightnessMode).apply()
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                }

                if (settingsRepository.getGameConfigDisableRotate(activeGamePkg)) {
                    val origRotation = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION, 1
                    )
                    prefs.edit().putInt("orig_rotation_mode", origRotation).apply()
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION, 0
                    )
                }
            }
        }

        val isAlreadyActive = _isActive.value

        // Phase 0 — Restart notification listener (fixes OEM "coma" state)
        if (!isAlreadyActive) {
            try {
                val component = ComponentName(context, GamingNotificationListener::class.java)
                context.packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                kotlinx.coroutines.delay(100)
                context.packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}
        }

        try {
            // Phase 1 — Kill cached background processes
            _state.value = GamingModeState.Enabling(0.5f, "Freeing RAM…")
            killBackgroundProcesses()

            // Phase 2 — Enable DND
            if (!isAlreadyActive) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                }
            }

            // Done
            settingsRepository.setGamingModeActive(true)
            _isActive.value = true
            _state.value = GamingModeState.Active

        } catch (e: Exception) {
            _state.value = GamingModeState.Error(e.message ?: "Unexpected error during activation")
            settingsRepository.setGamingModeActive(false)
            _isActive.value = false
        }
    }

    /**
     * Kill all cached background processes using the public ActivityManager API.
     * This requires KILL_BACKGROUND_PROCESSES permission (granted automatically
     * on install). Only kills cached processes, not foreground services.
     */
    private fun killBackgroundProcesses() {
        val pm = context.packageManager
        val runningApps = getInstalledUserApps()
        for (app in runningApps) {
            try {
                activityManager.killBackgroundProcesses(app.packageName)
            } catch (_: Exception) {
                // Some OEMs restrict this — non-critical
            }
        }
    }

    /**
     * Full Gaming Mode deactivation — restores all settings.
     */
    suspend fun disableGamingMode() {
        _state.value = GamingModeState.Disabling

        try {
            // Restore DND
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }

            // Restore original settings
            val prefs = context.getSharedPreferences("GameArena_settings", Context.MODE_PRIVATE)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Restore ringtone volume
            val origVol = prefs.getInt("orig_ringtone_val", -1)
            if (origVol != -1) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, origVol, 0)
                } catch (_: Exception) {}
                prefs.edit().remove("orig_ringtone_val").apply()
            }

            // Restore brightness and rotation
            if (Settings.System.canWrite(context)) {
                val origMode = prefs.getInt("orig_brightness_mode", -1)
                if (origMode != -1) {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE, origMode
                    )
                    prefs.edit().remove("orig_brightness_mode").apply()
                }
                val origRotate = prefs.getInt("orig_rotation_mode", -1)
                if (origRotate != -1) {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION, origRotate
                    )
                    prefs.edit().remove("orig_rotation_mode").apply()
                }
            }

        } catch (_: Exception) {
            // Always transition to Idle even on partial failure
        }

        settingsRepository.setGamingModeActive(false)
        _isActive.value = false
        _state.value = GamingModeState.Idle
    }

    /** Called on app start-up to recover state that was active before a kill. */
    fun recoverPersistedState() {
        if (settingsRepository.isGamingModeActive()) {
            _isActive.value = true
            _state.value = GamingModeState.Active
        }
    }
}
