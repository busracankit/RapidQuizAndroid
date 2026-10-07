package com.busracankit.rapidquiz.util

import android.os.SystemClock

/**
 * Oyun zamanlaması için monoton saat (ms). Cihazın duvar saati ve sunucunun served_at değeri
 * zamanlama için KULLANILMAZ (docs/PROJE.md › 5.1). Testlerde sahte saat verilir.
 */
fun interface MonotonicClock {
    fun now(): Long
}

object SystemMonotonicClock : MonotonicClock {
    override fun now(): Long = SystemClock.elapsedRealtime()
}
