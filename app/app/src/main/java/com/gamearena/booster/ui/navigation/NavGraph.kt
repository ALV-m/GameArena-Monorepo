package com.gamearena.booster.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamearena.booster.auth.AuthManager
import com.gamearena.booster.panels.tournament.TournamentManager
import com.gamearena.booster.ui.screens.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val tournamentManager: TournamentManager,
    val authManager: AuthManager
) : ViewModel()

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Appearance : Screen("appearance")
    object OverlayCustomization : Screen("overlay_customization")
    object Permissions : Screen("permissions")
    object About : Screen("about")
    object Performance : Screen("performance")
    object ThermalDiagnostics : Screen("thermal_diagnostics")
    object GameBooster : Screen("game_booster")
    object WalletSettings : Screen("wallet_settings")
    object Tournaments : Screen("tournaments")
    object CreateTournament : Screen("create_tournament")
    object PendingRequests : Screen("pending_requests")
}

@Composable
fun GameArenaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    val navViewModel: NavViewModel = hiltViewModel()
    val tournamentManager = navViewModel.tournamentManager
    val authManager = navViewModel.authManager

    var pendingTournamentRedirect by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) { popUpTo(0) } },
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                authManager = authManager,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    if (pendingTournamentRedirect) {
                        pendingTournamentRedirect = false
                        navController.navigate(Screen.Tournaments.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Screen.Dashboard.route) { popUpTo(0) }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                authManager = authManager,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } }
            )
        }
        composable(Screen.Dashboard.route) {
            val isLoggedIn by authManager.isLoggedIn.collectAsState()
            DashboardScreen(
                onNavigateToAppearance = { navController.navigate(Screen.Appearance.route) },
                onNavigateToOverlayCustomization = { navController.navigate(Screen.OverlayCustomization.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToPerformance = { navController.navigate(Screen.Performance.route) },
                onNavigateToThermalDiagnostics = { navController.navigate(Screen.ThermalDiagnostics.route) },
                onNavigateToTournaments = {
                    if (isLoggedIn) {
                        navController.navigate(Screen.Tournaments.route)
                    } else {
                        pendingTournamentRedirect = true
                        navController.navigate(Screen.Login.route)
                    }
                }
            )
        }
        composable(Screen.Appearance.route) {
            AppearanceScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.OverlayCustomization.route) {
            OverlayCustomizationScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Performance.route) {
            PerformanceScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.ThermalDiagnostics.route) {
            ThermalDiagnosticsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Tournaments.route) {
            TournamentListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.CreateTournament.route) },
                onNavigateToRequests = { navController.navigate(Screen.PendingRequests.route) },
                tournamentManager = tournamentManager
            )
        }
        composable(Screen.CreateTournament.route) {
            TournamentCreationScreen(
                onNavigateBack = { navController.popBackStack() },
                tournamentManager = tournamentManager
            )
        }
        composable(Screen.PendingRequests.route) {
            PendingRequestsScreen(
                onNavigateBack = { navController.popBackStack() },
                tournamentManager = tournamentManager
            )
        }
    }
}
