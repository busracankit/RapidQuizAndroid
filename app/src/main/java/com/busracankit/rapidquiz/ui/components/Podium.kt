package com.busracankit.rapidquiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.model.LeaderboardEntry
import com.busracankit.rapidquiz.ui.theme.Accent
import com.busracankit.rapidquiz.ui.theme.Bronze
import com.busracankit.rapidquiz.ui.theme.Gold
import com.busracankit.rapidquiz.ui.theme.Ink
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidShapes
import com.busracankit.rapidquiz.ui.theme.Silver
import com.busracankit.rapidquiz.ui.theme.SpaceGrotesk
import com.busracankit.rapidquiz.util.formatSeconds
import com.busracankit.rapidquiz.ui.theme.Surface as SurfaceColor

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> Gold
    2 -> Silver
    else -> Bronze
}

/** İlk 3: ortada 1. (altın), solda 2. (gümüş), sağda 3. (bronz). */
@Composable
fun Podium(entries: List<LeaderboardEntry>, highlightId: Int?, modifier: Modifier = Modifier) {
    val first = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third = entries.getOrNull(2)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumSlot(second, pedestal = 64, isMe = second != null && second.id == highlightId, modifier = Modifier.weight(1f))
        PodiumSlot(first, pedestal = 96, isMe = first != null && first.id == highlightId, modifier = Modifier.weight(1f))
        PodiumSlot(third, pedestal = 44, isMe = third != null && third.id == highlightId, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumSlot(entry: LeaderboardEntry?, pedestal: Int, isMe: Boolean, modifier: Modifier = Modifier) {
    if (entry == null) {
        Spacer(modifier)
        return
    }
    val medal = medalColor(entry.rank)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(medal.copy(alpha = 0.25f))
                .border(3.dp, if (isMe) Accent else medal, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.playerName.take(1).uppercase(),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.playerName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (isMe) YouTag()
        Text(
            "${entry.score} ${stringResource(R.string.points)}",
            style = MaterialTheme.typography.titleMedium,
            color = Primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${stringResource(R.string.n_correct, entry.correctCount)} · " +
                stringResource(R.string.seconds_fmt, formatSeconds(entry.totalTimeMs)),
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestal.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(medal),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.rank.toString(),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

/** 4–10 arası satır: sıra, isim, "N doğru · süre", puan. Kullanıcının satırı accent çerçeveli. */
@Composable
fun LeaderboardRow(entry: LeaderboardEntry, isMe: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RapidShapes.Button,
        color = SurfaceColor,
        border = if (isMe) BorderStroke(2.dp, Accent) else null,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${entry.rank}.",
                style = MaterialTheme.typography.titleLarge,
                color = InkMuted,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.playerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isMe) {
                        Spacer(Modifier.width(8.dp))
                        YouTag()
                    }
                }
                Text(
                    "${stringResource(R.string.n_correct, entry.correctCount)} · " +
                        stringResource(R.string.seconds_fmt, formatSeconds(entry.totalTimeMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.score.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Primary,
                )
                Text(stringResource(R.string.points), style = MaterialTheme.typography.labelSmall, color = InkMuted)
            }
        }
    }
}

/** "Sen" etiketi. */
@Composable
fun YouTag() {
    Text(
        stringResource(R.string.you),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(com.busracankit.rapidquiz.ui.theme.AccentText)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
