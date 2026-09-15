# InstaRecipe Development & Update Log

## Project Summary
**InstaRecipe** is a native Android personal cookbook application designed to save and organize recipes discovered on Instagram. Users can share Instagram cooking Reels (or local video files) directly into the app, which resolves public video content and uses **Gemini 3.8 Flash** to generate structured, editable recipes with direct references back to the original Instagram post.

---

## Release & Version History

### Unreleased: Gemini 3.8, Security, Data Integrity & Toolchain

- Defined the encrypted key order as free primary then paid fallback. Connection checks now run concurrently with a 12-second deadline, timeout and Files API failures can use the paid key, and Gemini failures are not retried again by WorkManager.
- Simplified the consumer shell to three destinations (Cookbook, Imports, Settings), moved search into Cookbook, added a unified Add recipe sheet, made recipe cards open directly, hid technical controls under Advanced settings, and replaced prominent AI/diagnostic copy with task-focused language.
- Standardized all AI extraction and connection testing on `gemini-3.8-flash`; removed obsolete 2.x/1.x selection and fallback paths.
- Replaced blocking OkHttp calls with coroutine-cancellable calls, bounded retries, separate generation/upload timeouts, safe user-facing failures, and bounded best-effort remote-file cleanup.
- Removed Instagram cookie/session extraction and public community-resolver defaults. Only public Instagram metadata or an explicitly configured HTTPS resolver is used.
- Added HTTPS-only redirect validation, local/private-address rejection, video MIME checks, 200 MB limits, storage checks, and partial-file cleanup.
- Added explicit confirmation before external shares trigger network/AI processing and made share intents one-shot after acceptance.
- Migrated Room to generated primary keys with an explicit v1-to-v2 migration, unique normalized source URLs, conflict-safe updates, exported schemas, and a migration instrumentation test.
- Added constructor-injected recipe storage, saveable editor/detail/cooking state, precise serving scaling, and stable accessible ingredient toggles.
- Upgraded to Gradle 9.7.1, AGP 9.4, Kotlin 2.4.20, API 37, Compose BOM 2026.09, Lifecycle 2.11, WorkManager 2.11.2, OkHttp 5.5, coroutines 1.11, KSP, and a version catalog.
- Enabled release shrinking/obfuscation and added unit coverage for the fixed Gemini model and serving scaling.
- Extracted main, detail, and editor Compose screens from `MainActivity`, propagated generated Room IDs before opening saved recipes, and added a regression test for persisted identity.
- Moved gallery/shared-video extraction into ViewModel-owned state so accepted imports and progress survive Activity recreation; added duplicate-start, retained-completion, error, cancellation-aware I/O, and cleanup safeguards.
- Completed the Gradle 9.7.1 wrapper upgrade with the official distribution and bootstrap-JAR checksums.
- Replaced manual Gemini JSON parsing with typed kotlinx.serialization DTOs and added MockWebServer coverage for success, malformed/error responses, retries, cancellation, Files API uploads, and remote cleanup.
- Added encrypted primary and backup Gemini keys with selective failover; accepted Reel links run through WorkManager, successful extraction auto-saves into the cookbook, and failed drafts can be reclaimed by sharing the link again.
- Verification evidence is recorded in `IMPLEMENTATION_DIFF.md`.

### Previous unreleased work: Reliability, Local Data, Tags & Recipe Cards

- Migrated recipes to Room with a one-time, failure-safe import of the previous on-device recipe data.
- Added lifecycle-aware database state, recipe deletion confirmation, and system-back handling for detail, editor, and cooking views.
- Encrypted the Gemini API key with Android Keystore, moved API authentication to request headers, and added large-video processing checks and cleanup.
- Previously retained optional in-app Instagram sign-in; this has now been removed by the security upgrade above.
- Added normalized editable tags, tag-aware search and cookbook filtering, and dynamic tag filter chips.
- Reworked recipe tiles as elevated flip cards: card tap reveals main ingredients while dedicated View and Cook actions enter the recipe flows.
- Updated the Android/API 36 build stack and added unit coverage for tag normalization and Instagram URL canonicalization.
- Verification: unit tests, `:app:assembleDebug`, and `:app:lintDebug` pass; lint reports 0 errors.

---

### [v0.3.1] - 2026-09-10: Production UI Polish, Accessible Themes & Typography
**A full visual-system pass that makes the culinary identity consistent in light and dark environments.**

- Rebuilt the Compose color schemes around semantic Material 3 roles so cards, navigation, dialogs, chips, editor surfaces, progress states, and cooking mode adapt consistently.
- Added a persistent `System / Light / Dark` appearance control and synchronized status/navigation bar icon contrast.
- Integrated licensed **Elvara Sans** Regular, Medium, SemiBold, and Bold files from the user-supplied Envato library for a coherent product typography hierarchy.
- Improved compact-screen behavior in the Inbox import panel, raised recipe-card action targets to 48dp, clarified stateful accessibility labels, and obscured the Gemini API key by default.
- Removed unnecessary media-library permissions; document-picker and shared-URI flows retain scoped access.
- Added a monochrome adaptive launcher asset for Android themed icons and corrected the manifest to use the adaptive launcher resource.
- Historical verification at v0.3.1: `:app:assembleDebug` and `:app:lintDebug` passed with 0 lint errors. Current verification is recorded in `IMPLEMENTATION_DIFF.md`.
- Visual QA status: manual code/token review complete; device screenshots remain pending because no emulator or Android device was connected.

---

### [v0.3.0] - 2026-09-09: Production Brand Identity, Culinary Design System & Interactive Cooking Mode
**Major UI/UX Overhaul: Concept A brand identity, culinary Material 3 tokens, hands-free cooking mode with timer, dynamic serving scaler, and cookbook filter pills.**

#### 🎨 Brand Identity & Iconography (Concept A)
1. **Adaptive Vector App Icon System**:
   - Implemented Concept A brand mark: modern chef's cloche hat merged with an Instagram Reel clapperboard / play glyph and Gemini AI sparkle stars.
   - Vector drawables:
     - `res/drawable/ic_launcher.xml`: Standalone vector combining background squircle, Deep Basil chef hat, Warm Saffron clapper with diagonal stripes, white play glyph, and Golden Honey sparkle stars.
     - `res/drawable/ic_launcher_background.xml`: Warm Toasted Sesame `#FAF8F5` background.
     - `res/drawable/ic_launcher_foreground.xml`: Safe-zone conforming foreground vector.
     - `res/mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`: Adaptive icon configurations for modern Android launchers.
   - Updated `AndroidManifest.xml` with `android:roundIcon="@mipmap/ic_launcher_round"`.

#### 🍳 Culinary Design System & Material 3 Theme
2. **Design Tokens & Palette**:
   - `DeepBasil` (`#2D6A4F`): Primary branding, active states, key buttons.
   - `WarmSaffron` (`#E07A5F`): Secondary accent, reel indicators, and favorite states.
   - `GoldenHoney` (`#F4A261`): Chef notes card, timers, and sparkle accents.
   - `ToastedSesame` (`#FAF8F5`): App background and scaffold, reducing glare in kitchen environments.
   - `CharcoalSlate` (`#1A201E`): High-contrast readable typography.
   - `SuccessSage` (`#2E7D32`): Cooked badges and checkbox confirmations.
   - `AlertPaprika` (`#C62828`): Extraction warning states.
   - Declared in `res/values/colors.xml`, `res/values/styles.xml`, and Compose `ui/theme/Color.kt` & `ui/theme/Theme.kt`.
   - Typography system tuned for kitchen readability.

#### ⏱ Interactive Kitchen Cooking Mode (`CookingModeDialog.kt`)
3. **Hands-Free Cooking Experience**:
   - **Screen WakeLock**: Uses `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` via `DisposableEffect` so the device screen stays awake while cooking.
   - **Step-by-Step Counter**: High-contrast 21sp instruction text with large navigation buttons (`Previous` and `Next Step`).
   - **Built-in Kitchen Timer**: Real-time countdown timer with quick preset chips (`+1m`, `+5m`), play, pause, and reset controls.
   - **Collapsible Ingredients Peek**: Quickly inspect ingredients without exiting the active cooking step.
   - **Finish & Mark Cooked**: 1-tap completion button that marks the recipe as cooked in the local library.

#### ⚖ Dynamic Serving Scaler & Interactive Checklist (`ServingScaler.kt`)
4. **Recipe Scaling & Prep Tracking**:
   - Dynamic multiplier selector (`1x`, `2x`, `4x`) that scales ingredient quantities (e.g. `200g` -> `400g`).
   - Interactive checkboxes allowing cooks to cross off prepped ingredients with strikethrough.

#### 🔍 Cookbook Discovery Pills & Modern Cards (`CategoryFilterRow.kt` & `RecipeCard.kt`)
5. **Enhanced Feed & Browsing UX**:
   - Horizontal category filter pills in **My Cookbook**: `All`, `Favorites`, `Quick (<20m)`, `High Protein`, `Vegetarian`, `Cooked`.
   - Modern recipe cards with category badges, extracted cook time pill, ingredient count, 1-tap Favorite/Cooked toggles, and direct **"Cook"** button to jump straight into cooking mode.
   - Expressive culinary empty states with guided instructions for sharing from Instagram.

#### 🛠 Verification & Build Status
- Build command: `.\gradlew.bat assembleDebug`
- Status: `BUILD SUCCESSFUL in 1m 4s` (36 actionable tasks executed / up-to-date, 0 errors).
- Output: `app/build/outputs/apk/debug/app-debug.apk`.

---

### [v0.2.0] - 2026-09-09: Instagram Video Sharing & Gemini Multimodal AI Extraction
**Major Feature Release: Direct video/link sharing, video stream resolution, and multimodal AI recipe extraction.**

#### 🚀 New Features & Capabilities
1. **Direct Share Target & Intent Handling**:
   - Added support for `video/*` shares in `AndroidManifest.xml`, allowing users to share downloaded Instagram reels, screen recordings, or gallery video files directly into InstaRecipe.
   - Enhanced `text/plain` share handling to reliably extract and clean Instagram URLs, stripping tracking parameters (such as `?igsh=...` and `?utm_source=...`).
   - Added `READ_MEDIA_VIDEO` and `READ_EXTERNAL_STORAGE` permissions to ensure seamless access to shared media files.

2. **Instagram Video Stream Resolution (`InstagramResolver.kt`)**:
   - Implemented automatic resolution of direct MP4 video streams from Instagram URLs using public Cobalt API instances (`https://cobalt-api.kwiatekm.pl`, `https://api.cobalt.tools`, etc.).
   - Downloads reel videos to temporary app cache for multimodal analysis.
   - Fallback mechanism to query Instagram oEmbed metadata if direct video stream retrieval is restricted.

3. **Gemini 3.1 Flash-Lite Multimodal Recipe Extraction (`GeminiRecipeExtractor.kt`)**:
   - Standardized on Google Gemini 3.1 Flash-Lite — the most cost-effective ($0.25 / 1M tokens), low-latency multimodal model for cooking video analysis.
   - Migrated from legacy `generateContent` to the modern **Interactions API** (`/v1beta/interactions`) to ensure full Gemini 3 series compatibility.
   - Built-in capacity fallback to `gemini-3.8-flash` if `3.1-flash-lite` experiences temporary server capacity shortages (`503 MODEL_CAPACITY_EXHAUSTED`).
   - Multimodal analysis captures:
     - **Visuals**: Cooking steps, technique, pan heat, ingredient changes.
     - **Audio**: Spoken voiceover instructions and chef tips.
     - **On-Screen Text**: Text overlays showing ingredient quantities, cooking times, and temperatures.
   - Smart upload mechanism:
     - Inline Base64 encoding for video files up to 16 MB (fast, single-request processing).
     - Gemini Resumable File API for larger video files (> 16 MB).
   - Structured JSON schema enforcement: extracts Recipe Title, Creator, Category, Tags, Ingredient list with exact quantities, Numbered Directions, Prep/Cook time, and Notes.
   - Caption-only fallback when video streams are unavailable.

4. **Interactive UI & Review Workflow (`MainActivity.kt`)**:
   - **Real-Time Progress Banners**: Visual step indicators during video resolution, download, and Gemini analysis (*"Connecting to Instagram..."* → *"Downloading reel video..."* → *"Gemini AI watching video & reading recipe..."*).
   - **Recipe Editor Enhancements**:
     - Pre-populated with extracted recipe fields.
     - Added a *"Re-extract with Gemini AI"* button directly inside the editor for manual links or re-runs.
   - **Recipe Detail Improvements**:
     - Added an *"Open Instagram Reel"* button linking directly to the source post via `ACTION_VIEW`.
   - **Settings Screen**:
     - Configurable Gemini API Key storage in `SharedPreferences`.
     - Built-in *"Test Connection"* tool with live status feedback.
     - Configurable custom video resolver endpoint (defaults to public Cobalt instances).
     - Step-by-step Instagram sharing instructions guide.

#### 🛠 Build & Toolchain Upgrades
- Added `gradle.properties` configuring `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, and optimized JVM arguments (`-Xmx2048m`).
- Configured Java 17 compile options (`sourceCompatibility` and `targetCompatibility`) in `app/build.gradle.kts`.
- Added dependencies:
  - `com.squareup.okhttp3:okhttp:4.12.0` (resilient HTTP client for video streaming and Gemini REST calls).
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0` (asynchronous background processing).
- Generated Gradle Wrapper scripts (`gradlew`, `gradlew.bat`) on Gradle 8.13.
- Successfully compiled and assembled debug APK:
  - Command: `.\gradlew.bat assembleDebug`
  - Output: `app/build/outputs/apk/debug/app-debug.apk` (16.7 MB).

---

### [v0.1.0] - 2026-07-03: Initial Local-First MVP Shell
**Initial Release: Project foundation, UI shell, and local manual recipe management.**

#### 🚀 Features Implemented
- Native Android app created with Kotlin and Jetpack Compose.
- 4-tab bottom navigation: **Library**, **Inbox**, **Search**, and **Settings**.
- Local recipe management:
  - Manual recipe creation and editing (title, creator, category, tags, ingredients, steps, notes).
  - Favorite toggle and cooked-before toggle.
  - Tag chips and category organization.
- Basic search across recipe title, creator, category, tags, ingredients, steps, and notes.
- Initial Android share target for `text/plain` shares:
  - Instagram URL regex detection.
  - Basic draft creation in the Inbox.
  - Duplicate source URL prevention.
- On-device local persistence using Android `SharedPreferences` with JSON serialization.
- Seed demo recipe for new installs (*Paneer Pepper Toast*).

---

## Technical Architecture & Pipeline

```
[Instagram App / Gallery / Files]
          │
          ▼ (User taps Share -> InstaRecipe)
[Android Intent: ACTION_SEND]
  ├─ text/plain (Reel URL) ───► InstagramResolver ──► Public Cobalt API ──► Download MP4
  └─ video/* (Local Video) ───► Cache Copy (MP4) ──────────────────────────┘
                                                              │
                                                              ▼
                                              Gemini 2.0 Flash Multimodal API
                                              (Visuals + Voiceover + On-Screen Text)
                                                              │
                                                              ▼
                                              Structured JSON Recipe Output
                                                              │
                                                              ▼
                                                Inbox Draft Review Screen
                                            (User edits, checks link, saves)
                                                              │
                                                              ▼
                                                 Local Recipe Library
```

---

## Verification & Build Status

| Task | Command | Result | Details |
| :--- | :--- | :--- | :--- |
| Gradle Tasks | `.\gradlew.bat tasks` | ✅ Passed | Toolchain detected Java 17 & Android SDK 35 |
| Kotlin Compilation | `.\gradlew.bat compileDebugSources` | ✅ Passed | Clean compile, 0 errors |
| APK Packaging | `.\gradlew.bat assembleDebug` | ✅ Passed | Built in 2m 39s; APK size: 16.7 MB |
| Artifact | `app/build/outputs/apk/debug/app-debug.apk` | ✅ Verified | Ready for install on device/emulator |

---

## Upcoming Roadmap

1. **Room Database**: Migrate from current `SharedPreferences` JSON storage to an Android Room SQLite database for improved querying, indexing, and scalability.
2. **Screenshot & OCR Import**: Support sharing screenshots of recipe cards or captions from Instagram stories.
3. **Backup & Export**: Export cookbook to JSON or Markdown / PDF files, and import backups.
4. **Enhanced Categorization**: Automatic tagging based on cuisine, meal prep, difficulty, and nutritional tags.
