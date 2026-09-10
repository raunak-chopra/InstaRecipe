# InstaRecipe: Planned vs Implemented

Last updated: 2026-09-09

## Product Direction

InstaRecipe is planned as a personal Android recipe library for saving recipe videos discovered on Instagram.

The app should let the user share an Instagram Reel or post into the app, create a recipe draft, use online AI to extract structured recipe details where possible, and save the final cleaned recipe on the device.

The first version is Android-only, personal-use, and local-storage-first.

## Planned Scope

### Platform

- Android mobile app.
- Native Android recommended.
- Kotlin.
- Jetpack Compose UI.
- Room database for local storage.
- Android share target for receiving Instagram links.
- Online AI service for recipe extraction.

### Core User Flow

1. User finds a recipe video on Instagram.
2. User taps Share in Instagram.
3. User chooses InstaRecipe from the Android share sheet.
4. App receives the Instagram link.
5. App creates a recipe draft in the Inbox.
6. User adds or pastes caption/notes if needed.
7. AI suggests title, ingredients, steps, tags, category, and metadata.
8. User reviews and edits the AI output.
9. User saves the recipe to the local library.
10. User later searches and filters saved recipes.

### Planned App Sections

- Inbox.
- Library.
- Search.
- Categories.
- Favorites.
- Settings.

### Planned Recipe Fields

- Title.
- Instagram source URL.
- Creator name.
- Category.
- Tags.
- Cuisine.
- Meal type.
- Ingredients.
- Cooking steps.
- Prep time.
- Cook time.
- Servings.
- Difficulty.
- Dietary labels.
- Personal notes.
- Favorite status.
- Cooked-before status.
- Date saved.
- Last cooked date.
- AI extraction confidence.
- Original pasted caption or notes.

### Planned AI Behavior

AI should assist with recipe extraction, not silently decide final saved content.

The app should:

- Accept caption text, shared text, user notes, screenshots, or other user-provided content.
- Extract structured recipe fields.
- Show a review screen before saving.
- Mark weak extractions clearly.
- Avoid inventing details when source information is incomplete.

Suggested AI confidence states:

- Good.
- Needs review.
- Missing ingredients.
- Missing steps.
- Link only.

### Planned Search

Search should cover:

- Recipe title.
- Ingredients.
- Steps.
- Category.
- Tags.
- Cuisine.
- Creator name.
- Notes.
- Dietary labels.

### Planned Categories

Initial categories:

- Breakfast.
- Lunch.
- Dinner.
- Snacks.
- Desserts.
- Drinks.
- Meal prep.
- High protein.
- Vegetarian.
- Non-vegetarian.
- Quick recipes.
- Saved to try.
- Cooked before.
- Favorites.

## Current Implementation

No app implementation exists yet.

The repository currently contains only this planning/difference note.

- Planned vs implemented tracking document.
- Android project scaffold.
- Native Kotlin app structure.
- Jetpack Compose UI.
- Main navigation for Library, Inbox, Search, and Settings.
- Recipe list cards.
- Recipe detail screen.
- Recipe review/edit screen.
- Manual recipe creation.
- Favorite toggle.
- Cooked-before toggle.
- Basic category and tag support.
- Basic local search across title, creator, category, tags, ingredients, steps, and notes.
- Android text share target for receiving Instagram links.
- Android video share target (`video/*`) for receiving downloaded reels and gallery videos.
- Instagram URL detection and cleaning.
- Instagram video resolution and downloading via public Cobalt API endpoints.
- Multimodal AI extraction using Gemini 2.0 Flash (video visual analysis, audio voiceover transcription, on-screen text recognition).
- Structured recipe generation (title, ingredients, instructions, category, tags, prep/cook times).
- Inbox draft review and direct Instagram source link reference.
- Settings screen for Gemini API key configuration, connection testing, and custom resolvers.
- On-device persistence using Android SharedPreferences.

Not implemented:

- Room database.
- Export or backup.
- Screenshot import.
- Advanced filters.
- Ratings.
- Cooked history beyond the current cooked toggle.
- Automated unit tests.

## Implementation Gap

The core Instagram video-to-recipe AI workflow is now implemented and verified with a working debug APK build.

The next implementation step is to replace the temporary SharedPreferences persistence with Room and then add the AI extraction workflow.

## MVP Target

The first milestone should prove this flow:

1. Receive or manually paste an Instagram link. Implemented.
2. Create a recipe draft. Implemented for shared Instagram links.
3. Edit recipe details manually. Implemented.
4. Save the recipe locally. Implemented with SharedPreferences.
5. View saved recipes in a library. Implemented.
6. Search saved recipes. Implemented.
7. Open the original Instagram link from the recipe detail screen. Implemented.

AI should be added after this base flow works.

## Recommended Build Phases

### Phase 1: Local Recipe Library

Planned:

- Native Android project.
- Compose UI.
- Room database.
- Recipe list.
- Recipe detail screen.
- Add/edit recipe screen.
- Categories and tags.
- Favorites.
- Cooked-before toggle.
- Basic search.

Implemented:

- Android project scaffold.
- Compose UI.
- Local persistence using SharedPreferences.
- Recipe list.
- Recipe detail screen.
- Add/edit recipe screen.
- Categories and tags.
- Favorites.
- Cooked-before toggle.
- Basic search.

Still planned:

- Room database.
- More robust recipe field validation.
- Automated tests.

### Phase 2: Instagram Share Capture

Planned:

- Android share target.
- Receive shared text/links.
- Detect Instagram URLs.
- Create draft recipe in Inbox.
- Prevent duplicate saved links.

Implemented:

- Android share target for text/plain shares.
- Instagram URL detection.
- Draft recipe creation in Inbox.
- Duplicate saved-link prevention.

Still planned:

- More robust URL cleanup. Implemented.
- Better handling for non-Instagram shared text. Implemented.
- User feedback when a shared item has no Instagram URL. Implemented.

### Phase 3: AI Extraction

Planned:

- AI settings.
- API key handling.
- Caption/notes input.
- Structured recipe extraction.
- Review-before-save screen.
- Confidence and missing-field warnings.
- Multimodal video and audio extraction.

Implemented:

- AI settings screen with live connection test.
- Gemini API key storage in SharedPreferences.
- Gemini 2.0 Flash multimodal video/audio/visuals recipe extraction.
- Structured recipe JSON schema output (ingredients, steps, tags, prep/cook times).
- Review-before-save editor screen with manual re-extract option.
- Automatic fallback to caption/notes parsing.

### Phase 4: Rich Capture

Planned:

- Screenshot import.
- Text extraction from screenshots.
- Better extraction from incomplete recipe posts.

Implemented:

- Direct video file share (`video/*`) import from gallery/files.

### Phase 5: Personal Cookbook Polish & Brand Identity

Planned:

- Ratings.
- Cooked history.
- Advanced filters.
- Export to JSON or CSV.
- Optional backup.
- Better empty/loading/error states.
- Dedicated brand identity and app icon.
- Interactive cooking mode.

Implemented:

- **Brand Identity & Iconography (Concept A)**: Production-ready vector app icon (`ic_launcher.xml`), background (`ic_launcher_background.xml`), foreground (`ic_launcher_foreground.xml`), and adaptive icon configurations (`ic_launcher_round.xml`).
- **Culinary Design System**: Material 3 tokens (`DeepBasil`, `WarmSaffron`, `GoldenHoney`, `ToastedSesame`, `CharcoalSlate`, `HerbMuted`) configured in `res/values/colors.xml`, `styles.xml`, and Compose `Theme.kt`.
- **Interactive Kitchen Cooking Mode (`CookingModeDialog.kt`)**: Hands-free fullscreen step walkthrough with screen WakeLock (`FLAG_KEEP_SCREEN_ON`), large typography, built-in kitchen countdown timer with +1m/+5m presets, ingredients peek drawer, and 1-tap "Finish & Mark Cooked".
- **Dynamic Serving Scaler (`ServingScaler.kt`)**: 1x, 2x, 4x multiplier recalculation of ingredient measurements and checkable ingredient list with strikethrough styling.
- **Cookbook Discovery Pills (`CategoryFilterRow.kt`)**: Filter chips for `All`, `Favorites`, `Quick (<20m)`, `High Protein`, `Vegetarian`, and `Cooked`.
- **Modern Recipe Cards (`RecipeCard.kt`)**: Elevated 20dp cards with cook time, ingredients count, 1-tap Favorite/Cooked toggles, and direct "Cook" action button.
- **Loading & Empty States**: Real-time resolution/extraction banners, dismissible error banners, and expressive culinary empty states.
- Demo recipe seed.

## Key Constraints

- Instagram sharing provides a link via Android share sheet, which InstaRecipe resolves to MP4 via public Cobalt instances.
- Directly shared video files (`video/*`) from device are supported natively.
- Gemini Multimodal AI extracts structured details without user needing to re-type.
- Recipe data remains on the device by default.

## Next Action

Replace temporary local persistence with Room database:

- Room entity definitions (`RecipeEntity`).
- DAO operations (insert, update, delete, search).
- Database migration from SharedPreferences.
