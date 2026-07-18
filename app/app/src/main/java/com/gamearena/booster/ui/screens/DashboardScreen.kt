package com.gamearena.booster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import com.gamearena.booster.overlay.OverlayService
import com.gamearena.booster.ui.components.PrimaryButton
import com.gamearena.booster.ui.components.QuickActionButton
import com.gamearena.booster.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    metricsEngine: com.gamearena.booster.metrics.MetricsEngine
) : ViewModel() {
    val fpsHistory = metricsEngine.fpsHistory
}

@Composable
fun DashboardScreen(
    onNavigateToAppearance: () -> Unit,
    onNavigateToOverlayCustomization: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onNavigateToThermalDiagnostics: () -> Unit,
    onNavigateToTournaments: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isOverlayRunning by OverlayService.isRunning.collectAsState()
    val fpsHistory by dashboardViewModel.fpsHistory.collectAsState()
    val context = LocalContext.current

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = com.gamearena.booster.R.mipmap.ic_launcher),
                    contentDescription = "Logo",
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "GameArena",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            IconButton(
                onClick = onNavigateToAbout,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "OVERLAY STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = if (isOverlayRunning) "Monitoring Active" else "Ready to Monitor",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(if (isOverlayRunning) Color(0xFF22C55E).copy(alpha = 0.1f) else Color.Gray.copy(0.1f), CircleShape)
                            .border(1.dp, if (isOverlayRunning) Color(0xFF22C55E).copy(alpha = 0.2f) else Color.Gray.copy(0.2f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(if (isOverlayRunning) Color(0xFF22C55E) else Color.Gray, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isOverlayRunning) "ACTIVE" else "INACTIVE", style = MaterialTheme.typography.labelSmall, color = if (isOverlayRunning) Color(0xFF22C55E) else Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    val lineColor = MaterialTheme.colorScheme.primary
                    val gridColor = Color.White.copy(alpha = 0.04f)
                    if (fpsHistory.isNotEmpty()) {
                        Text(
                            text = "${fpsHistory.last()} FPS",
                            color = lineColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    } else {
                        Text(
                            text = "FPS Graph",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, h * frac), end = androidx.compose.ui.geometry.Offset(w, h * frac), strokeWidth = 1.dp.toPx())
                        }
                        val history = fpsHistory
                        if (history.size >= 2) {
                            val maxFps = history.max().coerceAtLeast(1)
                            val path = androidx.compose.ui.graphics.Path()
                            history.forEachIndexed { i, fps ->
                                val x = w * i / (history.size - 1).toFloat()
                                val y = h * (1f - fps.toFloat() / maxFps)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2.5f.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                addPath(path)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(fillPath, color = lineColor.copy(alpha = 0.08f))
                        } else {
                            drawLine(
                                color = lineColor.copy(alpha = 0.3f),
                                start = androidx.compose.ui.geometry.Offset(0f, h),
                                end = androidx.compose.ui.geometry.Offset(w, h),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                if (isOverlayRunning) {
                    Button(
                        onClick = {
                            val intent = Intent(context, OverlayService::class.java).apply {
                                action = OverlayService.ACTION_STOP
                            }
                            context.startService(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.2f), contentColor = Color.Red),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Stop Overlay", fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (hasOverlayPermission) {
                        PrimaryButton(
                            text = "Start Overlay",
                            onClick = {
                                val intent = Intent(context, OverlayService::class.java)
                                context.startForegroundService(intent)
                            },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp)) }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Missing: Overlay permission",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onNavigateToPermissions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFBBF24).copy(alpha = 0.12f),
                                    contentColor = Color(0xFFFBBF24)
                                ),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Complete Setup First", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    title = "Metrics",
                    subtitle = "FPS, CPU, GPU, RAM",
                    iconContainerColor = Color(0xFF6366F1).copy(alpha = 0.1f),
                    iconContentColor = Color(0xFF818CF8),
                    onClick = onNavigateToOverlayCustomization,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = { Icon(Icons.Default.DashboardCustomize, null) }
                )
                QuickActionButton(
                    title = "Theme",
                    subtitle = "Colors, Opacity, Size",
                    iconContainerColor = Color(0xFFF59E0B).copy(alpha = 0.1f),
                    iconContentColor = Color(0xFFFBBF24),
                    onClick = onNavigateToAppearance,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = { Icon(Icons.Default.Palette, null) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    title = "Performance",
                    subtitle = "Game Mode · RAM Boost",
                    iconContainerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                    iconContentColor = Color(0xFF34D399),
                    onClick = onNavigateToPerformance,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = { Icon(Icons.Default.Bolt, null) }
                )
                QuickActionButton(
                    title = "Permissions",
                    subtitle = "Setup & Access",
                    iconContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                    iconContentColor = Color(0xFF60A5FA),
                    onClick = onNavigateToPermissions,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = { Icon(Icons.Default.Speed, null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    title = "Tournaments",
                    subtitle = "Create · Join · Play",
                    iconContainerColor = Color(0xFFFFD700).copy(alpha = 0.1f),
                    iconContentColor = Color(0xFFFFD700),
                    onClick = onNavigateToTournaments,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = { Icon(Icons.Default.EmojiEvents, null) }
                )
                QuickActionButton(
                    title = "Thermal Diagnostics",
                    subtitle = "Find what's causing frame drops",
                    iconContainerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                    iconContentColor = Color(0xFFF87171),
                    onClick = onNavigateToThermalDiagnostics,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = { Icon(Icons.Default.LocalFireDepartment, null) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
