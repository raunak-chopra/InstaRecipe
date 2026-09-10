# InstaRecipe

InstaRecipe is a native Android cookbook for saving recipes discovered on Instagram. Share a Reel URL or a video with the app, use your own Google Gemini API key to turn it into a structured recipe draft, review the result, and keep it in a local cookbook.

> Recipe extraction is assistive. Always review ingredient quantities, timings, allergens, and food-safety instructions before cooking.

## What it does

- Accepts shared Instagram Reel links and local video files.
- Resolves public Reel media when available, with a caption/manual-video fallback when it is not.
- Uses Gemini multimodal analysis to extract recipe metadata, ingredients, directions, timings, and notes.
- Keeps extracted recipes in an Inbox for review before saving them locally.
- Provides search, favorite/cooked markers, category filters, serving scaling, prep checklists, and a distraction-reduced cooking mode with a timer.
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
Local cookbook (SharedPreferences)
```

The default extraction model is `gemini-2.5-flash`; the user can select from the models exposed in the Settings screen. API keys and recipes are stored locally on the device and are not committed to this repository.

## Requirements

- Android Studio (current stable recommended)
- JDK 17
- Android SDK 35 / Android 8.0 (API 26) or later device/emulator
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/) for AI extraction

## Build and run

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Alternatively, open the folder in Android Studio and run the `app` configuration.

To enable extraction, open **Settings** in the app, enter a Gemini API key, choose a model if needed, and use **Test Connection**. The key remains on the device in the app's private preferences.

## Project structure

```text
app/src/main/java/com/instarecipe/app/
  MainActivity.kt              App UI, navigation, recipe storage, and share handling
  GeminiRecipeExtractor.kt     Gemini upload, prompt, and structured-response parsing
  InstagramResolver.kt         Reel URL resolution and download handling
  InstagramSessionManager.kt   Optional local Instagram session-cookie helpers
  ui/                          Compose components and theme
app/src/main/res/              App icons, colors, styles, and bundled typography
```

## Privacy, security, and limitations

- Do not add API keys, signing keys, `local.properties`, or `.env` files to source control. The included `.gitignore` excludes them.
- Sharing a Reel URL may send that URL to a configured resolver and, when extraction is used, sends the selected video/caption to Google Gemini under your API account.
- Instagram may prevent automated media resolution. Use the app's manual video/caption paths when that happens.
- This is a local-first app. Recipes currently use `SharedPreferences`; uninstalling the app or clearing its data removes them unless you export a backup.
- The bundled Elvara Sans font files remain subject to their original license. Confirm that your license permits redistribution before making the repository public.

## Version history

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

- Migrate local storage to Room.
- Add screenshot/OCR recipe-card import.
- Add cookbook export and backup/restore.
- Improve automatic categorisation and nutritional metadata.

## License

No open-source license has been selected for this repository. Add one before accepting external contributions or representing the code as open source.
