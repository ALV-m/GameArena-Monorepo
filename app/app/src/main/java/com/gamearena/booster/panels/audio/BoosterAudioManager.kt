package com.gamearena.booster.panels.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioManager
import com.gamearena.booster.model.AudioState
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
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    init {
        scope.launch {
            while (true) {
                updateBluetoothState()
                delay(5000)
            }
        }
    }

    fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val androidVolume = (clamped * maxVolume / 100)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, androidVolume, 0)
        _state.value = _state.value.copy(volume = clamped)
    }

    fun toggleMicrophone() {
        val muted = !_state.value.isMicrophoneMuted
        _state.value = _state.value.copy(isMicrophoneMuted = muted)
        try {
            audioManager.isMicrophoneMute = muted
        } catch (e: Exception) {
            // May not be allowed on some devices
        }
    }

    fun setOutputDevice(deviceName: String) {
        _state.value = _state.value.copy(outputDevice = deviceName)
    }

    fun getCurrentVolume(): Int {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100 / maxVolume).coerceIn(0, 100)
    }

    private fun updateBluetoothState() {
        try {
            val btAdapter = bluetoothManager?.adapter
            val isConnected = btAdapter?.isEnabled == true && btAdapter?.bondedDevices?.isNotEmpty() == true
            val deviceName = if (isConnected) {
                btAdapter?.bondedDevices?.firstOrNull()?.name ?: "Unknown"
            } else null

            _state.value = _state.value.copy(
                isBluetoothConnected = isConnected,
                bluetoothDeviceName = deviceName
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isBluetoothConnected = false,
                bluetoothDeviceName = null
            )
        }
    }

    fun isMusicActive(): Boolean {
        return audioManager.isMusicActive
    }
}
