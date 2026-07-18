package com.gamearena.booster.panels.tournament

import com.gamearena.booster.model.*
import com.gamearena.booster.network.CreateTournamentRequest
import com.gamearena.booster.network.GameArenaApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TournamentManager @Inject constructor(
    private val api: GameArenaApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentTournament = MutableStateFlow<TournamentInfo?>(null)
    val currentTournament: StateFlow<TournamentInfo?> = _currentTournament.asStateFlow()

    private val _currentMatch = MutableStateFlow<MatchInfo?>(null)
    val currentMatch: StateFlow<MatchInfo?> = _currentMatch.asStateFlow()

    private val _matchTimeRemaining = MutableStateFlow(0L)
    val matchTimeRemaining: StateFlow<Long> = _matchTimeRemaining.asStateFlow()

    private val _isMatchActive = MutableStateFlow(false)
    val isMatchActive: StateFlow<Boolean> = _isMatchActive.asStateFlow()

    private val _allTournaments = MutableStateFlow<List<TournamentInfo>>(emptyList())
    val allTournaments: StateFlow<List<TournamentInfo>> = _allTournaments.asStateFlow()

    private val _activeFilter = MutableStateFlow(TournamentFilter.ALL)
    val activeFilter: StateFlow<TournamentFilter> = _activeFilter.asStateFlow()

    private val _matchRequests = MutableStateFlow<List<MatchRequest>>(emptyList())
    val matchRequests: StateFlow<List<MatchRequest>> = _matchRequests.asStateFlow()

    private val _myTournamentIds = MutableStateFlow<Set<String>>(emptySet())
    val myTournamentIds: StateFlow<Set<String>> = _myTournamentIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var countdownJob: kotlinx.coroutines.Job? = null

    val filteredTournaments: StateFlow<List<TournamentInfo>> = combine(
        _allTournaments, _activeFilter, _myTournamentIds
    ) { tournaments, filter, myIds ->
        when (filter) {
            TournamentFilter.ALL -> tournaments
            TournamentFilter.OPEN -> tournaments.filter { it.status == TournamentStatus.REGISTRATION }
            TournamentFilter.LIVE -> tournaments.filter { it.status == TournamentStatus.IN_PROGRESS }
            TournamentFilter.MY_TOURNAMENTS -> tournaments.filter { it.id in myIds }
        }
    }.let { flow ->
        val state = MutableStateFlow(emptyList<TournamentInfo>())
        scope.launch {
            flow.collect { state.value = it }
        }
        state.asStateFlow()
    }

    fun setFilter(filter: TournamentFilter) {
        _activeFilter.value = filter
    }

    fun refreshTournaments() {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.getTournaments(limit = 100)
                if (response.isSuccessful) {
                    _allTournaments.value = response.body()?.map { it.toDomain() } ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load tournaments: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTournament(
        name: String,
        game: String,
        format: String,
        entryFee: Double,
        maxPlayers: Int,
        rules: String,
        description: String = "",
        startTime: String? = null,
        registrationDeadline: String? = null
    ) {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.createTournament(
                    CreateTournamentRequest(
                        name = name,
                        game = game,
                        description = description.ifBlank { null },
                        format = format,
                        rules = rules.ifBlank { null },
                        entry_fee = entryFee,
                        max_players = maxPlayers,
                        start_time = startTime,
                        registration_deadline = registrationDeadline
                    )
                )
                if (response.isSuccessful) {
                    refreshTournaments()
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to create"
                    _error.value = try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinTournament(tournamentId: String) {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.joinTournament(tournamentId)
                if (response.isSuccessful) {
                    _myTournamentIds.value = _myTournamentIds.value + tournamentId
                    refreshTournaments()
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to join"
                    _error.value = try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leaveTournament(tournamentId: String) {
        scope.launch {
            try {
                val response = api.leaveTournament(tournamentId)
                if (response.isSuccessful) {
                    _myTournamentIds.value = _myTournamentIds.value - tournamentId
                    refreshTournaments()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun sendChallenge(game: String, stakeAmount: Double, opponentUsername: String? = null) {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.createChallenge(
                    com.gamearena.booster.network.CreateChallengeRequest(
                        game = game,
                        stake_amount = stakeAmount,
                        opponent_username = opponentUsername
                    )
                )
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string() ?: "Failed"
                    _error.value = try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptChallenge(challengeId: String) {
        scope.launch {
            try {
                val response = api.acceptChallenge(challengeId)
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string() ?: "Failed"
                    _error.value = try { org.json.JSONObject(err).optString("error", err) } catch (e: Exception) { err }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun cancelChallenge(challengeId: String) {
        scope.launch {
            try {
                api.cancelChallenge(challengeId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun sendMatchRequest(
        tournamentId: String,
        tournamentName: String,
        game: String,
        gamePackage: String,
        requestedBy: PlayerInfo,
        message: String = ""
    ): MatchRequest {
        val request = MatchRequest(
            id = "req_${System.currentTimeMillis()}",
            tournamentId = tournamentId,
            tournamentName = tournamentName,
            game = game,
            gamePackage = gamePackage,
            requestedBy = requestedBy,
            requestedAt = System.currentTimeMillis(),
            message = message
        )
        _matchRequests.value = _matchRequests.value + request
        sendChallenge(game = game, stakeAmount = 0.0, opponentUsername = null)
        return request
    }

    fun clearError() { _error.value = null }

    fun setCurrentTournament(tournament: TournamentInfo) {
        _currentTournament.value = tournament
    }

    fun setCurrentMatch(match: MatchInfo) {
        _currentMatch.value = match
        _isMatchActive.value = true
        startMatchCountdown(match.startTime)
    }

    fun clearCurrentMatch() {
        _currentMatch.value = null
        _isMatchActive.value = false
        countdownJob?.cancel()
        _matchTimeRemaining.value = 0L
    }

    private fun startMatchCountdown(startTime: Long) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (true) {
                val remaining = startTime - System.currentTimeMillis()
                if (remaining <= 0) { _matchTimeRemaining.value = 0L; break }
                _matchTimeRemaining.value = remaining
                delay(1000)
            }
        }
    }

    fun formatTimeRemaining(millis: Long): String {
        if (millis <= 0) return "00:00"
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    fun getMatchStatusText(status: MatchStatus): String = when (status) {
        MatchStatus.PENDING -> "Waiting"
        MatchStatus.READY -> "Ready to Start"
        MatchStatus.IN_PROGRESS -> "In Progress"
        MatchStatus.COMPLETED -> "Completed"
        MatchStatus.DISPUTED -> "Under Review"
        MatchStatus.CANCELLED -> "Cancelled"
    }

    fun getTournamentStatusText(status: TournamentStatus): String = when (status) {
        TournamentStatus.REGISTRATION -> "Registering"
        TournamentStatus.IN_PROGRESS -> "Live"
        TournamentStatus.COMPLETED -> "Finished"
        TournamentStatus.CANCELLED -> "Cancelled"
    }

    private fun com.gamearena.booster.network.ApiTournament.toDomain() = TournamentInfo(
        id = id,
        name = name,
        game = game,
        gamePackage = "",
        format = format,
        status = when (status) {
            "registration" -> TournamentStatus.REGISTRATION
            "in_progress" -> TournamentStatus.IN_PROGRESS
            "completed" -> TournamentStatus.COMPLETED
            "cancelled" -> TournamentStatus.CANCELLED
            else -> TournamentStatus.REGISTRATION
        },
        entryFee = entry_fee,
        prizePool = prize_pool,
        totalPlayers = registered_players ?: current_players ?: 0,
        maxPlayers = max_players,
        currentRound = 0,
        totalRounds = 0,
        organizerName = creator_name ?: "Unknown",
        organizerId = creator_id,
        rules = "",
        startTime = System.currentTimeMillis(),
        registrationDeadline = System.currentTimeMillis() + 86400000,
        description = description ?: "",
        createdAt = System.currentTimeMillis()
    )
}
