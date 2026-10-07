# Rapid Quiz — Android

Üyeliksiz, hızlı bir bilgi yarışması. Bir kategori seç, **20 soru**, her soru için **5 saniye**. Hızlı doğru cevap daha çok puan getirir; oyun sonunda yalnızca ismini yazıp kategorinin **İlk 10** skor tablosuna girersin.

Bu repo, web sürümünün ([RapidQuizFrontend](https://github.com/busracankit/RapidQuizFrontend)) Kotlin + Jetpack Compose ile yazılmış Android karşılığıdır. Aynı API'yi ([RapidQuizBackend](https://github.com/busracankit/RapidQuizBackend), v1), aynı kuralları, metinleri ve renkleri kullanır. Puan, süre ve doğru cevap kararı tamamen sunucudadır; uygulama yalnızca gösterir.

## Ekranlar

| Ekran | İçerik |
| --- | --- |
| **Kategoriler** | Kategori kartları (renk, ikon, açıklama), yükleniyor iskeleti, hata + "Tekrar dene" |
| **Oyun** | 3-2-1 geri sayım, `index/20` + puan, 20 parçalı ilerleme çubuğu, dairesel sayaç, A/B/C/D şıklar, doğru/yanlış geri bildirimi ve `+puan` |
| **Sonuç** | Sayarak artan puan, doğru sayısı, toplam süre, isimle skor kaydı, "Tekrar oyna" / "Başka kategori" |
| **Skor Tablosu** | Kategori seçici, ilk 3 podyum, 4–10 liste, kullanıcının satırı vurgulu ("Sen"), İlk 10 dışındaysa "Senin sıran: N." |

## Teknoloji

- Kotlin 2.4, Jetpack Compose + Material 3, Navigation Compose (tip güvenli rotalar)
- `ViewModel` + `StateFlow`, elle bağımlılık kabı (`AppContainer`, Hilt yok)
- Retrofit 3 + OkHttp 5 + kotlinx.serialization
- `minSdk 26`, `targetSdk 37`, AGP 9.4, Gradle 9.6, JDK 17
- Fontlar: Space Grotesk, Plus Jakarta Sans (OFL, uygulamaya gömülü)

## Başlarken

### Gereksinimler

- Android Studio (en güncel kararlı sürüm)
- Yerelde denemek için backend: [RapidQuizBackend](https://github.com/busracankit/RapidQuizBackend) + PostgreSQL

### Backend'i yerelde çalıştırma

```bash
# PostgreSQL açık olmalı (ör. brew services start postgresql@17 ya da docker compose up -d)
cd RapidQuizBackend
uv run manage.py runserver 0.0.0.0:8000   # 0.0.0.0: emülatör/telefon erişebilsin
```

Backend `.env` dosyasında emülatör adresine izin verilmeli:

```
DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1,0.0.0.0,10.0.2.2
```

Kontrol: emülatörün tarayıcısında `http://10.0.2.2:8000/api/v1/health/` → `{"status":"ok"}`

### Uygulamayı çalıştırma

Projeyi Android Studio'da aç, Gradle Sync yap ve `app` yapılandırmasını emülatörde çalıştır. Debug build varsayılan olarak `http://10.0.2.2:8000/` adresine bağlanır.

## Sunucu adresi

API kök adresi koda gömülü değildir, derleme ayarından okunur (`BuildConfig.API_BASE_URL`):

| Build | Varsayılan | Değiştirmek için |
| --- | --- | --- |
| debug | `http://10.0.2.2:8000/` | `~/.gradle/gradle.properties` → `rapidquiz.debugApiBaseUrl=…` |
| release | `https://rapidap.co/` | `-Prapidquiz.releaseApiBaseUrl=…` |

Örnekler:

```properties
# Gerçek telefon (aynı Wi-Fi): Mac'in IP'si
# (IP'yi app/src/debug/res/xml/network_security_config.xml'e de ekle)
rapidquiz.debugApiBaseUrl=http://192.168.1.20:8000/

# DigitalOcean geçici adresi
rapidquiz.debugApiBaseUrl=https://starfish-app-yuzxi.ondigitalocean.app/
```

Düz `http` yalnızca debug'da ve yalnızca `10.0.2.2` / `localhost` için açıktır; release yalnızca HTTPS kullanır.

## Testler

```bash
./gradlew testDebugUnitTest            # birim testleri
./gradlew connectedDebugAndroidTest    # UI testi (emülatör açık olmalı)
```

| Test | Ne doğrular |
| --- | --- |
| `JsonTest` | API örnek yanıtlarının çözülmesi, süre dolunca `"choice_id": null` yazılması |
| `QuizRepositoryTest` | MockWebServer ile yollar, `X-Session-Token` başlığı, hata gövdesi → hata kodu |
| `GameTimingTest` | Sanal zamanla oyun zamanlaması: 3-2-1, süre dolumu, geri bildirimin tek kez beklenmesi, çift dokunma |
| `GameInterruptionTest` | Arka plan → senkron, ağ hatasında tekrar deneme, oturum kaybı |
| `ResultViewModelTest`, `LeaderboardViewModelTest` | İsim kuralı, skor kaydı, vurgu ve sıra |
| `FullGameFlowTest` (UI) | Ana ekran → 20 soru → isim → skor tablosu (sahte veriyle, sunucu gerekmez) |

## Proje yapısı

```
app/src/main/java/com/busracankit/rapidquiz/
├── AppContainer.kt, RapidQuizApp.kt   # bağımlılıklar (OkHttp, Retrofit, repository)
├── data/                              # API arayüzü, modeller, hata eşleme, repository
├── ui/
│   ├── home/  game/  result/  leaderboard/   # ekran + ViewModel
│   ├── components/                    # kartlar, şık butonu, sayaç, podyum…
│   ├── navigation/Routes.kt           # rotalar + NavHost
│   └── theme/                         # renkler, fontlar, tema
└── util/                              # monoton saat, biçimlendirme, UiText
docs/PROJE.md                          # ayrıntılı proje dokümanı (API sözleşmesi, zamanlama kuralları)
```

## Yayın (release)

- `./gradlew assembleRelease` ile R8 açık release APK'sı üretilir.
- Release şu an **geçici olarak debug anahtarıyla** imzalanıyor (yalnızca deneme için). Google Play'e yüklemeden önce gerçek imza anahtarı tanımlanmalı.
- Varsayılan canlı adres `rapidap.co` alınıp DigitalOcean'a bağlanana kadar çalışmaz.

## Yapılacaklar

- [ ] Uygulama ikonu ve mağaza görselleri
- [ ] `rapidap.co` ile canlı adreste release testi
- [ ] Play için gerçek imza anahtarı

## Lisanslar

Fontlar SIL Open Font License 1.1 ile dağıtılır: [`docs/licenses/`](docs/licenses/).
