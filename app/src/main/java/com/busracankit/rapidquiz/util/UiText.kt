package com.busracankit.rapidquiz.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.NetworkException

/** ViewModel'den ekrana metin taşır: ya strings.xml kaynağı ya da sunucudan gelen hazır Türkçe mesaj. */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> value
}

/**
 * Hata → kullanıcı mesajı (docs/PROJE.md › 4.2):
 * ağ hatası → "Sunucuya ulaşılamadı…", oturum kaybı → "Oyun oturumu bulunamadı…",
 * geliştirme hataları → [fallback], diğerleri → sunucunun Türkçe `message`'ı.
 */
fun Throwable.toUiText(@StringRes fallback: Int = R.string.error_generic): UiText = when (this) {
    is NetworkException -> UiText.Res(R.string.error_network)
    is ApiException -> when {
        isSessionLost -> UiText.Res(R.string.error_session_lost)
        isDeveloperError || message.isBlank() -> UiText.Res(fallback)
        else -> UiText.Raw(message)
    }
    else -> UiText.Res(fallback)
}
