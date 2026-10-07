package com.busracankit.rapidquiz.ui.theme

import androidx.compose.ui.graphics.Color

// Tasarım tokenları (docs/PROJE.md › 6. bölüm). Yalnızca açık tema.
val Bg = Color(0xFFFFF8F1)       // Ekran arka planı (krem)
val Surface = Color(0xFFFFFFFF)  // Kart, soru kutusu
val Ink = Color(0xFF1E1B4B)      // Ana metin (siyah yerine lacivert)
val Primary = Color(0xFF7C3AED)  // Ana buton, logo, ilerleme
val Accent = Color(0xFFFF4D8D)   // Vurgu, puan animasyonu, kullanıcının satırı
val Success = Color(0xFF22C55E)  // Doğru
val Danger = Color(0xFFEF4444)   // Yanlış, son 1 sn
val Warning = Color(0xFFFACC15)  // Sayaç 2 sn altı

// Metin için koyu tonlar: success/danger krem zeminde 4.5:1 kontrastı sağlamıyor,
// bu yüzden yazılarda bunlar, çerçeve/ikon/dolgu gibi yüzeylerde ana tokenlar kullanılır.
val SuccessText = Color(0xFF15803D)
val DangerText = Color(0xFFB91C1C)
val AccentText = Color(0xFFC81E5B)
val InkMuted = Color(0xFF4B4878)  // İkincil metin (krem ve beyaz üzerinde > 7:1)

// Podyum
val Gold = Color(0xFFF5B301)
val Silver = Color(0xFFA8B0BD)
val Bronze = Color(0xFFCD7F32)

/** Sunucudan gelen "#RRGGBB" kategori rengini çözer. Hatalı/boş değerde [Primary]'ye düşer. */
fun parseCategoryColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Primary
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Primary)
}
