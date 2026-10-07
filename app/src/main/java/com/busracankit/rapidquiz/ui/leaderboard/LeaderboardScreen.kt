package com.busracankit.rapidquiz.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.LeaderboardEntry
import com.busracankit.rapidquiz.ui.components.ErrorPanel
import com.busracankit.rapidquiz.ui.components.LeaderboardRow
import com.busracankit.rapidquiz.ui.components.Podium
import com.busracankit.rapidquiz.ui.theme.AccentText
import com.busracankit.rapidquiz.ui.theme.Bg
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidQuizTheme
import com.busracankit.rapidquiz.ui.theme.parseCategoryColor
import com.busracankit.rapidquiz.util.asString

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LeaderboardContent(state = state, onBack = onBack, onSelect = viewModel::select, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardContent(
    state: LeaderboardUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboard), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "picker") {
                CategoryPicker(
                    categories = state.categories,
                    selectedSlug = state.selectedSlug,
                    onSelect = onSelect,
                )
            }
            item(key = "title") {
                Text(
                    stringResource(R.string.top10),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
                )
            }

            val categoriesError = state.categoriesError
            val board = state.board
            when {
                // Kategoriler gelmeden ve seçili kategori yokken tabloyu da yükleyemeyiz.
                categoriesError != null && state.selectedSlug == null -> item(key = "catError") {
                    ErrorPanel(
                        title = stringResource(R.string.load_error_leaderboard),
                        message = categoriesError.asString(),
                        primaryLabel = stringResource(R.string.retry),
                        onPrimary = onRetry,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                board is BoardState.Loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                board is BoardState.Error -> item(key = "boardError") {
                    ErrorPanel(
                        title = stringResource(R.string.load_error_leaderboard),
                        message = board.message.asString(),
                        primaryLabel = stringResource(R.string.retry),
                        onPrimary = onRetry,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                board is BoardState.Content && board.entries.isEmpty() -> item(key = "empty") {
                    EmptyBoard()
                }

                board is BoardState.Content -> {
                    item(key = "podium") {
                        Podium(
                            entries = board.entries.take(3),
                            highlightId = state.highlightId,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(board.entries.drop(3), key = { it.id }) { entry ->
                        LeaderboardRow(
                            entry = entry,
                            isMe = entry.id == state.highlightId,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    val inList = board.entries.any { it.id == state.highlightId }
                    if (state.myRank != null && !inList) {
                        item(key = "myRank") {
                            Text(
                                stringResource(R.string.your_rank, state.myRank),
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentText,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(categories: List<Category>, selectedSlug: String?, onSelect: (String) -> Unit) {
    val listState = rememberLazyListState()
    // Sonuçtan gelindiyse seçili kategori görünür olsun.
    LaunchedEffect(categories, selectedSlug) {
        val index = categories.indexOfFirst { it.slug == selectedSlug }
        if (index > 0) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.slug }) { category ->
            val color = parseCategoryColor(category.color)
            FilterChip(
                selected = category.slug == selectedSlug,
                onClick = { onSelect(category.slug) },
                label = { Text(category.name, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun EmptyBoard() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                stringResource(R.string.leaderboard_empty),
                style = MaterialTheme.typography.titleMedium,
                color = InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LeaderboardPreview() {
    val entries = listOf(
        LeaderboardEntry(1, 1, "Ada", 2710, 19, 30120, ""),
        LeaderboardEntry(2, 2, "Büşra", 2540, 18, 33500, ""),
        LeaderboardEntry(3, 3, "Can", 2400, 17, 36000, ""),
        LeaderboardEntry(4, 4, "Deniz", 2140, 16, 41230, ""),
        LeaderboardEntry(5, 5, "Ece", 1980, 15, 44000, ""),
    )
    RapidQuizTheme(reduceMotion = true) {
        LeaderboardContent(
            state = LeaderboardUiState(
                categories = listOf(
                    Category("yazilim", "Yazılım", "", "code", "#3B82F6", 1),
                    Category("fizik", "Fizik", "", "atom", "#06B6D4", 5),
                ),
                categoriesLoading = false,
                selectedSlug = "yazilim",
                board = BoardState.Content(entries),
                highlightId = 4,
            ),
            onBack = {},
            onSelect = {},
            onRetry = {},
        )
    }
}
