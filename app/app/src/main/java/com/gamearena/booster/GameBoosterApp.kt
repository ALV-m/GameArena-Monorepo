package com.gamearena.booster

import android.app.Application
import com.gamearena.booster.gaming.GamingModeEngine
import com.gamearena.booster.launcher.GameLauncher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GameBoosterApp : Application() {

    @Inject
    lateinit var gamingModeEngine: GamingModeEngine

    @Inject
    lateinit var gameLauncher: GameLauncher

    override fun onCreate() {
        super.onCreate()
        gamingModeEngine.recoverPersistedState()
        gameLauncher.scanInstalledGames()
    }
}
