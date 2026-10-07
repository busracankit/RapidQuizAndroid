package com.busracankit.rapidquiz.util

import java.util.Locale

private val TURKISH: Locale = Locale.forLanguageTag("tr")

/** 41230 → "41,2" (ondalık ayırıcı virgül). Ekranda "%s sn" ile birlikte kullanılır. */
fun formatSeconds(ms: Long): String = String.format(TURKISH, "%.1f", ms / 1000.0)

/**
 * İsim kuralı (docs/PROJE.md › 4.8). Karakter ve uygunsuz kelime kontrolünü sunucu yapar,
 * mesajı isim alanının altında gösterilir; istemci yalnızca sadeleştirir ve uzunluğa bakar.
 */
object PlayerName {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 20

    private val whitespace = Regex("\\s+")

    /** Baştaki/sondaki boşluklar silinir, art arda boşluklar teke iner. */
    fun normalize(raw: String): String = raw.trim().replace(whitespace, " ")

    fun isLengthValid(name: String): Boolean = name.length in MIN_LENGTH..MAX_LENGTH
}
