package com.gamearena.booster.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamearena.booster.model.MatchRequest
import com.gamearena.booster.model.MatchRequestStatus
import com.gamearena.booster.model.PlayerInfo
import com.gamearena.booster.panels.tournament.TournamentManager
import com.gamearena.booster.ui.theme.TournamentGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsScreen(
    onNavigateBack: () -> Unit,
    tournamentManager: TournamentManager
) {
    val incomingRequests by tournamentManager.getIncomingRequests().collectAsState()
    val outgoingRequests by tournamentManager.getOutgoingRequests().collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Incoming", "Outgoing")

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "Match Requests",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${incomingRequests.size}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    containerColor = Color.White.copy(0.03f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val count = if (index == 0) incomingRequests.size else outgoingRequests.size
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$count", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val currentRequests = if (selectedTab == 0) incomingRequests else outgoingRequests

            if (currentRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (selectedTab == 0) Icons.Default.Inbox else Icons.Default.Outbox,
                                contentDescription = null,
                                tint = Color.Gray.copy(0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (selectedTab == 0) "No incoming requests" else "No outgoing requests",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (selectedTab == 0) "When players request matches in your tournaments, they'll appear here" else "Send match requests from tournament listings",
                                color = Color.Gray.copy(0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(currentRequests) { request ->
                    MatchRequestCard(
                        request = request,
                        isIncoming = selectedTab == 0,
                        onAccept = { tournamentManager.acceptMatchRequest(request.id, "Request accepted!") },
                        onDecline = { tournamentManager.declineMatchRequest(request.id) },
                        onCancel = { tournamentManager.cancelMatchRequest(request.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun MatchRequestCard(
    request: MatchRequest,
    isIncoming: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    val borderColor = when (request.status) {
        MatchRequestStatus.PENDING -> MaterialTheme.colorScheme.primary.copy(0.2f)
        MatchRequestStatus.ACCEPTED -> Color(0xFF22C55E).copy(0.2f)
        MatchRequestStatus.DECLINED -> Color(0xFFEF4444).copy(0.2f)
        MatchRequestStatus.CANCELLED -> Color.Gray.copy(0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(request.tournamentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                StatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Game: ", color = Color.Gray, fontSize = 12.sp)
                Text(request.game, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("From: ", color = Color.Gray, fontSize = 12.sp)
                Text(request.requestedBy.username, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (request.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f))
                ) {
                    Text(
                        text = request.message,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            request.responseMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (request.status) {
                            MatchRequestStatus.ACCEPTED -> Color(0xFF22C55E).copy(0.05f)
                            MatchRequestStatus.DECLINED -> Color(0xFFEF4444).copy(0.05f)
                            else -> Color.White.copy(0.03f)
                        }
                    )
                ) {
                    Text(
                        text = msg,
                        color = when (request.status) {
                            MatchRequestStatus.ACCEPTED -> Color(0xFF22C55E)
                            MatchRequestStatus.DECLINED -> Color(0xFFEF4444)
                            else -> Color.Gray
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (request.status == MatchRequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isIncoming) {
                        Button(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(0.15f),
                                contentColor = Color(0xFFEF4444)
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Decline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22C55E).copy(0.15f),
                                contentColor = Color(0xFF22C55E)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(0.1f),
                                contentColor = Color.Gray
                            )
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel Request", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: MatchRequestStatus) {
    val (bgColor, textColor, label) = when (status) {
        MatchRequestStatus.PENDING -> Triple(MaterialTheme.colorScheme.primary.copy(0.15f), MaterialTheme.colorScheme.primary, "Pending")
        MatchRequestStatus.ACCEPTED -> Triple(Color(0xFF22C55E).copy(0.15f), Color(0xFF22C55E), "Accepted")
        MatchRequestStatus.DECLINED -> Triple(Color(0xFFEF4444).copy(0.15f), Color(0xFFEF4444), "Declined")
        MatchRequestStatus.CANCELLED -> Triple(Color.Gray.copy(0.15f), Color.Gray, "Cancelled")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
