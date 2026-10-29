# Draft quick approval — 28 October 2026

## Behavior

The owner requested fast approval directly from a draft. Complete drafts in Imports now show **Approve recipe** and a separate **Review / edit** action. Approval promotes the same stored recipe to Recipes without opening the editor or changing tabs. A polite status message confirms persistence. Missing/placeholder title, ingredients or steps, partial-extraction markers, failed imports and active processing prevent quick approval; existing recovery/review actions remain.

Approval disables repeat taps while pending. A Room transaction verifies that the stored row still matches the displayed recipe, then updates that row. A changed, already-approved or deleted draft is declined without overwriting edits or recreating data. The row keeps its ID, source, notes, saved date, personal photo, favorite/cooked state and other metadata; review workflow tags are removed. Generic failure feedback offers review/retry without displaying database errors. No Gemini request or network access is required to approve a complete draft, including when no key is configured.

Affected native files: RecipePresentation.kt, RecipeDatabase.kt, InstaRecipeViewModel.kt, MainActivity.kt, MainScreen.kt, ImportsScreen.kt and ImportCard.kt. Tests: QuickApprovalTest.kt and QuickApprovalPersistenceTest.kt. No dependency, schema, build/signing configuration, version or palette changes.

The library prototype (local evidence retained outside this checkpoint) also demonstrates a direct Approve control. Its unified Recipes/Drafts navigation remains a proposal; this APK implements quick approval in the existing Imports tab. Prototype screenshot (local evidence retained outside this checkpoint) is browser output, not a native screenshot.

## Verification

- `.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --max-workers=2 --console=plain` succeeded (1m 29s).
- **105 unit tests in 15 suites passed**, zero failures, errors or skips. Three new JVM tests cover complete promotion/field preservation, invalid content and partial/processing rejection.
- Three Room regression tests compiled and were packaged in the instrumentation APK: same-row promotion/repeated approval, changed/deleted draft rejection, and partial-draft rejection. They **were not executed**; no emulator or physical-device use, per owner preference. Compose runtime behavior, lifecycle interruption, TalkBack and font-scale acceptance remain for the device.
- Debug APK verified with Android SDK 37.0.0 `apksigner verify --verbose --print-certs`. Certificate SHA-256: `b791063c2e0cc3313d5258d6cba6668d67b889d7a96ff91c4dc83a340300b611`.
- Artifact: `app/build/outputs/apk/debug/InstaRecipe-2026-10-28-debug.apk`, **21,756,027 bytes**. SHA-256: `23ce254f5231b56087045505bbed564150ae385c85deb9cfaa853c6bb7aa5014`.
- `git diff --check` passed. Existing Compose test-rule deprecation warning remains; no toolchain change was made.
- Browser prototype: Drafts showed Approve only for the complete Kadhai mushrooms sample; approving kept Drafts selected, removed that draft, updated Ready 3→4 / Drafts 3→2 and announced success. Partial samples had no Approve control. No browser console errors reported. This is simulated data, not evidence of native persistence.

## Device acceptance and rollback

Install the dated debug APK as an update, keeping app data. In Imports, approve a complete draft and confirm it appears once in Recipes with its details/photo retained. Confirm incomplete and processing drafts require review, repeated taps do not duplicate rows, and Review / edit still opens the editor. The owner will perform these checks on their device; no installation or data clearing was performed here.

Rollback only the quick-approval hunks and its new tests/documentation; keep the earlier search/screenshot/comment/photo changes and owner edits. Existing Room rows are compatible. Approved rows remain normal Saved recipes under the preceding APK; there is no one-tap Undo in this feature and no migration or database reset is needed.
