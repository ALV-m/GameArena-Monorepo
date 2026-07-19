package com.gamearena.booster.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface GameArenaApi {

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiUser>

    @GET("users/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 50): Response<List<ApiLeaderboard>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<ApiUser>

    @PUT("users/profile")
    suspend fun updateProfile(@Body body: Map<String, String?>): Response<ApiUser>

    @GET("wallet")
    suspend fun getWallet(): Response<ApiWallet>

    @GET("wallet/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<ApiTransaction>>

    @POST("wallet/deduct")
    suspend fun deductWallet(@Body body: Map<String, Any>): Response<ApiTransaction>

    @POST("wallet/credit")
    suspend fun creditWallet(@Body body: Map<String, Any>): Response<ApiTransaction>

    @GET("tournaments")
    suspend fun getTournaments(
        @Query("status") status: String? = null,
        @Query("game") game: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<ApiTournament>>

    @GET("tournaments/{id}")
    suspend fun getTournament(@Path("id") id: String): Response<ApiTournament>

    @POST("tournaments")
    suspend fun createTournament(@Body body: CreateTournamentRequest): Response<ApiTournament>

    @POST("tournaments/{id}/join")
    suspend fun joinTournament(@Path("id") id: String): Response<Map<String, String>>

    @POST("tournaments/{id}/leave")
    suspend fun leaveTournament(@Path("id") id: String): Response<Map<String, String>>

    @POST("tournaments/{id}/start")
    suspend fun startTournament(@Path("id") id: String): Response<Map<String, Any>>

    @GET("challenges")
    suspend fun getChallenges(
        @Query("status") status: String? = null,
        @Query("game") game: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<ApiChallenge>>

    @POST("challenges")
    suspend fun createChallenge(@Body body: CreateChallengeRequest): Response<ApiChallenge>

    @POST("challenges/{id}/accept")
    suspend fun acceptChallenge(@Path("id") id: String): Response<Map<String, Any>>

    @POST("challenges/{id}/cancel")
    suspend fun cancelChallenge(@Path("id") id: String): Response<Map<String, String>>

    @GET("matches/{id}")
    suspend fun getMatch(@Path("id") id: String): Response<ApiMatch>

    @POST("matches/{id}/result")
    suspend fun submitMatchResult(
        @Path("id") id: String,
        @Body body: SubmitResultRequest
    ): Response<Map<String, String>>

    @POST("matches/{id}/dispute")
    suspend fun submitDispute(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @Multipart
    @POST("matches/{id}/screenshot")
    suspend fun uploadScreenshot(
        @Path("id") matchId: String,
        @Part screenshot: MultipartBody.Part
    ): Response<ScreenshotUploadResponse>

    @GET("matches/{id}/screenshots")
    suspend fun getMatchScreenshots(@Path("id") matchId: String): Response<ScreenshotData>

    @POST("payments/payhero/deposit")
    suspend fun mpesaDeposit(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @POST("payments/stripe/deposit")
    suspend fun stripeDeposit(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @POST("payments/paypal/deposit")
    suspend fun paypalDeposit(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @POST("payments/paypal/capture")
    suspend fun paypalCapture(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("matches/{id}/ocr-result")
    suspend fun submitOcrResult(
        @Path("id") matchId: String,
        @Body body: Map<String, Any>
    ): Response<Map<String, String>>
}
