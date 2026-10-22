# InstaRecipe behavior history

## 22 October 2026 — checksum repair and debug APK

The owner explicitly authorized fixing the missing checksum and building the APK, superseding the earlier metadata restriction. Three missing existing transitive POM entries were individually checked against fresh Maven Central bytes and published SHA-1 values, then recorded as SHA-256 entries in `gradle/verification-metadata.xml`: Guava parent 33.4.0-jre, coroutines BOM 1.10.2 and Guava parent 33.2.1-jre. No dependencies were added/upgraded; strict verification was retained.

`assembleDebug` succeeded, all **94 Gradle unit tests passed**, instrumentation source compiled and the test APK packaged. The app APK signature verified. Artifact identity, checksum evidence and current device coverage are recorded in [build evidence](build-apk-2026-10-22.md). No source compile fixes, signing/configuration changes, app-data clearing, commits, push or publication were needed for this build.

Rollback for the metadata repair is removal of only those three component entries; this restores the previous verification blocker rather than producing a usable build. Do not weaken verification. The source redesign record below describes its earlier implementation stage; its original build blocker is now resolved.

## 22 October 2026 — approved recipe-first UI/UX source revision

**State:** Implemented in source; Android compilation/runtime acceptance and independent review pending. The owner retained the existing colours and personal Gemini-key model, and approved A's hierarchy with C's compact rows. No release number, commit, push, PR or publication is implied.

### What changed and why

- Consolidated primary navigation into Recipes / Imports / Settings. Search precedes decorative content; All/Favorites/To cook/Cooked views and category/tag/diet/quick filters share one library. Empty and failed-search states offer recovery. Add exposes link/text/video/manual sources.
- Added dedicated import states and primary actions. Empty drafts no longer show recipe counts or a Ready to cook label; review-ready drafts open the editor directly. New recipe-link entry rejects profile/unsupported links before extraction. Missing-key link/text sources can stay drafts; manual editing remains available.
- Put title, ingredients and method first in review; move optional facts later and source recovery first for empty linked imports. Draft saving allows unfinished content. Final save enforces the existing quality gate, waits for persistence and retains fields on failure. Back/Cancel guard unsaved work. Refresh keeps notes and cancels with the editor. Incoming/completed imports do not replace an open editor.
- Preserve meaningful partial extraction and missing-details notes. Partial results remain reviewable Drafts; they do not bypass final cookbook quality checks. No Room migration or typed database-state replacement was introduced.
- Replace active flip-card composition with compact directly opening rows, retaining favorite, cook/cooked/tag menu actions and private photos. Detail uses a smaller title, optional real cook photo, ingredients/method/notes jumps, wrapping actions and a persistent cook control. Delete is in the menu with confirmation; cooked toggling offers Undo. Existing dormant private card functions remain to avoid broadly deleting owner-modified code.
- Persist one active cooking step/scale/checklist/timer in separate private preferences. Timer uses elapsed-time deadlines; finish clears the session. Alerts sound while cooking mode is open; reboot clears a running timer. Background alarms and multiple sessions are outside scope. Completion keeps optional cook-photo capture available without opening it automatically.
- Reuse existing light/dark semantic colour pairs. Add a 12dp recipe/import card radius; retain palette values, fonts, dependencies, build metadata, schema and transport retry behavior.

### Files

Root state/services: MainActivity.kt, InstaRecipeViewModel.kt, RecipePresentation.kt, RecipeQuality.kt, GeminiRecipeExtractor.kt, CookingSession.kt and CookingSessions.kt.

Screens/components: MainScreen.kt, RecipeLibraryScreen.kt, AddRecipeSheet.kt, ImportsScreen.kt, RecipeEditorScreen.kt, RecipeDetailScreen.kt, ImportCard.kt, CompactRecipeSummary.kt, RecipeCard.kt, CookingModeDialog.kt and DesignTokens.kt.

Tests: CookingTimerStateTest.kt, RecipePresentationTest.kt, PartialExtractionTest.kt, RecipeCardAccessibilityTest.kt and RecipeEditorDraftTest.kt. Pre-existing ServingScaler source/test edits were preserved. Documentation: README.md, DESIGN_SYSTEM.md, work-status.md, implementation checklist, concept approval status and verification evidence.

### Verification and remaining risks

See [the verification record](ux-implementation-verification-2026-10-22.md). Supplementary syntax and pure JVM checks passed, but strict Gradle verification blocks Android compilation. Partial parsing, Compose/type/resource integration, device behavior, accessibility, insets, native after screenshots and independent review are unverified. The owner declined the missing-checksum metadata edit; no workaround disables verification.

### Rollback and data compatibility

Initial owner-modified sources are preserved locally in ignored `scratch/ux-before-2026-10-22/src`; base HEAD was `12edc97`, with eight pre-existing modified source/test files. Roll back only this revision's reviewed hunks using that snapshot as context. **Do not reset the repository or copy the entire snapshot over later owner work.** New files may be removed individually only after verifying references. No automated rollback or deletion was performed.

Existing Room recipe rows, IDs, source links, favorite/cooked fields, tags, notes and photo paths remain compatible; no database migration or app-data clearing is required. New cooking-session preferences can remain inert under an older build. Draft saves intentionally preserve unfinished work in Imports; promoting a reviewed recipe removes workflow tags while retaining other tags. Existing encrypted key and Instagram-session storage are unchanged.
