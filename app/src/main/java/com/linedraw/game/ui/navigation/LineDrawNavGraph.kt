package com.linedraw.game.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linedraw.game.LineDrawApp
import com.linedraw.game.ui.screens.GameplayScreen
import com.linedraw.game.ui.screens.LevelSelectScreen
import com.linedraw.game.ui.screens.MainMenuScreen
import com.linedraw.game.ui.screens.SettingsScreen

object Routes {
    const val MENU = "menu"
    const val LEVELS = "levels"
    const val SETTINGS = "settings"
    const val GAME = "game/{levelId}"

    fun game(levelId: Int) = "game/$levelId"
}

@Composable
fun LineDrawNavGraph(app: LineDrawApp) {
    val navController = rememberNavController()

    // Snappy sub-300ms fade/scale transitions between screens.
    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
        enterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.MENU) {
            MainMenuScreen(
                app = app,
                onPlay = { levelId -> navController.navigate(Routes.game(levelId)) },
                onLevelSelect = { navController.navigate(Routes.LEVELS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.LEVELS) {
            LevelSelectScreen(
                app = app,
                onLevelClick = { levelId -> navController.navigate(Routes.game(levelId)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                app = app,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
            GameplayScreen(
                app = app,
                initialLevelId = levelId,
                onExit = { navController.popBackStack() },
            )
        }
    }
}
