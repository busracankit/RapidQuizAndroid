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
- [ ] 2. Ağ katmanı
- [ ] 3. Kategoriler ekranı
- [ ] 4. Oyun ekranı + zamanlama testleri
- [ ] 5. Arka plan / geri tuşu / senkron / hatalar
- [ ] 6. Sonuç ekranı + isimle kaydetme
- [ ] 7. Skor tablosu
- [ ] 8. Erişilebilirlik, hareketi azalt, küçük ekran
- [ ] 9. Release build (R8) + canlı test
