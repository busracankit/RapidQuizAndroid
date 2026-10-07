package com.busracankit.rapidquiz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.busracankit.rapidquiz.R

// Fontlar uygulamaya gömülüdür (Google Fonts, OFL; lisanslar docs/licenses/).
// Dosyalar değişken (variable) font: kalınlık, FontWeight'ten otomatik olarak font eksenine aktarılır.

/** Başlıklar: Space Grotesk Bold */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Bold),
)

/** Metin: Plus Jakarta Sans Regular / SemiBold (Bold şık butonları için) */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

private val base = Typography()

// Renk stile GÖMÜLMEZ: metin rengi bulunduğu yüzeyden gelir (krem/beyaz üstünde Ink, mor buton üstünde beyaz).
private fun TextStyle.heading() = copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
private fun TextStyle.body(weight: FontWeight = FontWeight.Normal) =
    copy(fontFamily = PlusJakartaSans, fontWeight = weight)

val Typography = Typography(
    displayLarge = base.displayLarge.heading(),
    displayMedium = base.displayMedium.heading(),
    displaySmall = base.displaySmall.heading(),
    headlineLarge = base.headlineLarge.heading(),
    headlineMedium = base.headlineMedium.heading(),
    headlineSmall = base.headlineSmall.heading(),
    titleLarge = base.titleLarge.heading(),
    titleMedium = base.titleMedium.body(FontWeight.SemiBold),
    titleSmall = base.titleSmall.body(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.body(),
    bodyMedium = base.bodyMedium.body(),
    bodySmall = base.bodySmall.body(),
    labelLarge = base.labelLarge.body(FontWeight.SemiBold),
    labelMedium = base.labelMedium.body(FontWeight.SemiBold),
    labelSmall = base.labelSmall.body(FontWeight.SemiBold),
)

/** Soru metni: en az 20 sp (docs/PROJE.md › 6. bölüm). */
val QuestionTextStyle = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 30.sp,
)
