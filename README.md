# Hifth

Hifth is an offline-first Android Quran memorization and study app built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, coroutines, and StateFlow. It targets Android 7.0 (API 24) and newer.

## Features

- Browse all surahs, load juz by number, and filter custom ayah ranges. Long surahs load in 50-ayah pages.
- Read Uthmani Arabic with adjustable type size and an Arabic-only switch; open an ayah mini-reader, translations, and available tafsir.
- Choose a reciter, stream ayah audio, and tap individual words for their translation and transliteration. Save words to a local vocabulary list.
- Build and save memorization plans by ayah numbers or by selecting ayahs while reading.
- Practice recall or translation with hide/reveal, word-level checking, corrections, error percentages, saved weak spots, and a daily study streak.
- Mark ayahs memorized and browse completed sets with their dates.
- Record recitation to app-private storage, associate it with an ayah, and play saved recordings with seek controls.
- Follow system light/dark mode with a quiet emerald-and-gold Material 3 palette.

## Build and test

Install Android Studio with JDK 17 and Android SDK Platform 35. From the repository root, run:

```powershell
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

The app's minimum SDK is 24. Recording requires granting the microphone permission at runtime. Quran text, translations, tafsir, and reciter audio are fetched on demand; already fetched text and study data remain on-device in Room/DataStore for offline use. A first install needs an internet connection to sync content.

## Quran.com API and credentials

The app uses the Quran.com v4-compatible content endpoints for chapters, verses, word translations/transliterations, tafsir, and recitation audio. By default it connects to `https://api.quran.com/api/v4/`. To use a deployment-owned API proxy, set the Gradle project property `quranApiBaseUrl` to the proxy's compatible API base URL, for example in the local user's `~/.gradle/gradle.properties`:

```properties
quranApiBaseUrl=https://your-api.example/content/api/v4/
```

Do not put OAuth client secrets in this Android app. Quran Foundation's authenticated Content API uses the confidential OAuth2 Client Credentials flow, which must run on a trusted backend; a proxy can hold those credentials and forward compatible v4 requests. Hifth does not implement user sign-in/PKCE because all requested study data is local and the app does not call Quran.com User APIs. If account-based cloud sync is added later, use the official public-client Authorization Code + PKCE flow for User APIs rather than embedding a secret.

Quran.com v4's word-by-word response supplies translations and transliterations, but does not supply root, lemma, or grammar fields in its verse payload. The word detail view displays such fields when a configured content response provides them and states when they are unavailable; it does not fabricate linguistic analysis. The content API's available tafsir resources are loaded dynamically. Network-dependent features report errors and cached Quran text remains available offline.
