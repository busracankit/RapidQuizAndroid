// Tüm modüller için ortak eklentiler (burada uygulanmaz, sadece sürümleri tanımlanır).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
