package com.gamearena.booster.network

import android.content.Context
import android.graphics.Bitmap
import com.gamearena.booster.overlay.ScoreOcr
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ScreenshotUploadResult(
    val success: Boolean,
    val screenshotUrl: String? = null,
    val ocrResult: OcrResult? = null,
    val localOcr: com.gamearena.booster.overlay.OcrScoreResult? = null,
    val error: String? = null
)

@Singleton
class ScreenshotRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GameArenaApi,
    private val scoreOcr: ScoreOcr
) {
    suspend fun captureAndUpload(bitmap: Bitmap, matchId: String): ScreenshotUploadResult {
        return try {
            val localOcrResult = scoreOcr.analyzeScreenshot(bitmap)

            val file = scoreOcr.saveBitmapToFile(bitmap, "match_${matchId}_${System.currentTimeMillis()}.jpg")
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("screenshot", file.name, requestFile)

            val response = api.uploadScreenshot(matchId, multipartBody)

            if (response.isSuccessful) {
                val body = response.body()
                ScreenshotUploadResult(
                    success = true,
                    screenshotUrl = body?.screenshot_url,
                    ocrResult = body?.ocr_result,
                    localOcr = localOcrResult
                )
            } else {
                ScreenshotUploadResult(
                    success = true,
                    localOcr = localOcrResult,
                    error = "Server upload failed: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            val localOcrResult = scoreOcr.analyzeScreenshot(bitmap)
            ScreenshotUploadResult(
                success = localOcrResult.success,
                localOcr = localOcrResult,
                error = if (localOcrResult.success) null else "Upload failed: ${e.message}"
            )
        }
    }

    suspend fun uploadScreenshot(file: File, matchId: String): ScreenshotUploadResult {
        return try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("screenshot", file.name, requestFile)

            val response = api.uploadScreenshot(matchId, multipartBody)

            if (response.isSuccessful) {
                val body = response.body()
                ScreenshotUploadResult(
                    success = true,
                    screenshotUrl = body?.screenshot_url,
                    ocrResult = body?.ocr_result
                )
            } else {
                ScreenshotUploadResult(
                    success = false,
                    error = "Upload failed: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            ScreenshotUploadResult(
                success = false,
                error = "Upload failed: ${e.message}"
            )
        }
    }

    suspend fun getMatchScreenshot(matchId: String): ScreenshotData? {
        return try {
            val response = api.getMatchScreenshots(matchId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}
