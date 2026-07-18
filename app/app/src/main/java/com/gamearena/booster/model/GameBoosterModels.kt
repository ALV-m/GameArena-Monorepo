package com.gamearena.booster.model

data class TournamentInfo(
    val id: String,
    val name: String,
    val game: String,
    val gamePackage: String,
    val format: String,
    val status: TournamentStatus,
    val entryFee: Double,
    val prizePool: Double,
    val totalPlayers: Int,
    val maxPlayers: Int,
    val currentRound: Int,
    val totalRounds: Int,
    val organizerName: String,
    val organizerId: String,
    val rules: String,
    val startTime: Long,
    val endTime: Long? = null,
    val registrationDeadline: Long,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class TournamentStatus {
    REGISTRATION, IN_PROGRESS, COMPLETED, CANCELLED
}

data class MatchInfo(
    val id: String,
    val tournamentId: String,
    val tournamentName: String,
    val round: Int,
    val matchNumber: Int,
    val player1: PlayerInfo,
    val player2: PlayerInfo?,
    val status: MatchStatus,
    val roomCode: String?,
    val roomPassword: String?,
    val startTime: Long,
    val endTime: Long? = null,
    val player1Score: Int? = null,
    val player2Score: Int? = null,
    val winnerId: String? = null,
    val gamePackage: String,
    val instructions: String? = null,
    val organizerMessage: String? = null
)

enum class MatchStatus {
    PENDING, READY, IN_PROGRESS, COMPLETED, DISPUTED, CANCELLED
}

data class MatchRequest(
    val id: String,
    val tournamentId: String,
    val tournamentName: String,
    val game: String,
    val gamePackage: String,
    val requestedBy: PlayerInfo,
    val requestedAt: Long,
    val message: String = "",
    val status: MatchRequestStatus = MatchRequestStatus.PENDING,
    val responseMessage: String? = null,
    val respondedAt: Long? = null
)

enum class MatchRequestStatus {
    PENDING, ACCEPTED, DECLINED, CANCELLED
}

enum class TournamentFilter {
    ALL, OPEN, LIVE, MY_TOURNAMENTS
}

data class PlayerInfo(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val country: String? = null,
    val rank: Int? = null,
    val clan: String? = null
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val content: String,
    val timestamp: Long,
    val channelType: ChatChannel,
    val isSystem: Boolean = false
)

enum class ChatChannel {
    ORGANIZER, OPPONENT, TEAM, GENERAL
}

data class ClipboardEntry(
    val id: String,
    val label: String,
    val value: String,
    val type: ClipboardType,
    val createdAt: Long,
    val matchId: String? = null
)

enum class ClipboardType {
    ROOM_CODE, PASSWORD, LINK, TEXT
}

data class BoosterNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    val read: Boolean = false,
    val actionUrl: String? = null,
    val matchId: String? = null,
    val tournamentId: String? = null
)

enum class NotificationType {
    MATCH_READY, MATCH_STARTED, MATCH_RESULT, TOURNAMENT_UPDATE,
    FRIEND_MESSAGE, FRIEND_REQUEST, PRIZE_RECEIVED, SYSTEM
}

data class WalletInfo(
    val balance: Double,
    val pendingDeposits: Double,
    val pendingWithdrawals: Double,
    val pendingPrizes: Double,
    val totalEarned: Double,
    val totalSpent: Double,
    val currency: String = "KES"
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String,
    val status: TransactionStatus,
    val description: String,
    val timestamp: Long,
    val reference: String? = null
)

enum class TransactionType {
    DEPOSIT, WITHDRAWAL, TOURNAMENT_ENTRY, PRIZE_PAYOUT, REFUND, BONUS, REFERRAL
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, CANCELLED
}

data class AudioState(
    val isMusicPlaying: Boolean = false,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val volume: Int = 50,
    val isBluetoothConnected: Boolean = false,
    val bluetoothDeviceName: String? = null,
    val isMicrophoneMuted: Boolean = false,
    val outputDevice: String = "Speaker"
)

data class ScreenToolsState(
    val brightness: Int = 50,
    val isAutoBrightness: Boolean = true,
    val isOrientationLocked: Boolean = false,
    val isRecording: Boolean = false,
    val recordingStartTime: Long? = null
)

data class PerformanceStats(
    val fps: Int = 0,
    val cpuUsage: Int = 0,
    val cpuFrequency: Int = 0,
    val ramUsed: Float = 0f,
    val ramTotal: Float = 0f,
    val batteryTemp: Float = 0f,
    val batteryLevel: Int = 0,
    val networkSpeed: String = "0 KB/s",
    val ping: Int = 0,
    val gpuUsage: Int = 0,
    val gpuFrequency: Int = 0
)
