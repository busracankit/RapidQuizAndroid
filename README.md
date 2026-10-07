🇬🇧 English | [🇹🇷 Türkçe](README.tr.md)

# Rapid Quiz — Android

The native Android client for a fast-paced trivia game: **20 questions, 5 seconds each, no sign-up**, and a **Top 10 leaderboard per category**. It is built with Kotlin and Jetpack Compose.

> **Part of Rapid Quiz**
>
> | Repository | Role |
> | --- | --- |
> | [RapidQuizBackend](https://github.com/busracankit/RapidQuizBackend) | Django REST API: game logic, scoring, leaderboards |
> | [RapidQuizFrontend](https://github.com/busracankit/RapidQuizFrontend) | Web client (Vue 3 + TypeScript) |
> | [RapidQuizAndroid](https://github.com/busracankit/RapidQuizAndroid) | Android client (Kotlin + Jetpack Compose, this repo) |
>
> There is no live server. The app talks to the backend, which you run locally (see [Getting started](#getting-started)). A production domain was planned but never purchased.

## Features

- **Four screens, ported 1:1 from the web client:**
  - **Categories**: category cards with loading skeletons and an error state with retry.
  - **Game**: a 3-2-1 countdown, a circular 5-second timer, a 20-segment progress bar, A/B/C/D answers, correct/wrong feedback and points earned.
  - **Result**: an animated score, the correct count, the total time, and saving the score with a name.
  - **Leaderboard**: a category picker, a top-3 podium, ranks 4–10, the player's own row highlighted, and "Your rank: N" when the player is outside the top 10.
- Same API, rules, texts (Turkish UI) and colour palette as the web version.
- Survives interruptions: backgrounding the app, the back button (with a confirmation to quit), network errors and expired sessions.
- Accessibility: screen-reader headings and labels, haptic feedback, small-screen support, and decorative animations turned off when the system "remove animations" setting is on.

## Tech stack

| Area | Tools |
| --- | --- |
| Language & UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 |
| Architecture | `ViewModel` + `StateFlow`, Navigation Compose with type-safe `@Serializable` routes, a manual DI container (no Hilt) |
| Networking | Retrofit 3, OkHttp 5, kotlinx.serialization |
| Build | Gradle 9.6 (Kotlin DSL, version catalog), AGP 9.4, R8, `minSdk 26`, `targetSdk 37`, Java 17 |
| Testing | JUnit 4, kotlinx-coroutines-test (virtual time), OkHttp MockWebServer, Compose UI test |

The fonts are Space Grotesk and Plus Jakarta Sans, bundled under the SIL Open Font License (see [`docs/licenses/`](docs/licenses/)).

## Architecture and key design decisions

- **A thin client by design.** The server decides points, timing and whether an answer is correct. The app only displays the results, and the correct answer arrives only after the player answers.
- **Timing with a monotonic clock.** The server returns `starts_in_ms` and `remaining_ms` with every question. The app computes the start time once, the moment the response arrives (`SystemClock.elapsedRealtime() + starts_in_ms`). It never uses the device's wall clock or the server timestamp, so a wrong phone clock cannot affect the game.
- **No double waiting.** The feedback pause after an answer comes from the server's `starts_in_ms` for the next question, and the app adds no extra delay. An earlier web version waited twice, which cost the player about 0.8 seconds per question.
- **An explicit state machine.** The game ViewModel moves through clear phases (`Starting → Countdown → Playing → Answering → Feedback → … → Finished`, plus `AnswerFailed`, `Syncing` and `Error`). Answers are accepted only in `Playing`, which blocks double taps.
- **Resync instead of guessing.** When the app goes to the background the timer stops. When it returns, the app calls `GET /current/` and continues from the server's state.
- **Session token hygiene.** The `X-Session-Token` is kept in memory only. It is never written to disk, and request bodies and headers are never logged.
- **Correct JSON for timeouts.** When time runs out the app must send `"choice_id": null` explicitly, because the backend rejects a missing field. This is covered by a unit test.
- **Configurable server address.** The base URL comes from `BuildConfig.API_BASE_URL`, which is set by Gradle properties. Plain HTTP is allowed only in debug builds and only for `10.0.2.2` and `localhost` (`app/src/debug/res/xml/network_security_config.xml`). Release builds use HTTPS only.
- **Testable by construction.** The clock (`MonotonicClock`) and the repository (`QuizRepository`) are interfaces passed into the ViewModels, so the timing logic is tested with virtual time and a fake repository instead of real waits or a real server.

## API

The app uses the [RapidQuizBackend](https://github.com/busracankit/RapidQuizBackend) REST API (`/api/v1/`): `categories/`, `sessions/`, `sessions/{id}/answers/`, `sessions/{id}/current/`, `sessions/{id}/result/`, `sessions/{id}/score/` and `leaderboard/?category=`. The Retrofit interface is in `data/api/RapidQuizApi.kt`. The full contract, with request and response examples, is in [`docs/PROJE.md`](docs/PROJE.md) (Turkish).

## Getting started

**Prerequisites:** Android Studio (latest stable) and an emulator. To play against real data you also need the backend running locally.

**1. Start the backend.** Follow the [backend README](https://github.com/busracankit/RapidQuizBackend#getting-started). Two settings matter for the emulator:

```bash
# in RapidQuizBackend/.env: the emulator reaches your machine at 10.0.2.2
DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1,0.0.0.0,10.0.2.2

# listen on all interfaces, not only 127.0.0.1
uv run manage.py runserver 0.0.0.0:8000
```

To check the connection, open `http://10.0.2.2:8000/api/v1/health/` in the emulator's browser. It should return `{"status":"ok"}`.

**2. Run the app.**

```bash
git clone https://github.com/busracankit/RapidQuizAndroid.git
```

Open the project in Android Studio, let Gradle sync, and run the `app` configuration on the emulator. The debug build connects to `http://10.0.2.2:8000/` by default.

**Changing the server address** (no code changes needed):

| Build | Default | Override |
| --- | --- | --- |
| debug | `http://10.0.2.2:8000/` | `rapidquiz.debugApiBaseUrl=…` in `~/.gradle/gradle.properties` |
| release | `https://rapidap.co/` (planned domain, not live) | `./gradlew assembleRelease -Prapidquiz.releaseApiBaseUrl=https://…/` |

On a physical phone on the same Wi-Fi, set `rapidquiz.debugApiBaseUrl=http://<your-computer-ip>:8000/`. Then add that IP to `network_security_config.xml` and to the backend's `DJANGO_ALLOWED_HOSTS`.

## Running tests

```bash
./gradlew testDebugUnitTest            # unit tests (no server needed)
./gradlew connectedDebugAndroidTest    # UI test (needs a running emulator, no server needed)
```

| Test | What it checks |
| --- | --- |
| `JsonTest` | Decoding the API's sample responses, and `"choice_id": null` being written on timeout |
| `QuizRepositoryTest` | Paths, the `X-Session-Token` header, and mapping error bodies to error codes (MockWebServer) |
| `GameTimingTest` | 3-2-1 countdown, timeouts, a single feedback wait and double-tap protection (virtual time) |
| `GameInterruptionTest` | Resync after backgrounding, retry after a network error, and a lost session |
| `ResultViewModelTest`, `LeaderboardViewModelTest` | Name rules, saving the score, highlighting and rank |
| `FormatTest` | Time formatting, name normalisation and length rules, timer rounding |
| `FullGameFlowTest` (UI) | Home → 20 questions → name → leaderboard, with a fake repository |

## Project structure

```
app/src/main/java/com/busracankit/rapidquiz/
├── AppContainer.kt, RapidQuizApp.kt      # manual DI: OkHttp, Retrofit, repository, clock
├── data/
│   ├── api/                              # Retrofit interface, error mapping
│   ├── model/                            # @Serializable API models
│   ├── QuizRepository.kt                 # API calls → results or typed errors
│   └── GameSessionHolder.kt              # carries the session across Game → Result → Leaderboard
├── ui/
│   ├── home/ game/ result/ leaderboard/  # screen + ViewModel for each
│   ├── components/                       # category card, answer button, countdown ring, podium…
│   ├── navigation/Routes.kt              # type-safe routes + NavHost
│   └── theme/                            # colours, typography, theme
└── util/                                 # monotonic clock, formatting, reduce-motion
app/src/test/                             # unit tests
app/src/androidTest/                      # Compose UI test
docs/PROJE.md                             # detailed project document (Turkish)
```

## Status

- The release build uses R8, but it is temporarily signed with the debug key so it can be tested on devices. A real upload key is required before publishing to Google Play.
- The launcher icon is still the default template icon.

## Acknowledgements

Built while following [KURS ADI] by [HOCA ADI] on Udemy, then extended with my own design and implementation.
