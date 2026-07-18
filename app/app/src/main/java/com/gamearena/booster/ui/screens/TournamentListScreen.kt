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
import com.gamearena.booster.model.MatchRequestStatus
import com.gamearena.booster.model.PlayerInfo
import com.gamearena.booster.model.TournamentFilter
import com.gamearena.booster.model.TournamentInfo
import com.gamearena.booster.model.TournamentStatus
import com.gamearena.booster.panels.tournament.TournamentManager
import com.gamearena.booster.ui.theme.TournamentGold
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamearena.booster.ui.navigation.NavViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToRequests: () -> Unit,
    tournamentManager: TournamentManager
) {
    val navViewModel: NavViewModel = hiltViewModel()
    val currentUser by navViewModel.authManager.currentUser.collectAsState()

    val filteredTournaments by tournamentManager.filteredTournaments.collectAsState()
    val activeFilter by tournamentManager.activeFilter.collectAsState()
    val pendingRequests by tournamentManager.getPendingRequests().collectAsState()
    var showRequestDialog by remember { mutableStateOf<TournamentInfo?>(null) }
    var requestMessage by remember { mutableStateOf("") }
    var showJoinSuccess by remember { mutableStateOf<TournamentInfo?>(null) }

    LaunchedEffect(Unit) {
        tournamentManager.refreshTournaments()
    }

    val filters = listOf(
        TournamentFilter.ALL to "All",
        TournamentFilter.OPEN to "Open",
        TournamentFilter.LIVE to "Live",
        TournamentFilter.MY_TOURNAMENTS to "My Tournaments"
    )

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
                        "Tournaments",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (pendingRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clickable { onNavigateToRequests() }
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text("${pendingRequests.size}", fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Requests", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clickable { onNavigateToCreate() }
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TournamentGold.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create", tint = TournamentGold, modifier = Modifier.size(20.dp))
                    }
                }
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = filters.indexOfFirst { it.first == activeFilter },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    filters.forEachIndexed { index, (filter, label) ->
                        Tab(
                            selected = activeFilter == filter,
                            onClick = { tournamentManager.setFilter(filter) },
                            text = { Text(label, fontWeight = if (activeFilter == filter) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredTournaments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Gray.copy(0.4f), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No tournaments found", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Create one or change the filter", color = Color.Gray.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredTournaments) { tournament ->
                    TournamentCard(
                        tournament = tournament,
                        onJoin = { showJoinSuccess = tournament },
                        onRequestMatch = { showRequestDialog = tournament }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    showRequestDialog?.let { tournament ->
        requestMessage = ""
        androidx.compose.ui.window.Dialog(onDismissRequest = { showRequestDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Request Match", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("in ${tournament.name}", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = requestMessage,
                        onValueChange = { requestMessage = it },
                        label = { Text("Message (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedContainerColor = Color.White.copy(0.03f),
                            unfocusedContainerColor = Color.White.copy(0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRequestDialog = null },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(0.1f))
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                tournamentManager.sendMatchRequest(
                                    tournamentId = tournament.id,
                                    tournamentName = tournament.name,
                                    game = tournament.game,
                                    gamePackage = tournament.gamePackage,
                                    requestedBy = PlayerInfo(id = currentUser?.id ?: "", username = currentUser?.username ?: "You"),
                                    message = requestMessage
                                )
                                showRequestDialog = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Send Request", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    showJoinSuccess?.let { tournament ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { showJoinSuccess = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, TournamentGold.copy(0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(TournamentGold.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Joined!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You've joined ${tournament.name}", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            tournamentManager.joinTournament(tournament.id)
                            showJoinSuccess = null
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TournamentGold, contentColor = Color.Black)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(
    tournament: TournamentInfo,
    onJoin: () -> Unit,
    onRequestMatch: () -> Unit
) {
    val statusColor = when (tournament.status) {
        TournamentStatus.REGISTRATION -> Color(0xFF22C55E)
        TournamentStatus.IN_PROGRESS -> Color(0xFFFF4444)
        TournamentStatus.COMPLETED -> Color.Gray
        TournamentStatus.CANCELLED -> Color(0xFFEF4444)
    }
    val statusText = when (tournament.status) {
        TournamentStatus.REGISTRATION -> "OPEN"
        TournamentStatus.IN_PROGRESS -> "LIVE"
        TournamentStatus.COMPLETED -> "ENDED"
        TournamentStatus.CANCELLED -> "CANCELLED"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tournament.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tournament.game, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Group, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${tournament.totalPlayers}/${tournament.maxPlayers} players", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Prize: ${String.format("%.0f", tournament.prizePool)} KES", color = TournamentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Payments, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Entry: ${String.format("%.0f", tournament.entryFee)} KES", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tournament.format, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Round ${tournament.currentRound}/${tournament.totalRounds}", color = Color.Gray, fontSize = 12.sp)
            }

            if (tournament.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(tournament.description, color = Color.Gray.copy(0.7f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tournament.status == TournamentStatus.REGISTRATION) {
                    OutlinedButton(
                        onClick = onJoin,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, TournamentGold.copy(0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TournamentGold)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onRequestMatch,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.SportsKabaddi, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Request Match", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
