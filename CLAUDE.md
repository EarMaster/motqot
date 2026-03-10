# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Signed release APK
./gradlew bundleRelease          # AAB for Play Store
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests
./gradlew lint                   # Lint checks
```

## Architecture

Single-module Android app (`rocks.wiedemann.motqot`) following MVVM with a repository layer:

- **`api/`** — Retrofit-based OpenAI-compatible client. `OpenAiCompatibleClient` wraps a configurable base URL and API key; `ProviderPresets` holds built-in configs for OpenAI, Anthropic, Mistral, etc.
- **`repository/QuoteRepository`** — Fetches quotes from the API and caches them in SharedPreferences.
- **`viewmodel/MainViewModel`** — Exposes UI state via `StateFlow`; coordinates the repository and WorkManager.
- **`worker/DailyQuoteWorker`** — WorkManager `CoroutineWorker` that generates and posts the daily quote notification.
- **`MainActivity` / `SettingsActivity` / `SettingsFragment`** — UI layer using View Binding. Settings are preference-based (XML + `androidx.preference`).
- **`MotQotApplication`** — Initialises WorkManager manually (auto-init disabled).

Key patterns: StateFlow for reactive state, `java.time.LocalDate` for dates, coroutines with `Dispatchers.IO` for all network/IO work, View Binding throughout.

## Release Process

CI (`release.yml`) handles releases automatically:
1. Bumps `versionCode` and `versionName` (patch) and commits `[skip ci]`
2. Builds and signs the AAB
3. Uploads to the Play Store (track selectable via `workflow_dispatch`; defaults to `production`)

Required repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `PLAY_STORE_SERVICE_ACCOUNT_JSON`.

## Tech Stack

- Kotlin 2.1.0, AGP 8.10.0, compileSdk/targetSdk 35, minSdk 26, Java 17
- Retrofit 2 + OkHttp + Gson for networking
- WorkManager for background scheduling
- Material Design 3 (`com.google.android.material`)
- `androidx.preference` for settings UI
