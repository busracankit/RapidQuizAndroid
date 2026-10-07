package com.busracankit.rapidquiz.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.ui.theme.Danger
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.Success

/**
 * 20 parçalı ilerleme çubuğu: doğru yeşil, yanlış/süre doldu kırmızı, mevcut soru mor.
 * Ekran okuyucu için ilerleme zaten "index/20" metniyle okunur, burası süs.
 */
@Composable
fun ProgressSegments(
    total: Int,
    currentIndex: Int,
    results: Map<Int, Boolean>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in 1..total) {
            val target = when (results[i]) {
                true -> Success
                false -> Danger
                null -> if (i == currentIndex) Primary else Primary.copy(alpha = 0.15f)
            }
            val color by animateColorAsState(target, label = "segment$i")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}
