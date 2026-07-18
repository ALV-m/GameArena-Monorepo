package com.gamearena.booster.panels.tournament

import com.gamearena.booster.model.MatchInfo
import com.gamearena.booster.model.MatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultSubmissionManager @Inject constructor() {

    data class ResultSubmission(
        val matchId: String,
        val tournamentId: String,
        val score: Pair<Int, Int>,
        val screenshotPath: String? = null,
        val timestamp: Long,
        val status: SubmissionStatus
    )

    enum class SubmissionStatus {
        PENDING, SUBMITTED, APPROVED, REJECTED, DISPUTED
    }

    private val _pendingSubmissions = MutableStateFlow<List<ResultSubmission>>(emptyList())
    val pendingSubmissions: StateFlow<List<ResultSubmission>> = _pendingSubmissions.asStateFlow()

    private val _lastSubmission = MutableStateFlow<ResultSubmission?>(null)
    val lastSubmission: StateFlow<ResultSubmission?> = _lastSubmission.asStateFlow()

    fun submitResult(
        matchId: String,
        tournamentId: String,
        playerScore: Int,
        opponentScore: Int,
        screenshotPath: String? = null
    ): ResultSubmission {
        val submission = ResultSubmission(
            matchId = matchId,
            tournamentId = tournamentId,
            score = Pair(playerScore, opponentScore),
            screenshotPath = screenshotPath,
            timestamp = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED
        )
        _pendingSubmissions.value = _pendingSubmissions.value + submission
        _lastSubmission.value = submission
        return submission
    }

    fun submitDispute(matchId: String, tournamentId: String, reason: String): ResultSubmission {
        val submission = ResultSubmission(
            matchId = matchId,
            tournamentId = tournamentId,
            score = Pair(0, 0),
            timestamp = System.currentTimeMillis(),
            status = SubmissionStatus.DISPUTED
        )
        _pendingSubmissions.value = _pendingSubmissions.value + submission
        _lastSubmission.value = submission
        return submission
    }

    fun updateSubmissionStatus(matchId: String, status: SubmissionStatus) {
        _pendingSubmissions.value = _pendingSubmissions.value.map {
            if (it.matchId == matchId) it.copy(status = status) else it
        }
        if (_lastSubmission.value?.matchId == matchId) {
            _lastSubmission.value = _lastSubmission.value?.copy(status = status)
        }
    }

    fun getStatusText(status: SubmissionStatus): String = when (status) {
        SubmissionStatus.PENDING -> "Pending Review"
        SubmissionStatus.SUBMITTED -> "Submitted"
        SubmissionStatus.APPROVED -> "Approved"
        SubmissionStatus.REJECTED -> "Rejected"
        SubmissionStatus.DISPUTED -> "Under Dispute"
    }
}
