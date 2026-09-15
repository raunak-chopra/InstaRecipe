# InstaRecipe

InstaRecipe is a native Android cookbook for saving recipes discovered on Instagram. Share a Reel URL or a video with the app, use your own Google Gemini API keys to turn it into a structured recipe, and keep it in a local cookbook.

> Recipe extraction is assistive. Always review ingredient quantities, timings, allergens, and food-safety instructions before cooking.

## What it does

- Accepts shared Instagram Reel links and local video files.
- Resolves public Reel media when available, with a caption/manual-video fallback when it is not.
- Uses Gemini multimodal analysis to extract recipe metadata, ingredients, directions, timings, and notes.
- Keeps unfinished or failed imports under Imports for review while completed recipes enter the cookbook.
- Provides search, working tag/category filters, favorite/cooked markers, serving scaling, prep checklists, and a distraction-reduced cooking mode with a timer.
- Uses flip cards: tap a card for its main ingredients, or use its dedicated View action to open the full recipe.
- Resolves only public Instagram content or content returned by an explicitly configured HTTPS resolver; the app never reads Instagram session cookies.
- Supports system, light, and dark themes.

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
Inbox review and editing
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

To enable recipe creation, open **Settings**, enter the free key as the primary key and optionally enter a paid-project key as fallback, then use **Check connection**. Checks run concurrently with a short deadline. The paid key is tried after a free-key timeout, authentication/permission rejection, quota response, model-availability failure, or temporary Gemini outage. Both keys remain encrypted on the device.

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
  InstagramExtractionWorker.kt Background Reel resolution, extraction, retry, and auto-save
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
- The app does not collect or reuse Instagram cookies. Private/session-only posts must be supplied as a local video or caption by the user.
- Resolver redirects are checked hop-by-hop, local/private network destinations are rejected, and downloads are MIME-, storage-, and size-limited.
- The bundled Elvara Sans font files remain subject to their original license. Confirm that your license permits redistribution before making the repository public.

## Version history

### Current development

- Added Room persistence with safe one-time migration from the previous local recipe store.
- Added Keystore-backed Gemini-key storage and header-based API authentication.
- Added tag editing, normalization, search/filter navigation, and dynamic cookbook tag filters.
- Added true flip cards with ingredient backs and separate View/Cook actions.
- Fixed extraction to Gemini 3.8 Flash and removed obsolete model selection and model fallback behavior.
- Added encrypted primary/backup API keys with selective failover for rejected, rate-limited, or unavailable keys.
- Clarified the key order as free primary and paid fallback, added short concurrent connection checks, and prevented Gemini retries from being multiplied by WorkManager.
- Simplified navigation to Cookbook, Imports, and Settings; integrated search into Cookbook, added a single Add recipe sheet, made recipe cards open directly, and moved technical controls under Advanced settings.
- Shared Reel links now resolve and extract through WorkManager; successful results move into the Saved cookbook, while failed drafts can be re-shared to retry.
- Replaced manual Gemini JSON handling with typed serialization and added MockWebServer coverage for success, malformed/error responses, retries, cancellation, uploads, and cleanup.
- Removed Instagram cookie/session handling and hardened resolver/download boundaries.
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
