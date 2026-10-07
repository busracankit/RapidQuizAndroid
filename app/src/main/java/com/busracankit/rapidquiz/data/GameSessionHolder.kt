package com.busracankit.rapidquiz.data

import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.ScoreSaved

/** Oynanan (ya da yeni biten) oyun. */
data class ActiveGame(val session: GameSession, val category: CategoryBrief)

/**
 * Oyun → Sonuç → Skor Tablosu arasında oturumu taşır. Yalnızca bellekte; süreç kapanırsa oyun kaybolur
 * (docs/PROJE.md › 5.4, kabul edilen sadelik).
 */
class GameSessionHolder {
    @Volatile
    var current: ActiveGame? = null
        private set

    /** Son kaydedilen skor; skor tablosu tekrar istek atmadan bunu gösterebilir. */
    @Volatile
    var lastSaved: ScoreSaved? = null

    /** Oyun sırasında category_not_found alındıysa ana ekran kategorileri yeniden yükler. */
    @Volatile
    var categoriesStale: Boolean = false

    fun startGame(game: ActiveGame) {
        current = game
        lastSaved = null
    }

    fun clear() {
        current = null
        lastSaved = null
    }
}
