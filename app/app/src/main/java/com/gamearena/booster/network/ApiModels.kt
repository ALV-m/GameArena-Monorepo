package com.gamearena.booster.network

data class AuthResponse(val user: ApiUser, val token: String)

data class ApiUser(
    val id: String,
    val username: String,
    val email: String?,
    val avatar_url: String?,
    val country: String?,
    val rank_tier: String?,
    val elo_rating: Int?,
    val total_wins: Int?,
    val total_losses: Int?,
    val total_matches: Int?,
    val is_admin: Boolean?,
    val balance: Double?,
    val currency: String?,
    val total_earned: Double?,
    val total_deposited: Double?,
    val created_at: String?
)

data class ApiWallet(
    val id: String,
    val user_id: String,
    val balance: Double,
    val currency: String,
    val total_deposited: Double,
    val total_withdrawn: Double,
    val total_earned: Double,
    val total_spent: Double
)

data class ApiTournament(
    val id: String,
    val creator_id: String,
    val creator_name: String?,
    val name: String,
    val game: String,
    val description: String?,
    val format: String,
    val entry_fee: Double,
    val prize_pool: Double,
    val max_players: Int,
    val current_players: Int?,
    val registered_players: Int?,
    val status: String,
    val start_time: String?,
    val registration_deadline: String?,
    val created_at: String?
)

data class ApiChallenge(
    val id: String,
    val challenger_id: String,
    val challenger_name: String?,
    val opponent_id: String?,
    val opponent_name: String?,
    val game: String,
    val stake_amount: Double,
    val currency: String,
    val status: String,
    val winner_id: String?,
    val winner_name: String?,
    val room_code: String?,
    val room_password: String?,
    val created_at: String?,
    val accepted_at: String?
)

data class ApiTransaction(
    val id: String,
    val type: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val description: String?,
    val payment_gateway: String?,
    val created_at: String?
)

data class ApiMatch(
    val id: String,
    val tournament_id: String?,
    val challenge_id: String?,
    val round_number: Int?,
    val player1_id: String,
    val player2_id: String?,
    val player1_name: String?,
    val player2_name: String?,
    val player1_score: Int?,
    val player2_score: Int?,
    val winner_id: String?,
    val winner_name: String?,
    val room_code: String?,
    val room_password: String?,
    val status: String,
    val stake_amount: Double?,
    val tournament_name: String?
)

data class CreateTournamentRequest(
    val name: String,
    val game: String,
    val description: String?,
    val format: String,
    val rules: String?,
    val entry_fee: Double?,
    val max_players: Int,
    val start_time: String?,
    val registration_deadline: String?
)

data class CreateChallengeRequest(
    val game: String,
    val stake_amount: Double,
    val opponent_username: String?
)

data class SubmitResultRequest(
    val player1_score: Int,
    val player2_score: Int,
    val winner_id: String
)

data class ApiLeaderboard(
    val id: String,
    val username: String,
    val avatar_url: String?,
    val country: String?,
    val rank_tier: String?,
    val elo_rating: Int?,
    val total_wins: Int?,
    val total_losses: Int?,
    val total_matches: Int?
)

data class ScreenshotUploadResponse(
    val message: String?,
    val screenshot_url: String?,
    val ocr_result: OcrResult?
)

data class OcrResult(
    val scores: Map<String, Any>?,
    val detected_text: String?,
    val confidence: Double?,
    val note: String?,
    val error: String?
)

data class ScreenshotData(
    val screenshot_url: String?,
    val metadata: ScreenshotMetadata?
)

data class ScreenshotMetadata(
    val originalName: String?,
    val size: Long?,
    val uploadedBy: String?,
    val uploadedAt: String?
)
