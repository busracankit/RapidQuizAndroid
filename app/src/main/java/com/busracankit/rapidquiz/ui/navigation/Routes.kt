package com.busracankit.rapidquiz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.busracankit.rapidquiz.AppContainer
import com.busracankit.rapidquiz.ui.game.GameScreen
import com.busracankit.rapidquiz.ui.game.GameViewModel
import com.busracankit.rapidquiz.ui.home.HomeScreen
import com.busracankit.rapidquiz.ui.home.HomeViewModel
import com.busracankit.rapidquiz.ui.leaderboard.LeaderboardScreen
import com.busracankit.rapidquiz.ui.leaderboard.LeaderboardViewModel
import com.busracankit.rapidquiz.ui.result.ResultScreen
import com.busracankit.rapidquiz.ui.result.ResultViewModel
import kotlinx.serialization.Serializable

// Tip güvenli rotalar (docs/PROJE.md › 10.5)
@Serializable data object HomeRoute

@Serializable data class GameRoute(val categorySlug: String)

@Serializable data object ResultRoute

@Serializable
data class LeaderboardRoute(val slug: String? = null, val highlightId: Int? = null, val myRank: Int? = null)

@Composable
fun RapidQuizNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> { entry ->
            val vm = viewModel { HomeViewModel(container.repository, container.sessionHolder) }
            HomeScreen(
                viewModel = vm,
                onCategoryClick = { category ->
                    if (entry.isResumed()) navController.navigate(GameRoute(category.slug))
                },
                onLeaderboardClick = {
                    if (entry.isResumed()) navController.navigate(LeaderboardRoute())
                },
            )
        }

        composable<GameRoute> { entry ->
            val route = entry.toRoute<GameRoute>()
            val vm = viewModel {
                GameViewModel(route.categorySlug, container.repository, container.clock, container.sessionHolder)
            }
            GameScreen(
                viewModel = vm,
                // Sonuçtan geri gelince oyuna dönülmesin.
                onFinished = {
                    navController.navigate(ResultRoute) { popUpTo<GameRoute> { inclusive = true } }
                },
                onExit = { navController.popBackStack<HomeRoute>(inclusive = false) },
            )
        }

        composable<ResultRoute> { entry ->
            val vm = viewModel { ResultViewModel(container.repository, container.sessionHolder) }
            ResultScreen(
                viewModel = vm,
                onPlayAgain = { slug ->
                    if (entry.isResumed()) navController.navigate(GameRoute(slug)) { popUpTo<HomeRoute>() }
                },
                onOtherCategory = { navController.popBackStack<HomeRoute>(inclusive = false) },
                onLeaderboard = { target ->
                    if (entry.isResumed()) {
                        navController.navigate(LeaderboardRoute(target.slug, target.highlightId, target.myRank))
                    }
                },
            )
        }

        composable<LeaderboardRoute> { entry ->
            val route = entry.toRoute<LeaderboardRoute>()
            val vm = viewModel {
                LeaderboardViewModel(
                    repository = container.repository,
                    sessionHolder = container.sessionHolder,
                    initialSlug = route.slug,
                    highlightId = route.highlightId,
                    myRank = route.myRank,
                )
            }
            LeaderboardScreen(
                viewModel = vm,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
    }
}

/** Çift dokunmada iki kez gezinmeyi önler: yalnızca ekran öndeyken gezinilir. */
private fun NavBackStackEntry.isResumed(): Boolean = lifecycle.currentState == Lifecycle.State.RESUMED
