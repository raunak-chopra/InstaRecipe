# InstaRecipe simplification and link re-extraction plan

**State:** Reviewed; implementation not started
**Date:** 2026-09-16
**Scope:** Android app only; no release, signing, credential, production, or external-system changes.

## Outcome

Give a failed Instagram import one consistent **Extract from link again** action in both the **Imports** and **Recipes** tabs, while removing bounded duplication and documenting larger simplifications as follow-up work. Keep the existing single-module Compose, Room, repository, and WorkManager architecture.

For this plan, “failed linked import” means a recipe that:

- has a valid HTTPS Instagram post or Reel URL;
- is not currently tagged `Processing`; and
- has the normalized `Instagram` and `Needs review` tags plus a recognized import-failure status suffix in its notes.

“Recipes menu” is interpreted as the Recipes tab/list. The same action should remain available on the read-only recipe detail screen, but not inside the writable editor, and a new global overflow-menu system is not required.

## Current-state findings

### Already working

- Failed drafts in Imports already render a full-width **Import from link again** action.
- Retry work is already uniquely named by normalized source URL, although the explicit retry policy must change from `REPLACE` to `KEEP` to avoid cancellation races.
- The original shared caption is recovered without appending the previous failure message.
- Successful retry preserves the database ID, favorite state, and cooked state, then moves the recipe to `Saved`.
- Baseline verification on 2026-09-16: `gradlew.bat testDebugUnitTest` passed **46 tests**, with 0 failures and 0 skipped.

### Functional gap

Recipes only receives `Saved` records, while `claimInstagramExtraction` intentionally returns `null` for every saved recipe. Reusing the current retry callback in Recipes would therefore enqueue work that exits successfully without doing an extraction. A Recipes-side action must use an explicit, validated retry target rather than weakening normal share deduplication.

### Overengineered or unnecessarily coupled pieces

1. **Three extraction state owners.** `MainActivity` owns general extraction progress and retry IDs, `InstaRecipeViewModel` owns local-video progress, and `RecipeEditor` owns another extraction flag/message pair. The same user job has different loading and error paths depending on entry point.
2. **Activity-level extraction pipeline.** `MainActivity` validates URLs, reads settings, resolves Instagram media, invokes Gemini, manages temporary files, and translates failures for the editor callback. This mixes root composition and navigation with data work.
3. **Repeated editor mapping.** `RecipeEditorScreen.kt` copies `ExtractedRecipeData` into editor fields in three branches: attached video, pasted text, and link refresh. The copies can drift; they already differ in how notes are replaced or appended.
4. **Full recipe snapshots in saved UI state.** `MainActivity` manually serializes every `Recipe` field and stores separate editing, viewing, and cooking snapshots. This is a large saver, duplicates durable Room data, and can display stale data after WorkManager updates the same row.
5. **Import failure encoded in presentation text.** Retry eligibility and original-caption recovery depend on tags plus status text appended to `notes`, including a legacy regular expression. This is brittle. The bounded implementation will centralize and strictly recognize the existing markers; typed persisted state remains a later migration if the product expands this workflow.
6. **Screen file concentration.** `MainScreen.kt` is about 1,100 lines and combines root scaffold, Recipes, Imports, Instagram login, and Settings. Splitting those composables by existing screen boundary improves ownership without adding modules or frameworks.

### Complexity that should remain

- Keep the single `:app` module; there is no demonstrated module-isolation need.
- Keep manual ViewModel construction; adding Hilt/Koin for one ViewModel would add more machinery than it removes.
- Keep Room source-URL uniqueness and unique WorkManager jobs; both protect data integrity and duplicate work.
- Keep Gemini transport retry/fallback policy separate from WorkManager retry policy; they cover different failure boundaries.
- Do not add a generic action framework for recipe cards. One optional retry action is still simpler than a hierarchy of menu/action models.

## Proposed behavior

### Imports tab

- Replace the label with **Extract from link again** for consistency.
- Show it only for a failed linked import.
- On tap, disable the action immediately and show **Starting extraction…** until the persisted recipe changes to `Processing` or scheduling fails.
- While processing, the card remains in Imports and exposes no second retry action.
- On failure, the action returns with the new safe failure message.
- On success, the item moves to Recipes as it does today.

### Recipes tab

- Show **Extract from link again** on a saved card only when it has a valid Instagram URL and recognized Instagram-import failure provenance.
- Treat this as an explicit user-authorized refresh of that one recipe, not as normal share import deduplication.
- Keep the saved recipe and every user-edited field intact while extraction runs. Disable its action and show **Extracting from link…** on that card.
- On success, atomically replace the extracted recipe fields only if the stored recipe is still identical to the snapshot that was retried. Preserve ID, favorite, cooked, and saved date.
- On resolver, network, AI, cancellation, process-death, edit-conflict, or deletion outcomes, never recreate or downgrade the saved row. Leave its prior content unchanged and expose a recoverable result when the app is able to observe one.
- If API credentials are absent, keep the recipe unchanged, open the existing You/Settings destination, and show the existing connection guidance.
- If scheduling fails, keep the recipe in Recipes and show a recoverable error.

### Open recipe/detail

- Keep one visible **Extract from link again** action for an eligible failed linked recipe.
- Route it through the same retry command used by both tabs.
- A normal saved recipe without `Needs review` must not expose the action.
- Do not offer background link retry inside the writable editor. This avoids racing unsaved editor fields against a worker update. Retain the existing foreground editor tool as a distinct draft-local action and label it **Refresh draft from link**, not **Extract from link again**.

## Implementation steps

### 1. Define one eligibility rule

Affected paths:

- `app/src/main/java/com/instarecipe/app/InstagramExtractionWorker.kt` or a small adjacent `InstagramImportState.kt`
- `app/src/test/java/com/instarecipe/app/InstagramExtractionStateTest.kt`

Add small pure helpers such as `Recipe.isFailedInstagramImport()` and `Recipe.canExtractFromLinkAgain()`. Centralize the valid-link, normalized `Instagram` tag, `Needs review`, recognized failure suffix, and non-processing checks. Use these helpers in Imports, Recipes, detail, and retry validation. A user-authored saved recipe that merely has an Instagram URL or a `Needs review` tag must not qualify.

Do not add a Room column in this pass. Record a later migration only if import history or richer failure reasons become a product requirement.

### 2. Make retry target one exact recipe

Affected paths:

- `app/src/main/java/com/instarecipe/app/InstagramExtractionWorker.kt`
- `app/src/main/java/com/instarecipe/app/RecipeDatabase.kt`
- `app/src/test/java/com/instarecipe/app/InstagramExtractionStateTest.kt`

Extend only the explicit retry request with the recipe ID and a durable expected fingerprint captured from the recipe when the user authorizes retry. Compute the SHA-256 fingerprint from a deterministic, unambiguous encoding of every persisted recipe field, including ID, normalized source URL, content, tags, status, favorite/cooked state, and saved date. Put the compact fingerprint in WorkManager input so it survives backoff, automatic retry, and process death.

Add a repository operation that loads the target by ID and validates all of the following before every attempt starts:

- the target ID still exists;
- the target still satisfies `canExtractFromLinkAgain()`;
- its normalized source URL matches the retry URL;
- its current fingerprint matches the original expected fingerprint from WorkManager input.

Keep the existing share-import path unchanged: a normal shared URL must still deduplicate against any saved recipe. The worker may reclaim a saved recipe only when the request contains a validated explicit retry target.

For a draft retry, retain the current pending-draft flow. For a saved retry, do **not** write the pending or failed draft representation. The WorkManager input fingerprint, rather than worker memory, remains the authoritative original-snapshot identity across all attempts.

Add a Room `@Transaction` compare-and-set operation for saved retry completion. In one transaction it must:

1. load the current entity by ID;
2. return a conflict/no-op if the row was deleted or its current fingerprint differs from the durable expected fingerprint;
3. validate the normalized source URL and expected failed-import state again; and
4. update the row with extracted content without an insert fallback.

Failure and cancellation perform no saved-row write. An automatic retry must reuse the same expected fingerprint; it must never recapture a newer database row as its baseline. This prevents lost edits, cross-attempt stale overwrite, and deleted-row resurrection without adding a schema migration. The generic repository `upsert` must not be used for saved retry completion.

### 3. Move retry orchestration behind the ViewModel

Affected paths:

- `app/src/main/java/com/instarecipe/app/InstaRecipeViewModel.kt`
- `app/src/main/java/com/instarecipe/app/MainActivity.kt`

Add one ViewModel command for explicit link re-extraction. It should validate eligibility, check configured keys, capture the expected fingerprint, enqueue the targeted WorkManager request, and expose a small per-recipe starting/running/error/conflict state. Observe the deterministic unique-work name—not the newly constructed request ID—because `KEEP` may discard a new request when matching work already exists. Unique-work observation must restore state after process/composition recreation and clear it on terminal outcomes. `MainActivity` should translate only the missing-configuration navigation effect to Settings.

Change explicit retry work to `ExistingWorkPolicy.KEEP`. Combine that with synchronous per-recipe command deduplication in the ViewModel so a second tap cannot cancel a worker after it has claimed a draft or started a saved extraction. Normal first-time share import remains `KEEP` as it is today.

This removes URL validation, API-key checks, WorkManager calls, and retry-set reconciliation from the root composable. Do not introduce a use-case class unless a second non-UI caller appears.

### 4. Wire the same action to both tabs

Affected paths:

- `app/src/main/java/com/instarecipe/app/ui/screens/MainScreen.kt`
- `app/src/main/java/com/instarecipe/app/ui/components/RecipeCard.kt`
- `app/src/main/java/com/instarecipe/app/ui/screens/RecipeDetailScreen.kt`
- `app/src/main/java/com/instarecipe/app/ui/screens/RecipeEditorScreen.kt`

Pass the single retry callback/state to both `LibraryTabScreen` and `InboxTabScreen`. Reuse the existing card footer instead of creating a new menu abstraction. Add a visible secondary action on the read-only recipe detail screen only when the same eligibility helper returns true.

All action targets must remain at least 48 dp, have an explicit accessible label, remain usable with large font scale, and never rely on color alone to communicate failure or progress. Starting/running/error changes should use appropriate `stateDescription` and polite live-region semantics. Verify that the retry control remains separately reachable inside the clickable recipe card rather than being merged into the parent card announcement.

### 5. Remove repeated editor result mapping

Affected paths:

- `app/src/main/java/com/instarecipe/app/MainActivity.kt`
- `app/src/main/java/com/instarecipe/app/ui/screens/RecipeEditorScreen.kt`

Extract one editor-local function that applies `ExtractedRecipeData` to fields, with an explicit notes policy (`Replace` for video/foreground link results, `Append` for pasted text). Relabel the existing foreground editor control to **Refresh draft from link** so it is not confused with the durable background retry offered on list/detail surfaces. This removes the three repeated assignment blocks without adding a domain layer.

Do not otherwise change editor retry behavior in the bounded feature patch. Consolidating all editor extraction behind WorkManager requires an explicit save/discard policy for unsaved fields and belongs in a later product decision.

## Verification plan

### JVM tests

Add focused tests proving:

- a failed draft is eligible;
- a processing draft is not eligible;
- a normal saved recipe is not eligible;
- a saved `Needs review` recipe is eligible;
- normal shared-link import still refuses to replace a saved recipe;
- explicit retry reclaims only the requested saved failed recipe;
- mismatched ID/URL and stale eligibility are rejected without mutation;
- saved retry success replaces extracted fields only when the original snapshot is unchanged;
- saved retry failure/cancellation preserves every persisted field;
- edits or deletion between validation and completion cause a conflict/no-op, never overwrite or reinsertion;
- authorize retry, force WorkManager retry/process restart, then edit the recipe; the later attempt must retain the original input fingerprint and conflict/no-op;
- explicit retry preserves ID, favorite, cooked, and saved date;
- retry text removes only import status and preserves bracketed caption sections;
- a duplicate enqueue after work starts does not replace/cancel the running worker or strand a draft in `Processing`;
- ViewModel behavior for missing credentials, enqueue failure, duplicate commands, unique-work-name observation, process-state restoration, terminal-state clearing, and one-shot navigation effects.

Run `gradlew.bat testDebugUnitTest`.

### Compose/instrumented checks

Add or extend semantics tests proving:

- Imports failed card exposes **Extract from link again**;
- processing cards do not expose it;
- eligible Recipes cards expose it and normal saved cards do not;
- disabled/starting text is announced through state semantics or a polite live region;
- the child retry action remains a separately reachable accessibility node inside the clickable card;
- action remains reachable at large font scale and has a 48 dp touch target.

Compile instrumentation sources with `gradlew.bat compileDebugAndroidTestKotlin`. Run device tests when an emulator/device is available.

### Manual flow

1. Retry a failed item from Imports; observe processing, failure recovery, and success movement to Recipes.
2. Retry an eligible failed linked item from Recipes and confirm its prior content stays visible and intact during failure, cancellation, and conflict outcomes.
3. Retry from the read-only recipe detail surface and confirm the same background behavior; confirm no background retry appears in the writable editor.
4. Repeat with missing keys, offline network, resolver failure, app recreation, and a rapid double tap.
5. Confirm a normal saved recipe cannot be overwritten by sharing or by a stale retry command.

## Acceptance evidence

- All new and existing JVM tests pass.
- Instrumentation sources compile; relevant device tests pass or the device gap is recorded.
- No Room migration is introduced in this pass.
- Normal share deduplication remains unchanged.
- One retry pipeline serves Imports, Recipes, and read-only recipe-detail surfaces.
- Failed/cancelled saved retries preserve every stored field; a durable expected fingerprint prevents stale or restarted workers from overwriting edits or recreating deleted rows.
- Explicit retry uses `KEEP` and duplicate commands cannot strand a draft in `Processing`.
- The retry UI has explicit loading, error, success/movement, and disabled states.
- No unrelated user changes are overwritten.
- Independent review records no blocking correctness, data-loss, concurrency, accessibility, or security findings before the change is marked Verified.

## Deferred work

- Replace tag/note-based import metadata with typed persisted import state only when the product needs history, richer diagnostics, or multiple import providers.
- Consider a persistent user-facing import queue only if WorkManager progress must be inspected beyond the recipe card.
- Replace saved `Recipe` UI snapshots with saved IDs and derive current values from the Room-backed list; keep a separate unsaved editor draft and remove the manual full-object saver afterward.
- Split `MainScreen.kt` into existing Recipes, Imports, and Settings/login boundaries without changing signatures or adding modules.
- Decide whether editor link extraction should save current edits, confirm discard, or remain a foreground draft-only operation before unifying that path with WorkManager.
- Reconsider modules or dependency injection only after the app has multiple feature owners or measurable build/test isolation needs.

## Independent review record

The first reviews found blocking risks around saved-content loss, non-atomic and cross-attempt worker writes, `REPLACE` cancellation, and writable-editor races. This revision resolves those findings in the design by keeping saved rows unchanged until an atomic fingerprint compare-and-set succeeds, carrying the original fingerprint through every WorkManager attempt, observing deterministic unique work, using `KEEP`, and limiting background retry to list/detail surfaces. Implementation still requires its own independent review and verification before it can advance beyond Draft/Reviewed planning status.
