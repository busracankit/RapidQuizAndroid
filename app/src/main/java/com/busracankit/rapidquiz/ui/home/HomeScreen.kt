package com.busracankit.rapidquiz.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.ui.components.CategoryCard
import com.busracankit.rapidquiz.ui.components.CategoryCardSkeleton
import com.busracankit.rapidquiz.ui.components.ErrorPanel
import com.busracankit.rapidquiz.ui.theme.Bg
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidQuizTheme
import com.busracankit.rapidquiz.util.asString

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (Category) -> Unit,
    onLeaderboardClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    HomeContent(
        state = state,
        onCategoryClick = onCategoryClick,
        onLeaderboardClick = onLeaderboardClick,
        onRetry = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onCategoryClick: (Category) -> Unit,
    onLeaderboardClick: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Primary,
                    )
                },
                actions = {
                    TextButton(
                        onClick = onLeaderboardClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = Primary),
                    ) {
                        Icon(Icons.Rounded.Leaderboard, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.leaderboard), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                Column {
                    Text(
                        stringResource(R.string.slogan),
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkMuted,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.pick_category),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            when (state) {
                HomeUiState.Loading -> items(5, key = { "skeleton$it" }) { CategoryCardSkeleton() }
                is HomeUiState.Error -> item(key = "error") {
                    ErrorPanel(
                        title = stringResource(R.string.load_error_categories),
                        message = state.message.asString(),
                        primaryLabel = stringResource(R.string.retry),
                        onPrimary = onRetry,
                    )
                }
                is HomeUiState.Content -> items(state.categories, key = { it.slug }) { category ->
                    CategoryCard(category, onClick = { onCategoryClick(category) })
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun HomePreview() {
    RapidQuizTheme {
        HomeContent(
            state = HomeUiState.Content(
                listOf(
                    Category("yazilim", "Yazılım", "Kod ve algoritmalar", "code", "#3B82F6", 1),
                    Category("yapay-zeka", "Yapay Zeka", "Makine öğrenmesi", "brain", "#A855F7", 2),
                    Category("ulkeler", "Ülkeler", "Başkentler, bayraklar", "globe", "#10B981", 4),
                ),
            ),
            onCategoryClick = {},
            onLeaderboardClick = {},
            onRetry = {},
        )
    }
}
