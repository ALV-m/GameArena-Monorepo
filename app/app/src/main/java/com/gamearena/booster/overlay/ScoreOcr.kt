package com.gamearena.booster.overlay

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

data class OcrScoreResult(
    val player1Score: Int?,
    val player2Score: Int?,
    val detectedText: String,
    val confidence: Double,
    val success: Boolean,
    val error: String? = null
)

class ScoreOcr(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyzeScreenshot(bitmap: Bitmap): OcrScoreResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = processImage(inputImage)
            result
        } catch (e: Exception) {
            OcrScoreResult(
                player1Score = null,
                player2Score = null,
                detectedText = "",
                confidence = 0.0,
                success = false,
                error = e.message
            )
        }
    }

    suspend fun analyzeScreenshotUri(uri: Uri): OcrScoreResult {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            processImage(inputImage)
        } catch (e: Exception) {
            OcrScoreResult(
                player1Score = null,
                player2Score = null,
                detectedText = "",
                confidence = 0.0,
                success = false,
                error = e.message
            )
        }
    }

    private suspend fun processImage(inputImage: InputImage): OcrScoreResult {
        val fullText = suspendCancellableCoroutine<String> { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    cont.resume("")
                }
        }

        if (fullText.isBlank()) {
            return OcrScoreResult(
                player1Score = null,
                player2Score = null,
                detectedText = "",
                confidence = 0.0,
                success = false,
                error = "No text detected in screenshot"
            )
        }

        val scores = extractScores(fullText)
        val confidence = calculateConfidence(fullText, scores)

        return OcrScoreResult(
            player1Score = scores.first,
            player2Score = scores.second,
            detectedText = fullText,
            confidence = confidence,
            success = scores.first != null && scores.second != null
        )
    }

    private fun extractScores(text: String): Pair<Int?, Int?> {
        val normalizedText = text.replace("\\s+".toRegex(), " ").trim()

        val scorePatterns = listOf(
            // "3 - 1" or "3 -1" or "3- 1" or "3-1"
            Regex("""(\d+)\s*[-–—]\s*(\d+)"""),
            // "3:1" or "3 : 1"
            Regex("""(\d+)\s*:\s*(\d+)"""),
            // "Player1 3" and "Player2 1" on separate lines
            Regex("""(?:player\s*1|p1|you|self)\s*[:\s]*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:player\s*2|p2|opponent|enemy)\s*[:\s]*(\d+)""", RegexOption.IGNORE_CASE),
            // "Score: 3" patterns
            Regex("""score\s*[:\s]*(\d+)""", RegexOption.IGNORE_CASE),
            // Standalone numbers near each other (last resort)
            Regex("""(\d{1,2})\s+(\d{1,2})"""),
        )

        // Try pattern 1: separator patterns
        for (pattern in scorePatterns.take(2)) {
            val match = pattern.find(normalizedText)
            if (match != null) {
                val s1 = match.groupValues[1].toIntOrNull()
                val s2 = match.groupValues[2].toIntOrNull()
                if (s1 != null && s2 != null && s1 in 0..30 && s2 in 0..30) {
                    return Pair(s1, s2)
                }
            }
        }

        // Try pattern 2: labeled scores
        var p1Score: Int? = null
        var p2Score: Int? = null
        for (pattern in scorePatterns.drop(2).take(2)) {
            val match = pattern.find(normalizedText)
            if (match != null) {
                val score = match.groupValues[1].toIntOrNull()
                if (score != null && score in 0..30) {
                    if (p1Score == null) p1Score = score else p2Score = score
                }
            }
        }
        if (p1Score != null && p2Score != null) return Pair(p1Score, p2Score)

        // Try pattern 3: standalone numbers
        val numbers = Regex("""\b(\d{1,2})\b""").findAll(normalizedText)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it in 0..30 }
            .toList()

        if (numbers.size >= 2) {
            return Pair(numbers[0], numbers[1])
        }

        return Pair(null, null)
    }

    private fun calculateConfidence(text: String, scores: Pair<Int?, Int?>): Double {
        if (scores.first == null || scores.second == null) return 0.0

        var confidence = 0.3

        if (text.contains("score", ignoreCase = true)) confidence += 0.2
        if (text.contains("player", ignoreCase = true)) confidence += 0.15
        if (text.contains("winner", ignoreCase = true)) confidence += 0.1
        if (text.contains("result", ignoreCase = true)) confidence += 0.1

        val gameKeywords = listOf("goal", "kill", "death", "assist", "win", "lose", "match", "round", "set", "game")
        val foundKeywords = gameKeywords.count { text.contains(it, ignoreCase = true) }
        confidence += (foundKeywords * 0.05).coerceAtMost(0.15)

        return confidence.coerceAtMost(1.0)
    }

    fun saveBitmapToFile(bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }
}
