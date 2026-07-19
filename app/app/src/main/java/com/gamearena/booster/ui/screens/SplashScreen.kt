package com.gamearena.booster.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.gamearena.booster.repository.SettingsRepository
import com.gamearena.booster.network.GameArenaApi
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val api: GameArenaApi
) : ViewModel() {
    val isOnboardingCompleted = settingsRepository.isOnboardingCompleted

    init {
        viewModelScope.launch {
            val isFirstLaunch = !settingsRepository.isOnboardingCompleted.value
            val deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            } catch (_: Exception) { "unknown" }
            val version = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            } catch (_: Exception) { "1.0.0" }

            try {
                api.trackEvent(mapOf(
                    "event_type" to if (isFirstLaunch) "download" else "active",
                    "device_id" to deviceId,
                    "app_version" to version,
                    "platform" to "android"
                ))
                if (isFirstLaunch) {
                    api.trackEvent(mapOf(
                        "event_type" to "install",
                        "device_id" to deviceId,
                        "app_version" to version,
                        "platform" to "android"
                    ))
                }
            } catch (e: Exception) {
                Log.d("GameArena", "Analytics track failed: ${e.message}")
            }
        }
    }
}

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

    LaunchedEffect(key1 = true) {
        delay(1500)
        if (!isOnboardingCompleted) {
            onNavigateToOnboarding()
        } else {
            onNavigateToDashboard()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = com.gamearena.booster.R.mipmap.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "GameArena",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Text(
                text = "PERFORMANCE SUITE",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Standalone Game Booster",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
        }
    }
}
