# Rapid Quiz — Android

Tam proje dokümanı: **[docs/PROJE.md](docs/PROJE.md)**. Her işe başlamadan önce ilgili bölümü oku; API sözleşmesi, zamanlama kuralları, metinler ve renkler oradadır.

## Kısa kurallar

- Kotlin + Jetpack Compose, tek modül (`:app`), paket `com.busracankit.rapidquiz`.
- Kod adları İngilizce; kullanıcıya görünen metinler (`strings.xml`) ve kod yorumları Türkçe.
- Webdeki oyunun birebir karşılığı: **yeni özellik ekleme** (üyelik, ayarlar, koyu tema, ses vb. yok).
- Puan, süre ve doğru cevap kararı sunucudadır; istemci hesaplamaz.
- Zamanlama monoton saatle (`SystemClock.elapsedRealtime()`), geri bildirim süresi **bir kez** beklenir (PROJE.md › 5).
- API yolları `/` ile biter. `AnswerRequest.choiceId`'ye varsayılan değer verme (`"choice_id": null` açıkça gitmeli).
- `session_token` yalnızca bellekte tutulur, loglanmaz, kalıcı depoya yazılmaz.
- Base URL `BuildConfig.API_BASE_URL`'den okunur (debug: `http://10.0.2.2:8000/`, release: `https://rapidap.co/`).
- Kütüphane sürümleri `gradle/libs.versions.toml`'da; yeni bağımlılık eklemeden önce gerçekten gerekli mi diye bak.

## Git

- Yol haritasındaki (PROJE.md › 13) her adım ayrı commit.
- Commit yazarı: `Büşra Cankit <cankitbusra@gmail.com>`. **Push'u Büşra yapar.**
- Remote: `https://github.com/busracankit/RapidQuizAndroid.git`

## Durum

- [x] 1. Proje kurulumu
- [x] 2. Ağ katmanı
- [x] 3. Kategoriler ekranı
- [x] 4. Oyun ekranı + zamanlama testleri
- [x] 5. Arka plan / geri tuşu / senkron / hatalar
- [x] 6. Sonuç ekranı + isimle kaydetme
- [x] 7. Skor tablosu
- [x] 8. Erişilebilirlik, hareketi azalt, küçük ekran
- [x] 9. Release build (R8) + canlı test

## Çalıştırma ve test

- Birim testleri: `./gradlew testDebugUnitTest` (JSON, MockWebServer, zamanlama, sonuç/skor tablosu VM'leri)
- UI testi (emülatör açıkken): `./gradlew connectedDebugAndroidTest` (sahte repository, sunucu gerekmez)
- Debug sunucu adresini koda dokunmadan değiştirmek: `~/.gradle/gradle.properties` içinde
  `rapidquiz.debugApiBaseUrl=http://192.168.1.20:8000/` (gerçek cihaz) ya da DO adresi.
- Release'i DO adresiyle denemek: `./gradlew installRelease -Prapidquiz.releaseApiBaseUrl=https://starfish-app-yuzxi.ondigitalocean.app/`
- Release şimdilik **debug anahtarıyla** imzalanıyor (yalnızca deneme için). Play'e yüklemeden önce gerçek keystore.

## Bilinen eksikler / sonraya kalanlar

- Uygulama ikonu varsayılan şablon ikonu (mağaza görselleriyle birlikte yapılacak).
- `rapidap.co` alınıp DO'ya bağlanana kadar release varsayılan adresi çalışmaz.
