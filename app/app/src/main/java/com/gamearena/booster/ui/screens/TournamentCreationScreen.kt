package com.gamearena.booster.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamearena.booster.launcher.GameLauncher
import com.gamearena.booster.model.PlayerInfo
import com.gamearena.booster.panels.tournament.TournamentManager
import com.gamearena.booster.ui.theme.TournamentGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCreationScreen(
    onNavigateBack: () -> Unit,
    tournamentManager: TournamentManager
) {
    var name by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("") }
    var selectedGamePackage by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("Single Elimination") }
    var entryFee by remember { mutableStateOf("") }
    var prizePool by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableStateOf("8") }
    var totalRounds by remember { mutableStateOf("3") }
    var rules by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTimeHours by remember { mutableStateOf("1") }
    var registrationHours by remember { mutableStateOf("24") }
    var showGamePicker by remember { mutableStateOf(false) }
    var showFormatPicker by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val formats = listOf("Single Elimination", "Double Elimination", "Round Robin", "Swiss", "Best of 3", "Best of 5")
    val supportedGames = GameLauncher.SUPPORTED_GAMES.entries.toList()

    val canCreate = name.isNotBlank() && selectedGame.isNotBlank()

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
                        "Create Tournament",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "TOURNAMENT DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tournament Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TournamentGold,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedContainerColor = Color.White.copy(0.03f),
                            unfocusedContainerColor = Color.White.copy(0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                        onClick = { showGamePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Game", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    if (selectedGame.isNotBlank()) selectedGame else "Select a game",
                                    color = if (selectedGame.isNotBlank()) Color.White else Color.Gray.copy(0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TournamentGold,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedContainerColor = Color.White.copy(0.03f),
                            unfocusedContainerColor = Color.White.copy(0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "FORMAT & RULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                        onClick = { showFormatPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Format", color = Color.Gray, fontSize = 12.sp)
                                Text(format, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("Rules") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TournamentGold,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedContainerColor = Color.White.copy(0.03f),
                            unfocusedContainerColor = Color.White.copy(0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        minLines = 2,
                        maxLines = 6
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "PRIZES & ENTRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = entryFee,
                            onValueChange = { entryFee = it },
                            label = { Text("Entry Fee (KES)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TournamentGold,
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                focusedContainerColor = Color.White.copy(0.03f),
                                unfocusedContainerColor = Color.White.copy(0.03f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = prizePool,
                            onValueChange = { prizePool = it },
                            label = { Text("Prize Pool (KES)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TournamentGold,
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                focusedContainerColor = Color.White.copy(0.03f),
                                unfocusedContainerColor = Color.White.copy(0.03f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = maxPlayers,
                            onValueChange = { maxPlayers = it },
                            label = { Text("Max Players") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TournamentGold,
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                focusedContainerColor = Color.White.copy(0.03f),
                                unfocusedContainerColor = Color.White.copy(0.03f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = totalRounds,
                            onValueChange = { totalRounds = it },
                            label = { Text("Total Rounds") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TournamentGold,
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                focusedContainerColor = Color.White.copy(0.03f),
                                unfocusedContainerColor = Color.White.copy(0.03f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "SCHEDULE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTimeHours,
                            onValueChange = { startTimeHours = it },
                            label = { Text("Starts in (hours)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TournamentGold,
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                focusedContainerColor = Color.White.copy(0.03f),
                                unfocusedContainerColor = Color.White.copy(0.03f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = registrationHours,
                            onValueChange = { registrationHours = it },
                            label = { Text("Reg. Deadline (hrs)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TournamentGold,
                                unfocusedBorderColor = Color.White.copy(0.1f),
                                focusedContainerColor = Color.White.copy(0.03f),
                                unfocusedContainerColor = Color.White.copy(0.03f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val startIso = java.time.Instant.ofEpochMilli(now + (startTimeHours.toLongOrNull() ?: 1) * 3600000).toString()
                        val deadlineIso = java.time.Instant.ofEpochMilli(now + (registrationHours.toLongOrNull() ?: 24) * 3600000).toString()
                        tournamentManager.createTournament(
                            name = name,
                            game = selectedGame,
                            format = format,
                            entryFee = entryFee.toDoubleOrNull() ?: 0.0,
                            maxPlayers = maxPlayers.toIntOrNull() ?: 8,
                            rules = rules,
                            startTime = startIso,
                            registrationDeadline = deadlineIso,
                            description = description
                        )
                        showSuccessDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp),
                    shape = CircleShape,
                    enabled = canCreate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TournamentGold,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray.copy(0.2f),
                        disabledContentColor = Color.Gray
                    )
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Tournament", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showGamePicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showGamePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(0.06f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select Game", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.weight(1f, false).heightIn(max = 400.dp)) {
                        items(supportedGames) { (pkg, gameName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedGame = gameName
                                        selectedGamePackage = pkg
                                        showGamePicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(gameName, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormatPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showFormatPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(0.06f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select Format", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    formats.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    format = f
                                    showFormatPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(f, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSuccessDialog = false }) {
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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(TournamentGold.copy(0.1f))
                            .border(2.dp, TournamentGold.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TournamentGold, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tournament Created!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your tournament is now live. Players can join and request matches.", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onNavigateBack()
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TournamentGold, contentColor = Color.Black)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
