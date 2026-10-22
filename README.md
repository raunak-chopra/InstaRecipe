# InstaRecipe

InstaRecipe is a native Android cookbook for saving recipes discovered on Instagram. Share a Reel URL or a video with the app, use your own Google Gemini API keys to turn it into a structured recipe, and keep it in a local cookbook.

The brand direction is **Kitchen Journal + Save-to-Stove + Practical Index**: **Saved. Sorted.
Cooked.**

> Recipe extraction is assistive. Always review ingredient quantities, timings, allergens, and food-safety instructions before cooking.

## Development status — 22 October 2026

The approved UI/UX revision builds with the existing app colours. The owner approved verified checksum repairs; strict dependency verification remains enabled. The debug APK's signature verified, all 94 Gradle unit tests passed, and instrumentation tests compiled and packaged. See [APK/build evidence](docs/build-apk-2026-10-22.md) for the artifact, hash and device-test status, and [work status](docs/work-status.md) for remaining UX acceptance work.

## What it does

- Accepts shared Instagram Reel links and local video files.
- Resolves public Reel media when available, with a caption/manual-video fallback when it is not.
- Uses Gemini multimodal analysis to extract recipe metadata, ingredients, directions, timings, and notes.
- Keeps Instagram sources and unfinished recipes in Imports, distinguishing reading, missing connection, missing source details, partial content and ready-to-review states. Explicit Save to recipes requires a title, real ingredients and steps; Save draft and guarded exit keep unfinished work.
- Provides search, working tag/category filters, favorite/cooked markers, serving scaling, prep checklists, and a distraction-reduced cooking mode with a timer.
- Uses one Recipes library with visible search, compact rows, Favorites / To cook / Cooked views and a filters/sort sheet. Tap a recipe to open it; its menu retains cooking and cooked-state actions.
- Offers link, recipe-text, video and manual entry through Add recipe. Instagram access may use an explicitly connected app-private session or a configured HTTPS resolver; video/text fallback remains available.
- Supports system, light, and dark themes, plus a persistent Standard / Reduced / Off Motion preference.
- Restores one active cooking session (step, scale, checklist and timer). The timer uses elapsed-time deadlines; alerts sound while cooking mode is open. A reboot clears a running timer; no background alarm service is promised.
- Keeps cook photos optional through the recipe’s Add photo action; completion does not force a camera prompt.

## How it works

```text
Instagram Reel URL or shared video
            |
            v
Video resolver / selected local video
            |
            v
Gemini multimodal extraction (optional)
            |
            v
Imports review and editing
            |
            v
Local cookbook (Room database)
```

Extraction uses only `gemini-3.8-flash`. Recipes live in a local Room database, and configured Gemini keys are encrypted with an Android Keystore-backed key. Neither is committed to this repository.

## Requirements

- Android Studio (current stable recommended)
- JDK 17
- Android SDK Platform 37 and Build Tools 37.0.0 / Android 8.0 (API 26) or later device/emulator
- A current Gemini Auth key from [Google AI Studio](https://aistudio.google.com/) for recipe creation; an optional paid-project key can be added as fallback

## Build and run

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Alternatively, open the folder in Android Studio and run the `app` configuration.

To enable recipe creation, open **Settings**, enter a current Gemini Auth key as the primary key and optionally enter a second Auth key as backup, then use **Check connection**. Connection checks use the model-capability endpoint and do not spend generation tokens. The backup key is tried after a primary-key timeout, authentication/permission rejection, quota response, model-availability failure, or temporary Gemini outage. Both keys remain encrypted on the device.

## Project structure

```text
app/src/main/java/com/instarecipe/app/
  MainActivity.kt              Lifecycle, share intents, and root composition
  ui/screens/                  Main, search, recipe-detail, and recipe-editor screens
  InstaRecipeViewModel.kt      Recipe state, persistence, and local-video extraction ownership
  RecipeDatabase.kt            Room database, repository, and legacy-data migration
  SecurePreferences.kt         Keystore-backed primary/backup Gemini-key encryption
  TagNormalizer.kt             Tag aliases, validation, matching, and deduplication
  GeminiRecipeExtractor.kt     Gemini prompt and recipe-domain mapping
  GeminiApiClient.kt           Typed Gemini HTTP, retry, Files API, and cleanup transport
  InstagramExtractionWorker.kt Background Reel resolution, extraction, retry, and reviewable-draft persistence
  InstagramResolver.kt         Public/custom HTTPS resolution and bounded downloads
  OkHttpAwait.kt               Coroutine-cancellable OkHttp bridge
  VideoFileStore.kt            Validated, size-limited local video copying
  ui/components/               Reusable Compose components
  ui/theme/                    Compose colors, typography, and design tokens
app/src/main/res/              App icons, colors, styles, and bundled typography
```

## Privacy, security, and limitations

- Do not add API keys, signing keys, `local.properties`, or `.env` files to source control. The included `.gitignore` excludes them.
- Sharing a Reel URL may send that URL to a configured resolver and, when extraction is used, sends the selected video/caption to Google Gemini under your API account.
- Instagram may prevent automated media resolution. Use the app's manual video/caption paths when that happens.
- This is a local-first private app. Recipes use Room, cloud/device-transfer backup is disabled, and clearing app data or uninstalling removes the cookbook.
- Instagram access is opt-in for personal use: an app-private WebView session may be used for Reel imports and can be disconnected/cleared from Settings. The app does not export or log cookies.
- Resolver redirects are checked hop-by-hop, local/private network destinations are rejected, and downloads are MIME-, storage-, and size-limited.
- The bundled Elvara Sans font files remain subject to their original license. Confirm that your license permits redistribution before making the repository public.

## Version history

### Current development

- Implemented the approved 22 October recipe-first UX revision using the existing palette; see [version history](docs/version-history.md) for scope, files, rollback and verification limits.

- Added Room persistence with safe one-time migration from the previous local recipe store.
- Added Keystore-backed Gemini-key storage and header-based API authentication.
- Added tag editing, normalization, search/filter navigation, and dynamic cookbook tag filters.
- Earlier flip-card UI is superseded by directly opening compact recipe rows in the October UX revision.
- Fixed extraction to Gemini 3.8 Flash and removed obsolete model selection and model fallback behavior.
- Added encrypted primary/backup API keys with selective failover for rejected, rate-limited, or unavailable keys.
- Clarified the key order as primary and backup Auth keys, added short concurrent connection checks, and prevented Gemini retries from being multiplied by WorkManager.
- Simplified navigation to Cookbook, Imports, and Settings; integrated search into Cookbook, added a single Add recipe sheet, made recipe cards open directly, and moved technical controls under Advanced settings.
- Shared Reel links now resolve and extract through WorkManager; successful results remain populated in Imports for review, while failed drafts can be re-shared to retry. Explicit link retries open the review editor after extraction.
- Replaced manual Gemini JSON handling with typed serialization and added MockWebServer coverage for success, malformed/error responses, retries, cancellation, uploads, and cleanup.
- Hardened resolver/download boundaries; current opt-in Instagram session behavior is described in the privacy section above.
- Added Room schema migration, generated IDs, source-link uniqueness, and migration coverage.
- Upgraded to Gradle 9.7.1, AGP 9.4, Kotlin 2.4.20, API 37, Compose BOM 2026.09, OkHttp 5.5, and coroutines 1.11.

### v0.3.1 — 2026-09-10

- Refined the Material 3 culinary design system, adaptive launcher icon, typography, and light/dark/system appearance modes.
- Improved compact-screen behavior, touch-target accessibility, and API-key masking.
- Added cooking-mode usability improvements, including screen-on behavior, timer controls, ingredient peek, and completion tracking.

### v0.3.0 — 2026-09-09

- Added Instagram/local-video sharing, Reel media resolution, Gemini multimodal extraction, and Inbox review flow.
- Added cooking mode, serving scaling, checklist support, filters, and modern recipe cards.

### v0.1.0 — 2026-07-03

- Shipped the local-first Compose MVP with recipe creation, editing, search, share target support, and local persistence.

See [CHANGELOG.md](CHANGELOG.md) for the detailed release record and [IMPLEMENTATION_DIFF.md](IMPLEMENTATION_DIFF.md) for implementation tracking.

## Roadmap

- Add screenshot/OCR recipe-card import.
- Add cookbook export and backup/restore.
- Improve automatic categorisation and nutritional metadata.

## License

No open-source license has been selected for this repository. Add one before accepting external contributions or representing the code as open source.
