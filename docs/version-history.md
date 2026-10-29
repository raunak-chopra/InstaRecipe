# InstaRecipe behavior history

## 29 October 2026 — isolated source preservation

Preserved the 27–28 October search, screenshot/comment import, quick-approval implementation and tests on a separate feature branch based on e3abb8b. Source behavior is unchanged from the reviewed owner working snapshot. Nine retired decorative assets are byte-identical Git renames to the documentation archive.

Files: reviewed app source/manifest/tests, ASSET_PROVENANCE.md, DESIGN_SYSTEM.md, README.md and portable behavior/status notes. Removed an inherited machine-specific archive path and labelled excluded local design/build evidence links in this isolated copy. Original source/staging, private SDK configuration, local prototypes, screenshots and APKs remain outside this checkpoint.

Reason: preserve current work remotely without interfering with the active checkout or implying acceptance/merge approval. Rollback: revert this checkpoint on a feature branch, restoring resource references/assets together; keep personal data and original work untouched. Device/live API validation remains separate. Fresh maintenance validation is recorded in the preservation PR.

## 28 October 2026 — approve complete drafts directly

Complete drafts now offer **Approve recipe** in Imports, alongside Review / edit. Approval promotes the existing row without opening the editor or changing tabs and announces success after persistence. Incomplete, partial, failed or processing imports cannot bypass review. A transaction declines stale/deleted/already-approved rows, preserving edits and personal fields and avoiding duplicate creation; buttons disable during approval.

Files: RecipePresentation.kt, RecipeDatabase.kt, InstaRecipeViewModel.kt, MainActivity.kt, MainScreen.kt, ImportsScreen.kt and ImportCard.kt; QuickApprovalTest.kt and QuickApprovalPersistenceTest.kt. The browser library proposal also includes direct approval. Wider library/navigation changes remain proposed.

Verification: **105 unit tests passed**, debug APK and instrumentation APK built, app signature verified. Room device tests compiled but were not run; no emulator/device or live API use. Artifact identity, device checklist and scoped rollback are in [quick-approval evidence](quick-approval-2026-10-28.md). No schema, dependency, build/signing configuration, version or colour changes.

## 27 October 2026 — search recovery, screenshots, comments and image removal

The owner reported search failing and requested screenshot import, automatic recipe parsing from Reel comments, and removal of generated/decorative photos while retaining personal photos. Search now matches the loaded library's actual content and no longer waits for or depends on FTS IDs. Screenshot picker/share input uses bounded decoding and the existing Gemini client; an existing draft can also be completed from a screenshot. Comments exposed in authenticated media/public page metadata are automatically considered, scoped to the requested Reel, with creator priority and source/community labels. Unavailable/restricted comments remain a documented limitation with screenshot/text recovery. Nine decorative images were archived outside app resources; personal-photo/data fields are untouched.

Files: RecipeSearch.kt, RecipeLibraryScreen.kt, MainScreen.kt, MainActivity.kt, AndroidManifest.xml, AddRecipeSheet.kt, RecipeEditorScreen.kt, RecipeScreenshot.kt, GeminiApiClient.kt, GeminiRecipeExtractor.kt, InstaRecipeViewModel.kt, InstagramResolver.kt, InstagramRecipeComments.kt, RecipePresentation.kt and legacy EditorialChapters.kt; three unit-test files, retired assets and documentation. No dependencies, build metadata, schema, palette, signing or version changes.

Verification: debug APK built, signature verified; **102 Gradle unit tests passed** with no failures/errors/skips. Decorative assets are absent from the APK and archived originals match their original Git hashes. No emulator, physical-device or live API checks were performed. Artifact/hash, limitations and rollback from baseline `e3abb8b` are in [feature evidence](features-2026-10-27.md). Revert only this change's reviewed hunks/new helper files and restore archived resource references/assets together; preserve personal photos and owner edits, and never reset/clear app data as rollback.

## 22 October 2026 — checksum repair and debug APK

The owner explicitly authorized fixing the missing checksum and building the APK, superseding the earlier metadata restriction. Three missing existing transitive POM entries were individually checked against fresh Maven Central bytes and published SHA-1 values, then recorded as SHA-256 entries in `gradle/verification-metadata.xml`: Guava parent 33.4.0-jre, coroutines BOM 1.10.2 and Guava parent 33.2.1-jre. No dependencies were added/upgraded; strict verification was retained.

`assembleDebug` succeeded, all **94 Gradle unit tests passed**, instrumentation source compiled and the test APK packaged. The app APK signature verified. Artifact identity, checksum evidence and current device coverage are recorded in build evidence (local evidence retained outside this checkpoint). No source compile fixes, signing/configuration changes, app-data clearing, commits, push or publication were needed for this build.

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

## 2026-10-22 — Restore Linux CI artifact verification

- Behavior: trust the exact Linux AAPT2 9.4.0-15978811 artifact used by existing Ubuntu CI alongside its already-pinned Windows artifact.
- Files: gradle/verification-metadata.xml and this history entry.
- Reason: confirmed CI failed on missing platform-specific verification metadata. Google Maven's published SHA-1 matched the downloaded artifact before SHA-256 f5bebd466ecf14d341fd465f2756a16d86052f29eb4532003d5ff7bcffd08de5 was recorded.
- Verification: XML parses; strict verification and dependency versions remain unchanged. The Linux CI rerun is the acceptance check.
- Rollback: revert this checksum/history commit; do not disable dependency verification.
