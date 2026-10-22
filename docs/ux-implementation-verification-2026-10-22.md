# UX implementation verification — 22 October 2026

## Verdict

**The redesigned debug APK built successfully and all 94 Gradle unit tests passed.** After the owner explicitly approved fixing checksums/building the APK, three missing transitive POM checksums were individually verified and added. Strict verification remains enabled; palette values, dependency versions, build/signing configuration and Room schema were retained. The APK signature verified and instrumentation tests compiled/packaged. See [current APK/build evidence](build-apk-2026-10-22.md) for artifact/hash and device results. This is not release readiness or full UX acceptance.

## Checks and evidence

| Check | Result | Scope and limits |
|---|---|---|
| Initial `.\gradlew.bat assembleDebug --console=plain` | Initially blocked; later resolved | The original missing Guava parent-POM checksum was left unchanged until the owner explicitly authorized repair. Two further missing transitive POM checksums surfaced during processing and were individually verified. Earlier report: `build/reports/dependency-verification/at-1792645914956/dependency-verification-report.html`. |
| Final `assembleDebug --console=plain` | **Passed, 5m 5s** | Kotlin/Compose compile, Room/KSP, dex and package succeeded with strict verification. APK signature verified with apksigner (exit 0). |
| Standalone Kotlin PSI parse | **55 files; zero syntax errors** | All main/unit/instrumentation `.kt` files, after final source edits. Local cached Kotlin 2.0 parser, CRLF normalized only in parser memory. This checks syntax, **not types, Compose compiler behavior, resources, Android integration or the configured Kotlin 2.4.20 build**. |
| Standalone JVM regression tests | **30 passed** | Actual `CookingSession`, `Recipe`, `RecipeQuality`, `RecipePresentation`, `RecipeSearch`, `InstagramImportState` and `TagNormalizer` sources; actual verbatim slices for source normalization/FTS and serving/cooking helpers. JUnit 4.13.2. Compose's `Immutable` marker was the only annotation stub. No fake business logic. |
| Test classes run in standalone harness | Passed | `CookingTimerStateTest`, `RecipePresentationTest`, `RecipeSearchTest`, `TagNormalizerTest`, `ServingScalerTest`. Timer deadline/suspension/pause/resume/reboot, truthful imports, quality gate, FTS query escaping, filters/diet, tag normalization and existing serving helpers. |
| Gradle unit tests / partial JSON parser tests | **94 passed; zero failures/errors/skips** | 13 XML suites from `testDebugUnitTest`, including all 3 partial JSON parser tests and the existing network/state/persistence tests. |
| Compose/instrumentation compilation | **Passed** | `assembleDebugAndroidTest` compiled and packaged updated import-state/200% font and draft-save/dirty-exit tests. Existing Compose rule deprecation warnings did not fail compilation. Device execution is recorded separately. |
| Emulator/device execution | **Not a completed pass; stopped at owner request** | APK installed; MainActivity returned `Status: ok` on retry. Emulator startup showed Launcher/System UI ANRs. An instrumentation attempt returned `Process crashed` before completion; its cause is undiagnosed. The owner will test on a physical device. Disposable emulator closed without saving a snapshot. |
| Native after screenshots | No clean acceptance set | Captured emulator startup warnings are labelled as such. Existing 16 October screenshots remain baseline evidence only. No physical device was touched. |
| Palette/dependencies/schema diff | Retained; metadata repair is explicit | The earlier no-diff check passed for Color.kt, Theme.kt, build/settings files, schemas and RecipeDatabase.kt. Subsequently only three approved, verified checksum entries were added under `gradle/verification-metadata.xml`; dependency versions did not change. |
| Whitespace diff check | **Passed; exit 0** | `git diff --check`. Git reported only line-ending conversion notices, no whitespace errors. |
| Existing serving-scaler work | **Snapshot hashes match** | Current ServingScaler.kt and ServingScalerTest.kt match their pre-implementation snapshots byte-for-byte (SHA-256). |
| Independent fresh-context review | Pending | Self-review does not satisfy the repository's separate Tier 2 review requirement. No push, PR, release or publication is authorized or performed. |

The supplemental harness is retained locally in ignored `scratch/verify_ux_sources.py`; it verifies cached compiler/runtime JAR bytes against their cache-directory SHA-1 before using them. It does not invoke Gradle, assemble Android, resolve new dependencies, or weaken project verification. Its source slices come directly from current project files. Use the configured Gradle tasks for authoritative build/test results when the blocker is resolved.

## Self-review fixes

- Preserve the eight pre-existing modified source/test files; initial source snapshot remains in ignored `scratch/ux-before-2026-10-22/src`. ServingScaler source/tests were not rewritten by this revision.
- Duplicate links consult persisted rows. Already-saved recipes open detail; unfinished rows open review, while a running import remains in Imports.
- A completed text/video import waits while another editor/detail is open. An incoming link share waits while the editor owns unsaved work.
- Review saves await persistence and retain edits on failure. Back/Cancel guards unsaved fields. Link refresh runs in the editor's coroutine scope and cancels on exit.
- Applying extracted details keeps existing notes alongside new notes. Promotion filters workflow tags before the user-tag cap, so those markers do not displace personal tags.
- Empty linked drafts show source recovery before the essential fields. Errors have explicit severity and scroll into view. Creator/source fields no longer use vertical weights inside an unbounded list.
- Partial extraction keeps meaningful fragments and names missing details in notes. Its marker is prioritized before the existing per-recipe tag cap. Final cookbook save still enforces title/ingredient/step quality.
- The library enumerates all distinct tags across recipes rather than applying a per-recipe 12-tag cap to the whole library. Search errors offer retry or return to all recipes.
- Cooked toggling offers Undo in detail. Photos remain optional and accessible; completing cooking does not force the chooser. Owner-modified photo storage callbacks remain.
- Cooking persistence saves step, scale, checklist and elapsed-time timer deadline. Finishing prevents a pending save from restoring a cleared session. Timer sound has a separate lifecycle from ticking so resetting durable timer state does not immediately cancel the sound.

## Remaining acceptance work

1. **Done:** owner-approved verification repair; configured Android compile/APK and all 94 unit tests passed. Strict verification stays enabled.
2. Complete device execution of Compose tests and native inspection; lint remains outside the checks run so far. Instrumentation compilation already passed. Fix any demonstrated runtime failures before expanding acceptance claims.
3. Obtain the required independent review. Inspect native layouts at 320dp, normal phone, short landscape, tablet and 200% text in both themes. Exercise TalkBack/focus, IME, gesture and three-button insets, navigation restoration and last-row reachability.
4. Exercise link/text/video/manual import, no key, failed/partial/duplicate/background import, draft save/failure/exit, search/filter recovery, photos, cooking resume/timer/recreation and finish. Use fixtures/fakes first; no paid/live Gemini calls were made for this work.
5. Capture equivalent native before/after screenshots and measure actual task completion; the audit's journey improvements remain design hypotheses.

One active cooking session is supported. Timers alert in cooking mode; there is no background alarm/notification service. Reboot resets a running timer. No backup/export feature or database migration was introduced. Dormant legacy private card functions remain in RecipeCard.kt to avoid broadly removing owner-modified code; the active composition uses CompactRecipeSummary.
