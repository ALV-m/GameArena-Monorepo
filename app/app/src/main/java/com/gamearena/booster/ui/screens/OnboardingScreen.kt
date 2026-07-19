package com.gamearena.booster.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.gamearena.booster.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int
)

val OnboardingPages = listOf(
    OnboardingPage(
        title = "FPS-First Philosophy",
        description = "GameArena prioritizes frame rates above all else. Experience a modular system designed to keep your workflow at maximum refresh rates.",
        imageRes = com.gamearena.booster.R.drawable.img_onboarding_fps
    ),
    OnboardingPage(
        title = "Real-time Metrics",
        description = "Monitor your device's core vitals including CPU, RAM, and GPU workloads in a single glance without leaving your game.",
        imageRes = com.gamearena.booster.R.drawable.img_onboarding_metrics
    ),
    OnboardingPage(
        title = "Standalone Performance",
        description = "GameArena monitors CPU, RAM, FPS, battery, and network directly — no root, no ADB, no external tools needed.",
        imageRes = com.gamearena.booster.R.drawable.img_onboarding_metrics
    ),
    OnboardingPage(
        title = "Tournaments",
        description = "Compete in real-time tournaments! Join or create tournaments, track your performance, and climb the leaderboards. Wallet and matchmaking features are available when you're ready to compete.",
        imageRes = com.gamearena.booster.R.drawable.img_onboarding_fps
    )
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val accentColor = MaterialTheme.colorScheme.primary
    val totalPages = OnboardingPages.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.gamearena.booster.R.mipmap.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("GameArena", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        AnimatedContent(
            targetState = currentPage,
            modifier = Modifier.weight(1f),
            label = "onboarding_pager"
        ) { page ->
            val pageData = OnboardingPages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = pageData.imageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = pageData.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = pageData.description,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalPages) { index ->
                    val isSelected = currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 32.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color.DarkGray)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    viewModel.completeOnboarding()
                    onFinishOnboarding()
                }) {
                    Text("Skip", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                        } else {
                            viewModel.completeOnboarding()
                            onFinishOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        if (currentPage == totalPages - 1) "Start" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
