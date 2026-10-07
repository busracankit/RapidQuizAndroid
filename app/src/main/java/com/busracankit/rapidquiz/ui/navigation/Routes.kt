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
import com.busracankit.rapidquiz.AppContainer
import com.busracankit.rapidquiz.ui.home.HomeScreen
import com.busracankit.rapidquiz.ui.game.GameScreen
import com.busracankit.rapidquiz.ui.game.GameViewModel
import com.busracankit.rapidquiz.ui.home.HomeViewModel
import androidx.navigation.toRoute
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
                    // Skor tablosu 7. adımda eklenecek
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
                // Sonuç ekranı 6. adımda eklenecek; şimdilik ana ekrana dönülür.
                onFinished = { navController.popBackStack<HomeRoute>(inclusive = false) },
                onExit = { navController.popBackStack<HomeRoute>(inclusive = false) },
            )
        }
    }
}

/** Çift dokunmada iki kez gezinmeyi önler: yalnızca ekran öndeyken gezinilir. */
private fun NavBackStackEntry.isResumed(): Boolean = lifecycle.currentState == Lifecycle.State.RESUMED
