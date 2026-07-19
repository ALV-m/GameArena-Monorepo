package com.gamearena.booster.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gamearena.booster.metrics.MetricsState
import com.gamearena.booster.model.*
import com.gamearena.booster.ui.theme.GameArenaTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class OverlayPanel(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    PERFORMANCE("Perf", Icons.Outlined.Speed, Icons.Filled.Speed),
    TOURNAMENT("Match", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
    NOTIFICATIONS("Alerts", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    CHAT("Chat", Icons.Outlined.Chat, Icons.Filled.Chat),
    CLIPBOARD("Copy", Icons.Outlined.ContentCopy, Icons.Filled.ContentCopy),
    AUDIO("Audio", Icons.Outlined.VolumeUp, Icons.Filled.VolumeUp),
    SCREEN("Screen", Icons.Outlined.Screenshot, Icons.Filled.Screenshot),
    WALLET("Wallet", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet)
}

@Singleton
class BoosterOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: com.gamearena.booster.repository.SettingsRepository,
    private val metricsEngine: com.gamearena.booster.metrics.MetricsEngine
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _selectedPanel = MutableStateFlow(OverlayPanel.PERFORMANCE)
    val selectedPanel: StateFlow<OverlayPanel> = _selectedPanel.asStateFlow()

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    private var onScreenshotCallback: (() -> Unit)? = null

    fun setScreenshotCallback(callback: () -> Unit) {
        onScreenshotCallback = callback
    }

    fun showOverlay() {
        if (composeView != null) {
            _isVisible.value = true
            return
        }

        composeView = ComposeView(context).apply {
            setContent {
                GameArenaTheme {
                    val visible by _isVisible.collectAsState()
                    val metricsState by metricsEngine.metricsState.collectAsState()
                    val isExpanded by _isExpanded.collectAsState()
                    val selectedPanel by _selectedPanel.collectAsState()
                    val opacity by settingsRepository.overlayOpacity.collectAsState()

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BoosterOverlayContent(
                            metricsState = metricsState,
                            isExpanded = isExpanded,
                            selectedPanel = selectedPanel,
                            opacity = opacity,
                            onToggleExpand = { _isExpanded.value = !_isExpanded.value },
                            onPanelSelected = { _selectedPanel.value = it },
                            onDrag = { dx, dy -> handleDrag(dx, dy) },
                            onDragEnd = { persistPosition() },
                            onScreenshot = { onScreenshotCallback?.invoke() }
                        )
                    }
                }
            }
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        overlayLifecycleOwner = lifecycleOwner
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView?.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView?.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView?.setViewTreeViewModelStoreOwner(lifecycleOwner)
        composeView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val isLandscape = context.resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val (savedX, savedY) = settingsRepository.getOverlayPosition(isLandscape)

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
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
            x = if (savedX == -1) 50 else savedX
            y = if (savedY == -1) 50 else savedY
        }

        try {
            windowManager.addView(composeView, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideOverlay() {
        composeView?.let {
            windowManager.removeView(it)
            overlayLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            composeView = null
            overlayLifecycleOwner = null
            windowParams = null
        }
        _isVisible.value = false
    }

    fun setOverlayVisible(visible: Boolean) {
        _isVisible.value = visible
    }

    private fun handleDrag(dx: Float, dy: Float) {
        windowParams?.let { p ->
            p.x += dx.toInt()
            p.y += dy.toInt()
            val screenSize = getScreenSize()
            p.x = p.x.coerceIn(0, (screenSize.x - (composeView?.width ?: 0)).coerceAtLeast(0))
            p.y = p.y.coerceIn(0, (screenSize.y - (composeView?.height ?: 0)).coerceAtLeast(0))
            composeView?.let { windowManager.updateViewLayout(it, p) }
        }
    }

    private fun persistPosition() {
        windowParams?.let { p ->
            val isLandscape = context.resources.configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
            settingsRepository.setOverlayPosition(isLandscape, p.x, p.y)
        }
    }

    private fun getScreenSize(): android.graphics.Point {
        val size = android.graphics.Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            size.x = bounds.width()
            size.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            display.getSize(size)
        }
        return size
    }
}

@Composable
fun BoosterOverlayContent(
    metricsState: MetricsState,
    isExpanded: Boolean,
    selectedPanel: OverlayPanel,
    opacity: Float,
    onToggleExpand: () -> Unit,
    onPanelSelected: (OverlayPanel) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onScreenshot: () -> Unit = {}
) {
    val bgColor = Color.Black.copy(alpha = opacity)
    val accentColor = Color(0xFF6C5CE7)

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { onDragEnd() }) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleExpand() },
                    onLongPress = { onToggleExpand() }
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        if (isExpanded) {
            ExpandedOverlayPanel(
                metricsState = metricsState,
                selectedPanel = selectedPanel,
                accentColor = accentColor,
                onPanelSelected = onPanelSelected,
                onScreenshot = onScreenshot
            )
        } else {
            CompactOverlayBar(
                metricsState = metricsState,
                accentColor = accentColor
            )
        }
    }
}

@Composable
fun CompactOverlayBar(
    metricsState: MetricsState,
    accentColor: Color
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricChip("FPS", "${metricsState.fps}", 
            when { metricsState.fps >= 60 -> Color(0xFF22C55E); metricsState.fps >= 30 -> Color(0xFFFBBF24); else -> Color(0xFFEF4444) })
        MetricDivider()
        MetricChip("CPU", "${metricsState.cpuMhz}MHz", accentColor)
        MetricDivider()
        MetricChip("RAM", String.format("%.1fG", metricsState.ramUsedGb), accentColor)
        MetricDivider()
        MetricChip("NET", if (metricsState.networkRxKbps > 1024) 
            String.format("%.1fM", (metricsState.networkRxKbps + metricsState.networkTxKbps) / 1024f) 
        else 
            String.format("%.0fK", metricsState.networkRxKbps + metricsState.networkTxKbps), accentColor)
    }
}

@Composable
fun MetricChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricDivider() {
    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.DarkGray))
}

@Composable
fun ExpandedOverlayPanel(
    metricsState: MetricsState,
    selectedPanel: OverlayPanel,
    accentColor: Color,
    onPanelSelected: (OverlayPanel) -> Unit,
    onScreenshot: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .heightIn(min = 200.dp, max = 400.dp)
    ) {
        PanelNavigationBar(
            selectedPanel = selectedPanel,
            accentColor = accentColor,
            onPanelSelected = onPanelSelected
        )

        HorizontalDivider(color = accentColor.copy(alpha = 0.2f))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            when (selectedPanel) {
                OverlayPanel.PERFORMANCE -> PerformancePanel(metricsState, accentColor)
                OverlayPanel.TOURNAMENT -> TournamentPanel(accentColor)
                OverlayPanel.NOTIFICATIONS -> NotificationsPanel(accentColor)
                OverlayPanel.CHAT -> ChatPanel(accentColor)
                OverlayPanel.CLIPBOARD -> ClipboardPanel(accentColor)
                OverlayPanel.AUDIO -> AudioPanel(accentColor)
                OverlayPanel.SCREEN -> ScreenToolsPanel(accentColor, onScreenshot = onScreenshot)
                OverlayPanel.WALLET -> WalletPanel(accentColor)
            }
        }
    }
}

@Composable
fun PanelNavigationBar(
    selectedPanel: OverlayPanel,
    accentColor: Color,
    onPanelSelected: (OverlayPanel) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(OverlayPanel.entries) { panel ->
            val isSelected = panel == selectedPanel
            IconButton(
                onClick = { onPanelSelected(panel) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent
                    )
            ) {
                Icon(
                    imageVector = if (isSelected) panel.selectedIcon else panel.icon,
                    contentDescription = panel.label,
                    tint = if (isSelected) accentColor else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PerformancePanel(metricsState: MetricsState, accentColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Performance", Icons.Filled.Speed, accentColor)
        
        PerformanceMetricRow("FPS", "${metricsState.fps}", 
            when { metricsState.fps >= 60 -> Color(0xFF22C55E); metricsState.fps >= 30 -> Color(0xFFFBBF24); else -> Color(0xFFEF4444) })
        PerformanceMetricRow("Janky Frames", "${metricsState.jankyFrames}", Color(0xFFEF4444))
        PerformanceMetricRow("CPU", "${metricsState.cpuMhz} MHz", accentColor)
        PerformanceMetricRow("CPU Usage", "${metricsState.cpuPercentage ?: 0}%", accentColor)
        PerformanceMetricRow("RAM", String.format("%.1f / %.1f GB", metricsState.ramUsedGb, metricsState.ramTotalGb), accentColor)
        PerformanceMetricRow("Battery", String.format("%.1f°C", metricsState.batteryTempC), 
            if (metricsState.batteryTempC > 40) Color(0xFFEF4444) else accentColor)
        PerformanceMetricRow("Network", if (metricsState.networkRxKbps > 1024) 
            String.format("%.1f MB/s", (metricsState.networkRxKbps + metricsState.networkTxKbps) / 1024f)
        else 
            String.format("%.0f KB/s", metricsState.networkRxKbps + metricsState.networkTxKbps), accentColor)
        PerformanceMetricRow("Ping", "${metricsState.pingMs}ms",
            when { metricsState.pingMs < 50 -> Color(0xFF22C55E); metricsState.pingMs < 100 -> Color(0xFFFBBF24); else -> Color(0xFFEF4444) })
        
        if (metricsState.topProcessName != null) {
            PerformanceMetricRow("Top Process", "${metricsState.topProcessName} (${String.format("%.1f", metricsState.topProcessCpuPercent)}%)", Color(0xFFFBBF24))
        }
    }
}

@Composable
fun PerformanceMetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PanelHeader(title: String, icon: ImageVector, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TournamentPanel(accentColor: Color) {
    var hasActiveMatch by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Tournament Match", Icons.Filled.EmojiEvents, accentColor)
        
        if (hasActiveMatch) {
            TournamentMatchCard(accentColor)
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, 
                        tint = Color.Gray, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No active match", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TournamentMatchCard(accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Championship Finals", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("LIVE", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Room Code", color = Color.Gray, fontSize = 10.sp)
                    Text("GA-2847", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Timer", color = Color.Gray, fontSize = 10.sp)
                    Text("14:32", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Password", color = Color.Gray, fontSize = 10.sp)
                    Text("••••••", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallButton("Copy Code", accentColor, Modifier.weight(1f))
                SmallButton("Copy Pass", Color.Gray, Modifier.weight(1f))
                SmallButton("Report", Color(0xFFEF4444), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SmallButton(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable { }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun NotificationsPanel(accentColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Notifications", Icons.Filled.Notifications, accentColor)
        
        NotificationItem("Match Ready", "Your match against PlayerX starts in 5 min", "2m ago", accentColor)
        NotificationItem("Tournament Update", "Round 3 brackets are now live", "5m ago", accentColor)
        NotificationItem("Prize Received", "KES 500 deposited to your wallet", "1h ago", Color(0xFF22C55E))
        NotificationItem("Friend Request", "GamerPro42 wants to be friends", "2h ago", Color.Gray)
    }
}

@Composable
fun NotificationItem(title: String, message: String, time: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(message, color = Color.Gray, fontSize = 10.sp)
            }
            Text(time, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun ChatPanel(accentColor: Color) {
    var message by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        PanelHeader("Chat", Icons.Filled.Chat, accentColor)
        
        LazyColumn(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                ChatBubble("Organizer", "Match starts in 5 minutes. Good luck!", false, accentColor)
            }
            item {
                ChatBubble("You", "Ready!", true, accentColor)
            }
            item {
                ChatBubble("Opponent", "gl hf", false, Color.Gray)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type...", fontSize = 12.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { message = "" }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(sender: String, text: String, isSelf: Boolean, accentColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {
        Text(sender, color = Color.Gray, fontSize = 9.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelf) accentColor.copy(alpha = 0.3f) else Color(0xFF1A1A2E))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .widthIn(max = 220.dp)
        ) {
            Text(text, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun ClipboardPanel(accentColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Clipboard", Icons.Filled.ContentCopy, accentColor)
        
        ClipboardItem("Room Code", "GA-2847", accentColor)
        ClipboardItem("Password", "match2024", accentColor)
        ClipboardItem("Server", "asia-east1.gamearena.com", Color.Gray)
    }
}

@Composable
fun ClipboardItem(label: String, value: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.Gray, fontSize = 10.sp)
                Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = { }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", 
                    tint = accentColor, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun AudioPanel(accentColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Audio Controls", Icons.Filled.VolumeUp, accentColor)
        
        AudioControlRow("Music", Icons.Filled.MusicNote, "Playing", accentColor)
        AudioControlRow("Bluetooth", Icons.Filled.Bluetooth, "Connected", Color(0xFF22C55E))
        AudioControlRow("Microphone", Icons.Filled.Mic, "Unmuted", accentColor)
        
        Column {
            Text("Volume", color = Color.Gray, fontSize = 10.sp)
            Slider(
                value = 0.6f,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor
                )
            )
        }
    }
}

@Composable
fun AudioControlRow(label: String, icon: ImageVector, status: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(status, color = color, fontSize = 11.sp)
    }
}

@Composable
fun ScreenToolsPanel(accentColor: Color, onScreenshot: () -> Unit = {}) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Screen Tools", Icons.Filled.Screenshot, accentColor)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolButton("Screenshot", Icons.Filled.CameraAlt, accentColor, Modifier.weight(1f), onClick = onScreenshot)
            ToolButton("Record", Icons.Filled.Videocam, Color(0xFFEF4444), Modifier.weight(1f))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolButton("Orientation", Icons.Filled.ScreenRotation, accentColor, Modifier.weight(1f))
            ToolButton("Brightness", Icons.Filled.Brightness6, accentColor, Modifier.weight(1f))
        }
    }
}

@Composable
fun ToolButton(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = color, fontSize = 10.sp)
        }
    }
}

@Composable
fun WalletPanel(accentColor: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PanelHeader("Wallet", Icons.Filled.AccountBalanceWallet, accentColor)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Balance", color = Color.Gray, fontSize = 10.sp)
                Text("KES 2,450.00", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Pending Prizes", color = Color.Gray, fontSize = 9.sp); Text("KES 500", color = Color(0xFF22C55E), fontSize = 11.sp) }
                    Column { Text("Total Earned", color = Color.Gray, fontSize = 9.sp); Text("KES 12,400", color = accentColor, fontSize = 11.sp) }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton("Deposit", Color(0xFF22C55E), Modifier.weight(1f))
            SmallButton("Withdraw", accentColor, Modifier.weight(1f))
        }
    }
}

