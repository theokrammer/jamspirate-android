# Jamspirate! 🎵

**Jamspirate!** is an interactive, real-time music improvisation companion and generative audio studio for Android. It combines on-device algorithmic music theory synthesis with Cloud AI (Google Gemini, OpenAI ChatGPT, Anthropic Claude) to improvise expressive solos over customizable backing chord progressions.

---

## Features

- **15 Musician Profiles**: Jam alongside digital avatars of legendary artists (Miles Davis, John Coltrane, Thelonious Monk, Jimi Hendrix, B.B. King, Herbie Hancock, Stevie Wonder, Bill Evans, and more), each with signature scales, swing factors, phrasing preferences, and iconic repertoire.
- **Dual-Engine Architecture**:
  - **On-Device Algorithmic Theory Engine**: Zero-latency, offline synthesis respecting modal theory, chord-scale relationships, tension/resolution, and syncopation.
  - **Cloud AI Engine**: Generates creative musical phrases using Google Gemini, ChatGPT, or Claude.
- **Lookahead Coroutine Batching**: Pre-fetches future bars asynchronously so cloud-generated notes play precisely on the downbeat with 0ms playback latency.
- **Famous Piece Prompt Mode**: Allows prompting the cloud model using an artist's iconic masterpiece (e.g. *So What*, *Giant Steps*, *Voodoo Child*, *The Thrill Is Gone*) instead of numerical parameters.
- **Custom AI Prompt Directives**: Write custom phrasing rules or choose from quick style presets (Blues, Bebop, Ambient) with a compiled prompt preview inspector.
- **Fail-Safe Local Backup**: Automatically falls back to the algorithmic engine during network hiccups or buffer underruns, or switch to pure cloud output.
- **Built-In Polyphonic Synthesizer**: Native PCM audio engine with real-time waveform generation, ADSR envelope shaping, and harmonic backing chord accompaniment.
- **Interactive Studio Controls**: Real-time tempo adjustment (BPM), key transpositions, scale selection, chord progression customization, and visual piano roll.

---

## Local Development & Build Guide

### 1. Prerequisites
- **JDK 17 or JDK 21**: Required for Android Gradle Plugin and Kotlin compilation.
- **Android Studio** (Ladybug, Koala, or newer):
  - Android SDK (API 26 through API 34/35)
  - Android SDK Platform-Tools & Build-Tools
- **Target Device**: Physical Android device or Android Virtual Device (AVD) running **Android 8.0+ (API level 26 or higher)**.

---

### 2. Getting Started
1. **Clone or Extract the Project**:
   ```bash
   git clone <repository-url>
   cd jamspirate
   ```
2. **Open in Android Studio**:
   - Select **Open** and choose the project root folder.
   - Wait for Android Studio to download dependencies and sync Gradle.

3. **API Keys (Optional)**:
   - The on-device theory engine works out of the box with **zero setup or network requirements**.
   - To use Cloud AI generation (Gemini, ChatGPT, or Claude), you can either enter your API key directly in the in-app **Cloud AI & Lookahead** menu or configure them in `.env`.

---

### 3. Build & Run

#### Using Android Studio:
1. Select the `app` run configuration in the toolbar.
2. Select your connected device or emulator in the dropdown.
3. Click the **Run** button (`Shift + F10`).

#### Using the Command Line:
- **Build the Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
  The generated APK will be located at:
  ```
  app/build/outputs/apk/debug/app-debug.apk
  ```

- **Install and Launch on Connected Device/Emulator**:
  ```bash
  ./gradlew installDebug
  adb shell am start -n com.example/.MainActivity
  ```

#### Automated GitHub Releases:
You can also generate and publish downloadable APKs directly to GitHub Releases via GitHub Actions:
1. Go to the repository's **Actions** tab.
2. Select **Build & Release Android APK**.
3. Click **Run workflow**, specify the release tag (e.g. `v1.0.0`) and title, then run.
4. The workflow will build the APK and publish it as a public asset under your repository's **Releases** page.

---

### 4. Running Tests

- **Run JVM & Robolectric Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- Test reports are generated at:
  ```
  app/build/reports/tests/testDebugUnitTest/index.html
  ```

---

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
