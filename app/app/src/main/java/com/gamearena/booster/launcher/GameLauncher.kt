package com.gamearena.booster.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.gamearena.booster.model.MatchInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager

    data class GameInfo(
        val packageName: String,
        val name: String,
        val icon: Drawable?,
        val isInstalled: Boolean
    )

    private val _installedGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val installedGames: StateFlow<List<GameInfo>> = _installedGames.asStateFlow()

    private val _currentGame = MutableStateFlow<GameInfo?>(null)
    val currentGame: StateFlow<GameInfo?> = _currentGame.asStateFlow()

    private val _matchStartTime = MutableStateFlow<Long?>(null)
    val matchStartTime: StateFlow<Long?> = _matchStartTime.asStateFlow()

    private val _isGameRunning = MutableStateFlow(false)
    val isGameRunning: StateFlow<Boolean> = _isGameRunning.asStateFlow()

    companion object {
        val SUPPORTED_GAMES = mapOf(
            "jp.konami.pesam" to "eFootball",
            "com.ea.game.pokemonfifa_row" to "EA FC Mobile",
            "com.tencent.ig" to "PUBG Mobile",
            "com.tencent.tmgp.cod" to "Call of Duty Mobile",
            "com.mobile.legends" to "Mobile Legends",
            "com.dts.freefiremax" to "Free Fire MAX",
            "com.dts.freefireth" to "Free Fire",
            "com.supercell.clashroyale" to "Clase Royale",
            "com.chess.com" to "Chess.com",
            "com.supercell.clashofclans" to "Clash of Clans",
            "com.garena.game.codm" to "Call of Duty Mobile",
            "com.mobilelegends Bang Bang" to "Mobile Legends"
        )
    }

    fun scanInstalledGames() {
        val games = SUPPORTED_GAMES.map { (pkg, name) ->
            val (isInstalled, icon) = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                true to packageManager.getApplicationIcon(appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                false to null
            }
            GameInfo(pkg, name, icon, isInstalled)
        }
        _installedGames.value = games
    }

    fun launchGame(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                _isGameRunning.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun launchGameForMatch(match: MatchInfo): Boolean {
        _currentGame.value = GameInfo(
            match.gamePackage,
            SUPPORTED_GAMES[match.gamePackage] ?: match.gamePackage,
            null,
            true
        )
        _matchStartTime.value = match.startTime
        return launchGame(match.gamePackage)
    }

    fun onGameLaunched(packageName: String) {
        _isGameRunning.value = true
        _matchStartTime.value = System.currentTimeMillis()
    }

    fun onGameClosed() {
        _isGameRunning.value = false
        _matchStartTime.value = null
        _currentGame.value = null
    }

    fun getGameIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun isGameInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledGamePackages(): List<String> {
        return _installedGames.value.filter { it.isInstalled }.map { it.packageName }
    }
}
