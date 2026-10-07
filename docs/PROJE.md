# Rapid Quiz — Android Proje Dokümanı (Kotlin + Jetpack Compose)

> Sürüm 1.0 · 6 Ekim 2026 · Faz 4 (mobil). Konum: `docs/PROJE.md` (kökteki `CLAUDE.md` buraya yönlendirir).
> Kaynak: backend reposu `RapidQuizBackend` (API v1) ve web istemcisi `RapidQuizFrontend`. iOS uygulaması aynı dokümanın SwiftUI sürümünü izler.

## 1. Amaç ve kapsam

Rapid Quiz üyeliksiz, hızlı bir bilgi yarışmasıdır: kategori seçilir, **20 soru**, her soru için **5 saniye**. Hızlı doğru cevap daha çok puan getirir. Oyun sonunda yalnızca **isim** alınır ve kategori bazlı **İlk 10** skor tablosuna yazılır. Puan, süre ve doğru cevap kararı **sunucudadır**, uygulama yalnızca gösterir.

Bu uygulama webdeki oyunun Android'e aktarımıdır. Hedefler:

- 4 ekran: Kategoriler → Oyun → Sonuç → Skor Tablosu.
- Webdeki ile aynı API, aynı kurallar, aynı metinler, aynı renkler.
- Telefon, dikey yön. Tablet ve yatay yön zorunlu değil (bozulmasın yeter).
- Kod adları İngilizce, kullanıcıya görünen metinler ve kod yorumları Türkçe.

## 2. Ekranlar ve akış

Uygulama webdeki oyunun birebir mobil karşılığıdır. **Yeni özellik yok:** üyelik, profil, ayarlar, bildirim, çevrimdışı mod, reklam, paylaşım, ses yok. Dil yalnızca Türkçe, tema yalnızca açık.

```
Kategoriler ──(kategoriye dokun)──► Oyun: 3-2-1 → 20 soru ──► Sonuç + isim ──(Kaydet)──► Skor Tablosu
     │                                                        │
     └──────────────(üst çubuktaki "Skor Tablosu")────────────┴──(Skor tablosunu gör)──► Skor Tablosu
```

| # | Ekran | İçerik | API |
| --- | --- | --- | --- |
| 1 | **Kategoriler** (açılış) | Logo "Rapid Quiz", slogan "20 soru. Her biri 5 saniye. Ne kadar hızlısın?", başlık "Bir kategori seç", kategori kartları (renk, ikon, ad, açıklama). Üst çubukta "Skor Tablosu" butonu. Yüklenirken iskelet kartlar, hata olursa mesaj + "Tekrar dene" | `GET /categories/` |
| 2 | **Oyun** | Önce tam ekran 3-2-1 (kategori adı ve rengiyle). Sonra: üstte `index/20` ve puan, 20 parçalı ilerleme çubuğu (doğru yeşil, yanlış/süre doldu kırmızı), dairesel geri sayım halkası, soru metni, alt alta 4 şık butonu (A/B/C/D rozetli). Cevaptan sonra doğru/yanlış + `+puan` | `POST /sessions/`, `POST /answers/`, `GET /current/` |
| 3 | **Sonuç** | "Oyun bitti!", büyük puan (sayarak artan animasyon), doğru `16/20`, toplam süre `41,2 sn`. İsim alanı (placeholder "Adın (2–20 karakter)") + "Kaydet". Altında "Tekrar oyna" (aynı kategori, yeni oyun), "Başka kategori" (ana ekran), "Skor tablosunu gör" | `GET /result/`, `POST /score/` |
| 4 | **Skor Tablosu** | Başlık "Skor Tablosu". Yatay kaydırılan kategori seçici (renk noktası + ad). Seçili kategori için İlk 10: ilk 3 podyum (altın/gümüş/bronz), 4–10 liste (sıra, isim, puan, "N doğru", süre). Oyundan gelindiyse kullanıcının satırı vurgulu (`accent` çerçeve, "Sen" etiketi). İlk 10'a giremediyse altta "Senin sıran: N.". Boşsa "Henüz skor yok. İlk sen ol!" | `GET /categories/`, `GET /leaderboard/?category=` |

Notlar:

- Oyun **kategoriye dokunduğun anda** başlar (POST anında sunucu sayacı 3 sn sonrasına kurar). Arada "Başla" butonu yoktur. Kurallar 3-2-1 ekranında küçük yazıyla gösterilebilir: "20 soru, her soru için 5 saniye · Hızlı cevap = daha çok puan (soru başı 150'ye kadar) · Süre dolarsa soru boş sayılır · Geri dönüp cevap değiştiremezsin".
- Skor Tablosu'na ana ekrandan girilirse ilk kategori seçilidir. Sonuçtan girilirse oynanan kategori seçili ve kullanıcının satırı vurguludur.
- Sonuç ekranından geri gidilirse ana ekrana dönülür (oyun ekranına geri dönülmez).

## 3. Sunucu adresleri

Uygulama tek bir **API kök adresi** (base URL) ile çalışır. Bu adres koda gömülmez, derleme ayarından okunur (aşağıdaki "Yapılandırma" bölümü).

| Ortam | Base URL | Not |
| --- | --- | --- |
| Canlı (planlanmıştı) | `https://rapidap.co` | **Yayında değil:** alan adı alınmadı, DigitalOcean'daki deneme kurulumu kapatıldı. |
| Yerel (Mac'te `runserver`) | Emülatör: `http://10.0.2.2:8000`, gerçek cihaz: `http://<Mac-IP>:8000` | Backend: `uv run manage.py runserver 0.0.0.0:8000` |

Bütün uç noktalar `<base URL>/api/v1/` altındadır. Planlanan canlı adresle tam liste (yerelde `<base URL>` yerine yerel adres kullanılır):

| # | Metot | Tam URL | Ne için |
| --- | --- | --- | --- |
| 1 | GET | `https://rapidap.co/api/v1/health/` | Sunucu ayakta mı (`{"status":"ok"}`) |
| 2 | GET | `https://rapidap.co/api/v1/categories/` | Ana ekran ve skor tablosundaki kategori listesi |
| 3 | POST | `https://rapidap.co/api/v1/sessions/` | Kategoriye dokununca oyunu başlatır, ilk soruyu döner |
| 4 | POST | `https://rapidap.co/api/v1/sessions/{session_id}/answers/` | Cevap gönderir, sonucu ve sıradaki soruyu döner |
| 5 | GET | `https://rapidap.co/api/v1/sessions/{session_id}/current/` | Uygulama arka plandan dönünce oyunu senkronlar |
| 6 | GET | `https://rapidap.co/api/v1/sessions/{session_id}/result/` | Oyun sonu özeti (puan, doğru sayısı, süre) |
| 7 | POST | `https://rapidap.co/api/v1/sessions/{session_id}/score/` | İsimle skor tablosuna kaydeder |
| 8 | GET | `https://rapidap.co/api/v1/leaderboard/?category={slug}` | Kategorinin İlk 10 listesi |

Tam şema (OpenAPI): `https://rapidap.co/api/schema/`, tarayıcıda deneme: `https://rapidap.co/api/docs/`.

## 4. API sözleşmesi (v1)

### 4.1 Genel kurallar

- **Sondaki `/` zorunlu.** `.../sessions` değil `.../sessions/`. Django eğik çizgisiz POST'u yönlendiremez, istek hata verir.
- Gövdeler JSON. Her istekte `Accept: application/json`, gövdeli isteklerde `Content-Type: application/json`.
- JSON anahtarları **snake_case** (`session_id`, `starts_in_ms`).
- **Üyelik, cookie ve CSRF yok.** Oyun oturumu, başlatırken dönen `session_token` ile taşınır: 4–7 numaralı isteklerde `X-Session-Token: <session_token>` başlığı gönderilir. Token yalnızca bir kez döner, uygulama oyun boyunca bellekte tutar ve loglara yazmaz.
- `client_type` mutlaka platforma göre gönderilir (`"android"` / `"ios"`). İstatistik için kullanılır.
- Zamanlar UTC ve ISO 8601 (`"2026-10-06T12:00:03.123456Z"`). **İstemci bunları zamanlama için kullanmaz** (bkz. 5. bölüm).
- **Puan, süre ve doğru cevap kararı tamamen sunucudadır.** İstemci yalnızca gösterir; puan hesaplamaz.

### 4.2 Hata biçimi

Başarısız her yanıt aynı biçimdedir. `message` Türkçe ve kullanıcıya gösterilebilir:

```json
{ "error": { "code": "invalid_player_name", "message": "İsim 2–20 karakter olmalı.", "details": {} } }
```

| HTTP | `code` | Ne zaman | İstemci ne yapar |
| --- | --- | --- | --- |
| 400 | `validation_error` | Gövde eksik/hatalı (`details` alan hatalarını içerir) | Genel hata mesajı. Geliştirme hatasıdır. |
| 400 | `invalid_json` | Gövde JSON değil | Geliştirme hatası |
| 400 | `invalid_choice` | `choice_id` 1–4 dışında | Geliştirme hatası |
| 400 | `invalid_player_name` | İsim kuralına uymuyor ya da uygunsuz | `message`'ı isim alanının altında göster |
| 403 | `invalid_session_token` | Token eksik/yanlış | "Oturum bulunamadı" ekranı → ana sayfa |
| 404 | `category_not_found` | Slug yok/pasif | Kategorileri yeniden yükle |
| 404 | `session_not_found` | Oturum yok | "Oturum bulunamadı" ekranı → ana sayfa |
| 404 | `not_found` | Yanlış adres | Geliştirme hatası (sondaki `/`'ı kontrol et) |
| 409 | `not_enough_questions` | Kategoride 20'den az soru | `message`'ı göster |
| 409 | `question_mismatch` | Gönderilen soru güncel soru değil | `/current/` ile senkronla (5.4) |
| 409 | `session_finished` | Tüm sorular zaten cevaplandı | Sonuç ekranına git |
| 409 | `session_not_finished` | Bitmemiş oyunda sonuç/skor istendi | `/current/` ile senkronla |
| 409 | `score_already_saved` | Skor bu oturum için zaten kaydedildi | Kaydedilmiş say, skor tablosuna geç |
| 410 | `session_expired` | Oyun başlayalı 30 dk geçti | "Oturumun süresi doldu" → ana sayfa |
| 429 | `rate_limited` | Çok fazla istek (IP başına) | `message`'ı göster |
| 500 | `server_error` | Sunucu hatası | Genel hata + "Tekrar dene" |

Bağlantı yoksa veya zaman aşımı olursa: "Sunucuya ulaşılamadı. İnternet bağlantını kontrol et." + "Tekrar dene".

İstek limitleri (IP başına, dakikada): oyun başlatma 30, cevap 120, skor kaydı 10, okuma (kategori, skor tablosu, current, result) 300.

### 4.3 Kategoriler

`GET /api/v1/categories/` → `200`, sayfalama yok, `order`'a göre sıralı dizi:

```json
[
  { "slug": "yazilim", "name": "Yazılım", "description": "…", "icon": "code", "color": "#3B82F6", "order": 1 },
  { "slug": "yapay-zeka", "name": "Yapay Zeka", "description": "…", "icon": "brain", "color": "#A855F7", "order": 2 },
  { "slug": "bilgisayar-muhendisligi", "name": "Bilgisayar Mühendisliği", "description": "…", "icon": "cpu", "color": "#F97316", "order": 3 },
  { "slug": "ulkeler", "name": "Ülkeler", "description": "…", "icon": "globe", "color": "#10B981", "order": 4 },
  { "slug": "fizik", "name": "Fizik", "description": "…", "icon": "atom", "color": "#06B6D4", "order": 5 }
]
```

`icon` bir anahtar kelimedir, platform ikonuna eşlenir (bkz. tasarım bölümü). Bilinmeyen anahtar için yedek ikon kullanılır. `color` `#RRGGBB` biçimindedir. Kategoriler admin'den değişebilir, **listeyi uygulamaya gömme.**

### 4.4 Oyunu başlat

`POST /api/v1/sessions/`

```json
{ "category": "yazilim", "client_type": "android" }
```

→ `201`

```json
{
  "session_id": "3f6c1d9e-6a0b-4c55-9d3e-2b7f1c0a9e11",
  "session_token": "kX9…(uzun rastgele metin)",
  "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
  "total_questions": 20,
  "time_limit_ms": 5000,
  "question": {
    "index": 1,
    "id": 412,
    "text": "HTTP'de 404 durum kodu ne anlama gelir?",
    "difficulty": 1,
    "choices": [
      { "id": 1, "text": "Sunucu hatası" },
      { "id": 2, "text": "Bulunamadı" },
      { "id": 3, "text": "Yetkisiz" },
      { "id": 4, "text": "Yönlendirme" }
    ],
    "served_at": "2026-10-06T12:00:03.000000Z",
    "starts_in_ms": 3000,
    "remaining_ms": 5000
  }
}
```

**Soru nesnesi** (her yerde aynı):

| Alan | Tip | Anlam |
| --- | --- | --- |
| `index` | int | Oyundaki sırası, 1–20 |
| `id` | int | Soru kimliği. Cevapta `question_id` olarak geri gönderilir |
| `text` | string | Soru metni (≤ 120 karakter) |
| `difficulty` | int | 1 kolay, 2 orta, 3 zor (gösterim isteğe bağlı) |
| `choices` | dizi (4) | `id` **bu oyuna özel 1–4** (A=1, B=2, C=3, D=4 sırasıyla gösterilir), `text` ≤ 40 karakter |
| `served_at` | string | Sunucu tarafı sayaç başlangıcı. İstemci kullanmaz |
| `starts_in_ms` | int | Sayaç başlayana kadar bekleme: ilk soruda ≈3000 (3-2-1), sonrakilerde ≈800 (doğru/yanlış gösterimi) |
| `remaining_ms` | int | Sayaç başladıktan sonra kalan süre (normalde 5000) |

**Doğru şık soru ile asla gelmez**, yalnızca cevap yanıtında gelir.

### 4.5 Cevap gönder

`POST /api/v1/sessions/{session_id}/answers/` + `X-Session-Token`

```json
{ "question_id": 412, "choice_id": 2 }
```

Süre dolduysa **`choice_id` alanı `null` olarak açıkça gönderilir**. Alanı hiç göndermemek `400 validation_error` verir:

```json
{ "question_id": 412, "choice_id": null }
```

→ `200`

```json
{
  "is_correct": true,
  "timed_out": false,
  "correct_choice_id": 2,
  "selected_choice_id": 2,
  "points": 132,
  "score": 132,
  "correct_count": 1,
  "answered_count": 1,
  "finished": false,
  "next_question": { "index": 2, "id": 87, "text": "…", "difficulty": 1, "choices": [ … ], "served_at": "…", "starts_in_ms": 800, "remaining_ms": 5000 }
}
```

`finished: true` olduğunda `next_question` `null` gelir, sonuç ekranına geçilir.

### 4.6 Oyun durumu (senkron)

`GET /api/v1/sessions/{session_id}/current/` + `X-Session-Token` → `200`

```json
{
  "session_id": "3f6c…",
  "status": "in_progress",
  "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
  "total_questions": 20,
  "time_limit_ms": 5000,
  "score": 264,
  "correct_count": 2,
  "answered_count": 3,
  "finished": false,
  "question": { …soru nesnesi… },
  "answers": [ { "index": 1, "is_correct": true, "timed_out": false, "points": 132, "response_ms": 1180 } ]
}
```

`status`: `in_progress` / `completed` / `expired`. Sunucu, süresi geçmiş soruları bu çağrıda "süre doldu" olarak kapatır ve gerekirse sıradakini verir. `finished: true` ise `question` `null`'dır.

### 4.7 Sonuç

`GET /api/v1/sessions/{session_id}/result/` + `X-Session-Token` → `200` (oyun bitmediyse `409 session_not_finished`)

```json
{
  "session_id": "3f6c…",
  "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
  "score": 2140,
  "max_score": 3000,
  "correct_count": 16,
  "total_questions": 20,
  "total_time_ms": 41230,
  "finished_at": "2026-10-06T12:02:10.000000Z",
  "answers": [ { "index": 1, "is_correct": true, "timed_out": false, "points": 132, "response_ms": 1180 } ],
  "score_saved": false,
  "player_name": null,
  "rank": null
}
```

`total_time_ms` "41,2 sn" gibi gösterilir. `score_saved: true` ise isim alanı yerine "Skorun “İsim” adıyla kaydedildi." yazılır.

### 4.8 Skoru kaydet

`POST /api/v1/sessions/{session_id}/score/` + `X-Session-Token`

```json
{ "player_name": "Büşra" }
```

İsim kuralı (sunucu uygular, istemci de ön kontrol yapar): baştaki/sondaki boşluklar silinir, art arda boşluklar teke iner, **2–20 karakter**, yalnızca harf (Türkçe dahil), rakam, boşluk ve `- _ . '`, en az bir harf/rakam. Uygunsuz kelimeler reddedilir. Her oyun yalnızca **bir kez** kaydedilir.

→ `201`

```json
{
  "entry": { "id": 981, "rank": 4, "player_name": "Büşra", "score": 2140, "correct_count": 16, "total_time_ms": 41230, "created_at": "2026-10-06T12:02:30.000000Z" },
  "rank": 4,
  "in_top": true,
  "leaderboard": {
    "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
    "entries": [ { "id": 12, "rank": 1, "player_name": "Ada", "score": 2710, "correct_count": 19, "total_time_ms": 30120, "created_at": "…" } ]
  }
}
```

Yanıttaki `leaderboard` güncel İlk 10'dur, skor tablosuna geçerken tekrar istek atmaya gerek yoktur. `in_top: false` ise listenin altında "Senin sıran: 37." gösterilir. Kullanıcının satırı `entry.id` ile bulunup vurgulanır.

### 4.9 Skor tablosu

`GET /api/v1/leaderboard/?category=yazilim` → `200`

```json
{
  "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
  "entries": [
    { "id": 12, "rank": 1, "player_name": "Ada", "score": 2710, "correct_count": 19, "total_time_ms": 30120, "created_at": "2026-10-05T18:11:02.000000Z" }
  ]
}
```

En fazla 10 satır. Sıralama sunucudadır (puan ↓, süre ↑, kayıt zamanı ↑). Aynı isim birden çok kez yer alabilir. Boş liste → "Henüz skor yok. İlk sen ol!". **Tüm kategorileri birleştiren genel bir liste API'de yoktur.** Skor tablosu ekranı, webdeki gibi kategori seçici + seçili kategorinin İlk 10'udur.

### 4.10 Puan kuralı (yalnızca bilgi; istemci hesaplamaz)

Doğru cevap 100 puan + hız bonusu `floor(50 × (5000 − cevap_ms) / 5000)` → soru başına 100–150. Yanlış ya da süre dolması 0. En yüksek puan 20 × 150 = 3000. Sunucu 750 ms ağ toleransı tanır.

## 5. Oyun zamanlaması (en kritik bölüm)

Webde bulunan ve düzeltilen hata burada tekrar edilmemeli: geri bildirim süresi iki kez beklenirse oyuncu her soruda ~0,8 sn kaybeder.

### 5.1 Saat

- **Monoton saat** kullanılır (Android: `SystemClock.elapsedRealtime()`). Cihazın duvar saati ve `served_at` zamanlama için **kullanılmaz** (cihaz saati yanlış olabilir).
- `starts_in_ms` **yanıtın alındığı ana** göredir. Başlangıç anı yanıt geldiği anda bir kez hesaplanır: `startsAt = şimdi + starts_in_ms`.

### 5.2 Akış

1. **Başlat:** `POST /sessions/` → `startsAt = şimdi + question.starts_in_ms` (≈3000). Bu sürede tam ekran **3-2-1** gösterilir (kalan süreden hesaplanır, sabit 1 sn'lik adımlarla değil).
2. **Soru:** `startsAt` gelince soru ve şıklar görünür, geri sayım `remaining_ms − (şimdi − startsAt)` değerinden başlar. Halka renkleri: >2 sn yeşil, 2–1 sn sarı, son 1 sn kırmızı + hafif titreşim.
3. **Cevap:** Şıkka dokununca hemen şıklar kilitlenir, sayaç durur, seçilen şık "bekliyor" görünür, `POST /answers/` atılır. Aynı anda ikinci istek atılmaz (çift dokunma koruması).
4. **Süre doldu:** Sayaç 0'a inince `choice_id: null` ile aynı istek atılır.
5. **Geri bildirim:** Yanıt gelince `nextStartsAt = şimdi + next_question.starts_in_ms` (≈800) hesaplanır. Doğru/yanlış, `correct_choice_id` ve `+puan` gösterilir. Yanlışta hafif titreşim. **`nextStartsAt` gelince** doğrudan 2. adıma geçilir, ek bekleme eklenmez.
6. **Son soru:** `finished: true` → geri bildirim ~900 ms gösterilir → `GET /result/` → Sonuç ekranı.

### 5.3 Şık durumları

| Durum | Görünüm |
| --- | --- |
| `idle` | Normal, dokunulabilir |
| `pending` | Seçildi, yanıt bekleniyor (diğerleri soluk) |
| `correct` | Seçilen = doğru → yeşil + ✓ |
| `wrong` | Seçilen ama yanlış → kırmızı + ✕ (hafif sallanma) |
| `reveal` | Seçilmedi ama doğru → yeşil çerçeve + ✓ |
| `dimmed` | Diğerleri, soluk |

Doğru/yanlış yalnızca renkle değil ikonla da belirtilir.

### 5.4 Senkron ve kesintiler

- Uygulama oyun sırasında **arka plana gider ve geri gelirse:** sayaç durdurulur, `GET /current/` çağrılır, gelen duruma göre devam edilir (sunucu bu arada süresi dolan soruları kapatmış olabilir). `finished: true` ise sonuç ekranına geçilir.
- `409 question_mismatch` veya `409 session_not_finished` → `GET /current/` ile senkron.
- `403 / 404 session_not_found / 410` → "Oyun oturumu bulunamadı ya da süresi doldu. Yeni bir oyun başlat." + "Ana sayfaya dön".
- Cevap isteği ağ hatası verirse: bir kez "Tekrar dene" sunulur. Tekrar denemede yine aynı `question_id` gönderilir. Sunucu zaten işlediyse `409 question_mismatch` döner, o zaman `/current/` ile senkronlanır.
- Uygulama süreci tamamen kapanırsa oyun kaybolur (kabul edilen sadelik). Token kalıcı depoya yazılmaz.
- Oyun ekranında geri tuşu/kaydırma: "Oyundan çıkılsın mı?" onayı. Çıkılırsa oturum terk edilir (sunucu 30 dk sonra kendisi kapatır).

## 6. Tasarım

Açık zemin, canlı ve enerjik: krem arka plan üzerinde doygun renkli kartlar, hızlı mikro animasyonlar. Koyu tema yok.

| Token | Hex | Kullanım |
| --- | --- | --- |
| `bg` | `#FFF8F1` | Ekran arka planı |
| `surface` | `#FFFFFF` | Kart, soru kutusu |
| `ink` | `#1E1B4B` | Ana metin (siyah yerine lacivert) |
| `primary` | `#7C3AED` | Ana buton, logo, ilerleme |
| `accent` | `#FF4D8D` | Vurgu, puan animasyonu, kullanıcının satırı |
| `success` | `#22C55E` | Doğru |
| `danger` | `#EF4444` | Yanlış, son 1 sn |
| `warning` | `#FACC15` | Sayaç 2 sn altı |

- **Font:** başlıklar *Space Grotesk* Bold, metin *Plus Jakarta Sans* Regular/SemiBold (Google Fonts, OFL lisanslı, uygulamaya gömülür). Soru metni en az 20 pt/sp.
- **Köşeler:** kartlar 24, butonlar 16. Kategori kartında kategori renginin %25'i ile hafif gölge.
- **Şık butonu:** tam genişlik, en az 56 yükseklik, solda A/B/C/D rozeti, metin kalın.
- **Podyum:** 1. altın `#F5B301`, 2. gümüş `#A8B0BD`, 3. bronz `#CD7F32`.
- **Animasyon:** doğruda kısa pulse ve yukarı uçan `+132`. Yanlışta şık hafifçe sallanır. Sistemde "hareketi azalt" açıksa animasyonlar kapatılır.
- **Erişilebilirlik:** metin kontrastı en az 4.5:1. Şıkların ekran okuyucu etiketi "A şıkkı: …". Geri sayım "3 saniye kaldı" diye okunur. Dinamik yazı boyutu desteklenir.
- **İkon eşlemesi** (`category.icon` → platform ikonu): bkz. platform bölümü.

## 7. Arayüz metinleri (Türkçe, webdekiyle aynı)

| Anahtar | Metin |
| --- | --- |
| app_name | Rapid Quiz |
| slogan | 20 soru. Her biri 5 saniye. Ne kadar hızlısın? |
| leaderboard | Skor Tablosu |
| pick_category | Bir kategori seç |
| load_error_categories | Kategoriler yüklenemedi. |
| retry | Tekrar dene |
| get_ready | Hazır ol! |
| progress | %1$d/%2$d |
| score | Puan |
| seconds_left | %d saniye kaldı |
| correct | Doğru! |
| wrong | Yanlış! |
| time_up | Süre doldu! |
| correct_answer_was | Doğru cevap: %s |
| points_plus | +%d |
| result_title | Oyun bitti! |
| result_correct | Doğru |
| result_time | Toplam süre |
| seconds_fmt | %s sn (ondalık ayırıcı virgül: 41,2 sn) |
| name_prompt | Skor tablosuna adını yaz |
| name_placeholder | Adın (2–20 karakter) |
| name_too_short | İsim en az 2 karakter olmalı. |
| save | Kaydet |
| saving | Kaydediliyor… |
| already_saved | Skorun “%s” adıyla kaydedildi. |
| see_leaderboard | Skor tablosunu gör |
| play_again | Tekrar oyna |
| other_category | Başka kategori |
| top10 | İlk 10 |
| leaderboard_empty | Henüz skor yok. İlk sen ol! |
| you | Sen |
| your_rank | Senin sıran: %d. |
| n_correct | %d doğru |
| points | puan |
| load_error_leaderboard | Skor tablosu yüklenemedi. |
| error_generic | Bir şeyler ters gitti. Lütfen tekrar dene. |
| error_network | Sunucuya ulaşılamadı. İnternet bağlantını kontrol et. |
| error_session_lost | Oyun oturumu bulunamadı ya da süresi doldu. Yeni bir oyun başlat. |
| back_home | Ana sayfaya dön |
| quit_title | Oyundan çıkılsın mı? |
| quit_message | İlerlemen kaybolacak. |
| quit_confirm | Çık |
| quit_cancel | Devam et |

## 8. Teknoloji (Ekim 2026)

| Bileşen | Seçim | Not |
| --- | --- | --- |
| IDE | Android Studio (en güncel kararlı) | Yeni proje: **Empty Activity** (Compose) şablonu |
| Dil | Kotlin 2.4.x | Ekim 2026 itibarıyla 2.4.20 |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) | AGP 9.x |
| SDK | `minSdk 26`, `compileSdk 37`, `targetSdk 37` | Android 17 = API 37 |
| UI | Jetpack Compose + Material 3 | Compose BOM `2026.09.00` veya daha yenisi |
| Navigasyon | Navigation Compose (tip güvenli rotalar, `@Serializable`) | |
| Durum | `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle` | lifecycle-viewmodel-compose, lifecycle-runtime-compose |
| Ağ | Retrofit + OkHttp + kotlinx.serialization dönüştürücüsü | Coroutine `suspend` fonksiyonlar |
| JSON | kotlinx.serialization | `org.jetbrains.kotlin.plugin.serialization` |
| İkonlar | `material-icons-extended` | R8 kullanılmayanları atar |
| DI | Yok. Basit `AppContainer` (elle) | Hilt gereksiz |
| Test | JUnit, kotlinx-coroutines-test, OkHttp MockWebServer, Compose UI test | |

Kütüphane sürümlerini Android Studio'nun önerdiği en güncel kararlı sürümlerle version catalog'a yaz. Yeni bağımlılık eklemeden önce gerçekten gerekli mi diye bak.

## 9. Yapılandırma

### 9.1 `app/build.gradle.kts` (önemli kısımlar)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.busracankit.rapidquiz"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.busracankit.rapidquiz"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            // Emülatör → Mac'teki runserver. Gerçek cihazda Mac'in IP'si: "http://192.168.1.20:8000/"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://rapidap.co/\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

Canlı sunucuya debug'dan bağlanmak istersen debug'daki değeri geçici olarak `https://rapidap.co/` (veya DO adresi) yap. Retrofit base URL'si **`/` ile bitmeli.**

### 9.2 İzinler ve HTTP (cleartext)

`app/src/main/AndroidManifest.xml`: `<uses-permission android:name="android.permission.INTERNET" />`

Android varsayılan olarak `http://` trafiğini engeller. Yerel sunucu için **yalnızca debug'da** izin verilir:

`app/src/debug/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
        <!-- Gerçek cihazla test: Mac'in yerel IP'si -->
        <!-- <domain includeSubdomains="false">192.168.1.20</domain> -->
    </domain-config>
</network-security-config>
```

`app/src/debug/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:networkSecurityConfig="@xml/network_security_config" />
</manifest>
```

Release'te bu dosyalar yoktur, yalnızca HTTPS çalışır.

### 9.3 Yerel ortam özeti

| Cihaz | Base URL | Gerekenler |
| --- | --- | --- |
| Emülatör | `http://10.0.2.2:8000/` | Backend `.env` → `DJANGO_ALLOWED_HOSTS`'a `10.0.2.2` ekle |
| Gerçek telefon (aynı Wi-Fi) | `http://<Mac-IP>:8000/` | `runserver 0.0.0.0:8000`, IP'yi `ALLOWED_HOSTS`'a ve network_security_config'e ekle |
| Canlı | `https://rapidap.co/` | Alan adı alınmış ve DO'ya bağlanmış olmalı |

## 10. Mimari ve kod iskeleti

### 10.1 Paket yapısı

```
com.busracankit.rapidquiz
├── MainActivity.kt              # setContent { RapidQuizTheme { RapidQuizNavHost() } }
├── RapidQuizApp.kt              # Application: AppContainer'ı kurar
├── AppContainer.kt              # Json, OkHttp, Retrofit, QuizRepository
├── data/
│   ├── api/RapidQuizApi.kt      # Retrofit arayüzü
│   ├── api/ApiError.kt          # hata gövdesi → ApiException
│   ├── model/*.kt               # @Serializable veri sınıfları
│   └── QuizRepository.kt        # API çağrıları + hata eşleme (Result tipi)
├── ui/
│   ├── theme/                   # Color.kt, Type.kt (fontlar), Theme.kt
│   ├── components/              # CategoryCard, ChoiceButton, CountdownRing, ProgressSegments, Podium, ErrorPanel
│   ├── navigation/Routes.kt     # @Serializable rotalar + NavHost
│   ├── home/                    # HomeScreen + HomeViewModel
│   ├── game/                    # GameScreen + GameViewModel (zamanlama burada)
│   ├── result/                  # ResultScreen + ResultViewModel
│   └── leaderboard/             # LeaderboardScreen + LeaderboardViewModel
└── util/MonotonicClock.kt       # test edilebilir saat (SystemClock.elapsedRealtime)
```

### 10.2 Modeller

```kotlin
@Serializable
data class Category(
    val slug: String, val name: String, val description: String = "",
    val icon: String = "", val color: String, val order: Int = 0,
)

@Serializable
data class CategoryBrief(val slug: String, val name: String, val color: String)

@Serializable
data class Choice(val id: Int, val text: String)

@Serializable
data class Question(
    val index: Int, val id: Int, val text: String, val difficulty: Int,
    val choices: List<Choice>,
    @SerialName("served_at") val servedAt: String,
    @SerialName("starts_in_ms") val startsInMs: Long,
    @SerialName("remaining_ms") val remainingMs: Long,
)

@Serializable
data class StartSessionRequest(val category: String, @SerialName("client_type") val clientType: String = "android")

@Serializable
data class SessionStart(
    @SerialName("session_id") val sessionId: String,
    @SerialName("session_token") val sessionToken: String,
    val category: CategoryBrief,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("time_limit_ms") val timeLimitMs: Long,
    val question: Question,
)

/** DİKKAT: choiceId'ye varsayılan değer VERME. Varsayılanlı alanlar encodeDefaults=false ile hiç yazılmaz,
 *  süre dolunca "choice_id": null gönderilmesi gerekir (alan yoksa sunucu 400 döner). */
@Serializable
data class AnswerRequest(
    @SerialName("question_id") val questionId: Int,
    @SerialName("choice_id") val choiceId: Int?,
)

@Serializable
data class AnswerResult(
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("timed_out") val timedOut: Boolean,
    @SerialName("correct_choice_id") val correctChoiceId: Int,
    @SerialName("selected_choice_id") val selectedChoiceId: Int? = null,
    val points: Int, val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("answered_count") val answeredCount: Int,
    val finished: Boolean,
    @SerialName("next_question") val nextQuestion: Question? = null,
)

@Serializable
data class AnswerSummary(
    val index: Int, @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("timed_out") val timedOut: Boolean, val points: Int,
    @SerialName("response_ms") val responseMs: Long,
)

@Serializable
data class SessionState(
    @SerialName("session_id") val sessionId: String, val status: String,
    val category: CategoryBrief,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("time_limit_ms") val timeLimitMs: Long,
    val score: Int, @SerialName("correct_count") val correctCount: Int,
    @SerialName("answered_count") val answeredCount: Int, val finished: Boolean,
    val question: Question? = null, val answers: List<AnswerSummary> = emptyList(),
)

@Serializable
data class GameResult(
    @SerialName("session_id") val sessionId: String, val category: CategoryBrief,
    val score: Int, @SerialName("max_score") val maxScore: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("total_time_ms") val totalTimeMs: Long,
    @SerialName("finished_at") val finishedAt: String? = null,
    val answers: List<AnswerSummary> = emptyList(),
    @SerialName("score_saved") val scoreSaved: Boolean,
    @SerialName("player_name") val playerName: String? = null,
    val rank: Int? = null,
)

@Serializable data class ScoreRequest(@SerialName("player_name") val playerName: String)

@Serializable
data class LeaderboardEntry(
    val id: Int, val rank: Int, @SerialName("player_name") val playerName: String,
    val score: Int, @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_time_ms") val totalTimeMs: Long,
    @SerialName("created_at") val createdAt: String,
)

@Serializable data class Leaderboard(val category: CategoryBrief, val entries: List<LeaderboardEntry>)

@Serializable
data class ScoreSaved(
    val entry: LeaderboardEntry, val rank: Int,
    @SerialName("in_top") val inTop: Boolean, val leaderboard: Leaderboard,
)

@Serializable data class ApiErrorBody(val error: ApiErrorDetail)
@Serializable data class ApiErrorDetail(val code: String, val message: String)
```

`Json { ignoreUnknownKeys = true }` kullan (sunucu ileride alan eklerse uygulama bozulmasın).

### 10.3 Retrofit arayüzü

```kotlin
interface RapidQuizApi {
    @GET("api/v1/categories/") suspend fun categories(): List<Category>

    @POST("api/v1/sessions/") suspend fun startSession(@Body body: StartSessionRequest): SessionStart

    @POST("api/v1/sessions/{id}/answers/")
    suspend fun answer(@Path("id") id: String, @Header("X-Session-Token") token: String, @Body body: AnswerRequest): AnswerResult

    @GET("api/v1/sessions/{id}/current/")
    suspend fun current(@Path("id") id: String, @Header("X-Session-Token") token: String): SessionState

    @GET("api/v1/sessions/{id}/result/")
    suspend fun result(@Path("id") id: String, @Header("X-Session-Token") token: String): GameResult

    @POST("api/v1/sessions/{id}/score/")
    suspend fun saveScore(@Path("id") id: String, @Header("X-Session-Token") token: String, @Body body: ScoreRequest): ScoreSaved

    @GET("api/v1/leaderboard/") suspend fun leaderboard(@Query("category") slug: String): Leaderboard
}
```

Yollar `/` ile başlamaz (base URL'ye eklenir) ve `/` ile biter. OkHttp zaman aşımları: bağlantı 10 sn, okuma 10 sn. Release'te HTTP gövdesi loglanmaz (token sızmasın).

Hata eşleme (`QuizRepository`): `HttpException` → gövdeyi `ApiErrorBody` olarak çöz → `ApiException(status, code, message)`. `IOException` → `NetworkException`. Ekranlar `code`'a göre karar verir (4.2'deki tablo), kullanıcıya `message` gösterilir.

### 10.4 Oyun durumu (GameViewModel)

```kotlin
sealed interface GamePhase {
    data object Starting : GamePhase                       // POST /sessions/ bekleniyor
    data class Countdown(val startsAt: Long) : GamePhase   // 3-2-1 (ilk soru)
    data object Playing : GamePhase                        // soru görünür, sayaç akıyor
    data class Answering(val selected: Int?) : GamePhase   // istek gitti
    data class Feedback(val answer: AnswerResult) : GamePhase
    data object Finished : GamePhase                       // → Sonuç ekranına git
    data class Error(val message: String, val sessionLost: Boolean) : GamePhase
}

data class GameUiState(
    val phase: GamePhase = GamePhase.Starting,
    val category: CategoryBrief? = null,
    val question: Question? = null,
    val questionStartsAt: Long = 0,   // monoton saat (ms)
    val totalQuestions: Int = 20,
    val timeLimitMs: Long = 5000,
    val score: Int = 0,
    val answers: List<AnswerSummary> = emptyList(),
)
```

Kurallar (5. bölümün Kotlin karşılığı):

- `now()` = `SystemClock.elapsedRealtime()`. Test için `MonotonicClock` arayüzü ile enjekte edilir.
- Yanıt alınır alınmaz `questionStartsAt = now() + question.startsInMs`.
- Kalan süre: `max(0, remainingMs - max(0, now() - questionStartsAt))`.
- Sayaç: `viewModelScope` içinde bir `Job`. Bekle (`delay(questionStartsAt - now())`), sonra kalan süre bitince `submit(null)`. Cevap verilince ya da ekran durunca job iptal edilir.
- Ekrandaki halka için Compose'da `LaunchedEffect` + `withFrameMillis` döngüsü kalan süreyi okur (saniyelik `delay` ile değil, akıcı olsun).
- `submit` yalnızca `phase == Playing` iken çalışır (çift dokunma koruması).
- Geri bildirimden sonra: `delay(nextStartsAt - now())` → sıradaki soruya geç. **Ek bekleme yok.**
- `finished` → `delay(900)` → `Finished`.
- Oturum (`sessionId`, `token`) ViewModel'de tutulur. Sonuç ekranı aynı oturumu kullanır: rotaya `sessionId` ve `token` geçirilir ya da oturum tutan tek bir `GameSessionHolder` (AppContainer'da) paylaşılır. Kalıcı depoya yazılmaz.
- `LifecycleEventEffect(Lifecycle.Event.ON_STOP)` → sayacı durdur. `ON_START` → `resync()` (`GET /current/`).
- `BackHandler` → "Oyundan çıkılsın mı?" diyaloğu.
- Titreşim: `LocalHapticFeedback.current.performHapticFeedback(...)`, yanlış cevapta ve son 1 saniyeye girerken bir kez.

### 10.5 Navigasyon

```kotlin
@Serializable data object HomeRoute
@Serializable data class GameRoute(val categorySlug: String)
@Serializable data object ResultRoute
@Serializable data class LeaderboardRoute(val slug: String? = null, val highlightId: Int? = null, val myRank: Int? = null)
```

- Home → Game: `navigate(GameRoute(slug))`.
- Game → Result: `navigate(ResultRoute) { popUpTo<GameRoute> { inclusive = true } }` (geri gelince oyuna dönülmesin).
- Result → Leaderboard (kayıttan sonra): `LeaderboardRoute(slug, entry.id, rank)`.
- "Tekrar oyna": `navigate(GameRoute(slug)) { popUpTo<HomeRoute>() }`. "Başka kategori": `popBackStack<HomeRoute>(inclusive = false)`.

### 10.6 Tema ve ikonlar

- `Color.kt` 6. bölümdeki tokenlar. `Color(android.graphics.Color.parseColor(category.color))` ile kategori rengi (hatalı değerde `primary`'ye düş).
- Fontlar: `res/font/space_grotesk_bold.ttf`, `plus_jakarta_sans_regular.ttf`, `plus_jakarta_sans_semibold.ttf` → `Type.kt`'de `FontFamily`.
- `category.icon` eşlemesi: `code` → `Icons.Rounded.Code`, `brain` → `Icons.Rounded.Psychology`, `cpu` → `Icons.Rounded.Memory`, `globe` → `Icons.Rounded.Public`, `atom` → `Icons.Rounded.Science`, diğer → `Icons.Rounded.Quiz`.
- Metinler `res/values/strings.xml`'de (7. bölümdeki anahtarlar). Ondalık için `String.format(Locale.forLanguageTag("tr"), "%.1f", ms / 1000.0)`.
- Edge-to-edge: `enableEdgeToEdge()` + `Scaffold` iç boşlukları.

## 11. Test planı

| Seviye | Ne | Araç |
| --- | --- | --- |
| Birim | Zamanlama: `startsAt` hesaplama, kalan süre, süre dolunca `null` gönderimi, geri bildirimin tek kez beklenmesi (sahte saatle) | JUnit + coroutines-test (`runTest`, sanal zaman) |
| Birim | JSON: 4. bölümdeki örnek yanıtlar çözülüyor mu; `AnswerRequest(1, null)` → `{"question_id":1,"choice_id":null}` | kotlinx.serialization |
| Entegrasyon | Repository: 201/200 yanıtları, 4xx hata gövdesi → `ApiException.code` | MockWebServer |
| UI | Ana ekran → kategori → 20 soru → isim → skor tablosu (sahte repository ile) | Compose UI test |
| Elle | Emülatör + yerel backend ile bir tam oyun, uçak modu (ağ hatası), arka plana atıp 10 sn sonra dönme, isim hataları, release build ile canlı adres | |


## 12. Backend tarafı: mobil için gerekenler

API kodunda **değişiklik gerekmez.** Mobil için tasarlandı: cookie/CSRF yok (token başlıkta), `client_type` zaten `web/ios/android` kabul ediyor, cevap yanıtı sıradaki soruyu da içeriyor. CORS yalnızca tarayıcıları ilgilendirir, yerel uygulamalar etkilenmez.

Yapılması gereken **ayarlar**:

1. **Yerel geliştirme:** Android emülatörü Mac'e `10.0.2.2` adresiyle gelir, gerçek cihazlar Mac'in ağ adresiyle gelir. Django bu `Host` değerlerini tanımıyorsa `400 Bad Request` döner. Backend `.env` dosyasında:
   `DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1,0.0.0.0,10.0.2.2,<mac-adı>.local,<mac-ip>`
   ve sunucu `uv run manage.py runserver 0.0.0.0:8000` ile başlatılır (yalnızca `127.0.0.1`'i dinlerse cihazlar ulaşamaz).
2. **Canlı:** backend HTTPS ile yayında olmalı (DO kaynakları silindiyse `docs/deploy.md` › "Panelden kurulum" ile yeniden kurulur). `rapidap.co` alınıp DO'ya bağlandığında `api` bileşeninde:
   `DJANGO_ALLOWED_HOSTS=rapidap.co,www.rapidap.co` ve `CSRF_TRUSTED_ORIGINS=https://rapidap.co,https://www.rapidap.co` (CSRF yalnızca admin paneli için).
   iOS (ATS) ve Android (cleartext kapalı) canlıda yalnızca HTTPS'e izin verir. DO sertifikayı otomatik verir.
3. **İsteğe bağlı, şimdilik gerek yok:** tüm kategorileri birleştiren "genel skor tablosu" istenirse yeni bir uç nokta (ör. `GET /api/v1/leaderboard/overall/`) gerekir, çünkü bugün API'de ve webde yok. Ayrıca mobil operatörler çok kullanıcıyı aynı IP'den çıkarabilir (CGNAT). Kullanıcı sayısı artarsa IP başına limitler (`THROTTLE_*`) gevşetilebilir.

## 13. Yol haritası (sırayla, her adım ayrı commit)

1. Proje kurulumu: şablon, paket adı, version catalog, tema/renkler/fontlar, `strings.xml`, debug network config.
2. Ağ katmanı: modeller, Retrofit, hata eşleme, JSON testleri.
3. Kategoriler ekranı (yükleniyor/hata/liste).
4. Oyun ekranı: 3-2-1, soru, sayaç, cevap, geri bildirim, ilerleme çubuğu + zamanlama testleri.
5. Arka plan/geri tuşu/senkron ve hata durumları.
6. Sonuç ekranı + isimle kaydetme.
7. Skor tablosu ekranı (kategori seçici, podyum, vurgu, "Senin sıran").
8. Erişilebilirlik, hareketi azalt, küçük ekran kontrolü.
9. Release build (R8) ile canlı adreste test. Mağaza için ikon/ekran görüntüleri sonra (Google Play geliştirici hesabı tek seferlik ücretlidir).

Commit yazarı: `Büşra Cankit <cankitbusra@gmail.com>`. Push'u Büşra yapar.
