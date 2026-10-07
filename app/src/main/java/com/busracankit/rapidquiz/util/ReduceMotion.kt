package com.busracankit.rapidquiz.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Sistemde "animasyonları kaldır / hareketi azalt" açıksa true. Süslemeler (sallanma, pulse, uçan +puan,
 * sayarak artan puan, iskelet nabzı) kapatılır. Oyun sayacı ve 3-2-1 bundan etkilenmez.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    var reduce by remember { mutableStateOf(readReduceMotion(context)) }
    // Ayar uygulama arka plandayken değişebilir: her dönüşte yeniden oku.
    LifecycleResumeEffect(Unit) {
        reduce = readReduceMotion(context)
        onPauseOrDispose { }
    }
    return reduce
}

private fun readReduceMotion(context: android.content.Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
