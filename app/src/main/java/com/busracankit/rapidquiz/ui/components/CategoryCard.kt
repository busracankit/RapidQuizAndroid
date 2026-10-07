package com.busracankit.rapidquiz.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.ui.theme.Ink
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.RapidQuizTheme
import com.busracankit.rapidquiz.ui.theme.RapidShapes
import com.busracankit.rapidquiz.ui.theme.parseCategoryColor
import com.busracankit.rapidquiz.util.LocalReduceMotion
import com.busracankit.rapidquiz.ui.theme.Surface as SurfaceColor

/** `category.icon` anahtarı → platform ikonu. Bilinmeyen anahtar için yedek ikon. */
fun categoryIcon(key: String?): ImageVector = when (key) {
    "code" -> Icons.Rounded.Code
    "brain" -> Icons.Rounded.Psychology
    "cpu" -> Icons.Rounded.Memory
    "globe" -> Icons.Rounded.Public
    "atom" -> Icons.Rounded.Science
    else -> Icons.Rounded.Quiz
}

/** Kategori kartı: renkli ikon kutusu, ad, açıklama. Kategori renginin %25'i ile hafif gölge. */
@Composable
fun CategoryCard(category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = parseCategoryColor(category.color)
    Surface(
        onClick = onClick,
        shape = RapidShapes.Card,
        color = SurfaceColor,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RapidShapes.Card,
                ambientColor = color.copy(alpha = 0.25f),
                spotColor = color.copy(alpha = 0.25f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(color = color, icon = categoryIcon(category.icon))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleLarge)
                if (category.description.isNotBlank()) {
                    Text(
                        category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = color)
        }
    }
}

@Composable
fun CategoryBadge(color: Color, icon: ImageVector, modifier: Modifier = Modifier, size: Int = 56) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.55f).dp))
    }
}

/** Yüklenirken gösterilen iskelet kart. */
@Composable
fun CategoryCardSkeleton(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val alpha = if (reduceMotion) {
        0.08f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val animated by transition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        animated
    }
    Surface(
        shape = RapidShapes.Card,
        color = SurfaceColor,
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Ink.copy(alpha = alpha)),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth(0.5f).height(20.dp).clip(RoundedCornerShape(6.dp))
                        .background(Ink.copy(alpha = alpha)),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth(0.85f).height(14.dp).clip(RoundedCornerShape(6.dp))
                        .background(Ink.copy(alpha = alpha)),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CategoryCardPreview() {
    RapidQuizTheme {
        Column(Modifier.background(com.busracankit.rapidquiz.ui.theme.Bg).padding(16.dp)) {
            CategoryCard(
                Category("yazilim", "Yazılım", "Kod, algoritma ve web soruları", "code", "#3B82F6", 1),
                onClick = {},
            )
            Spacer(Modifier.height(16.dp))
            CategoryCardSkeleton()
        }
    }
}
