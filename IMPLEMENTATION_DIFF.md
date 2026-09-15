# InstaRecipe: Planned vs Implemented

Last verified: 2026-09-15

## Status

The security, data-integrity, networking, dependency, release-build, Gemini-model, and screen-structure upgrades in this work package are implemented and pass the local automated gates listed below. The screen-extraction, generated-ID, lifecycle-safe local-video, and typed-Gemini-transport follow-ups are **Reviewed** and **Verified**; the wider work package remains **Draft** until it receives an independent whole-package review. No app signing, publishing, deployment, or production mutation was performed.

## Upgrade inventory

| Area | Implemented state |
| --- | --- |
| Gemini | The only active model is `gemini-3.8-flash`. The first encrypted slot is the free primary key and the optional second slot is the paid fallback. Timeout, authentication/permission, quota, model-availability, and service-availability failures may use the paid key. Connection checks run concurrently with a 12-second deadline. Requests, generation responses, recipe payloads, Files API responses, and error envelopes use typed `kotlinx.serialization` DTOs. |
| HTTP/coroutines | OkHttp calls suspend without blocking a dispatcher thread, cancel the underlying call, preserve `CancellationException`, use bounded same-model retries, and expose mapped errors rather than response bodies. Generation and large-file upload clients have separate bounded timeouts. Gemini owns its retry/fallback policy; WorkManager retries only non-Gemini resolver/network failures once. |
| Gemini files | Videos use the Files API instead of inline Base64. Bounded best-effort remote deletion is attempted after success, failure, or coroutine cancellation in a non-cancellable cleanup block. |
| Instagram privacy | Raw Instagram cookie/session access and the in-app WebView login flow were removed. There are no bundled community resolver endpoints. |
| Resolver security | Custom resolvers must be HTTPS. Local, loopback, link-local, site-local, multicast, and IPv6 ULA destinations are rejected. Redirects are followed manually and revalidated on each hop. |
| Video safety | Incoming shared videos must be `content://` URIs. Video MIME type, available storage, streaming size, and a 200 MB maximum are enforced; partial temporary files are deleted. |
| Share intents | Externally shared text/video requires user confirmation before network or AI processing. Accepted Reel links enqueue unique, network-constrained WorkManager extraction. A processing draft is created or reclaimed, successful extraction replaces it and enters the Saved cookbook, and a failed draft can be re-shared to retry. Accepted video shares transfer synchronously to ViewModel ownership before the intent is cleared. Accepted intents are cleared to prevent replay, and text imports require a valid HTTPS Instagram URL. |
| Room | Database v2 uses generated IDs, an explicit v1-to-v2 migration, a unique nullable normalized source URL, conflict-safe inserts, update-by-ID, exported schemas, and a migration instrumentation test. |
| Architecture | `Recipe` models were extracted, storage is behind `RecipeStore`, and the ViewModel receives its repository through a factory instead of fetching a singleton internally. `MainActivity` now owns lifecycle/share orchestration while main, detail, and editor UI live in dedicated screen files. Gallery/shared-video extraction and its progress/error/completion state are owned by the ViewModel, survive Activity recreation, suppress duplicate starts, and retain the persisted result until UI acknowledgement. Persisted recipes propagate their generated Room IDs before editor/detail state is updated. Important recipe/cooking UI state is saveable. |
| Secrets | Primary and optional backup Gemini keys use Android Keystore-backed encryption and are committed atomically. Plaintext legacy data is removed only after an encrypted round-trip succeeds, and settings report persistence failures. No committed key/session patterns were found in the closing scan. |
| Accessibility/UX | Ingredient rows expose one checkbox role with stable checklist identity. Recipe cards open directly instead of flipping. Search is integrated into Cookbook, Add recipe presents video/manual choices, navigation has three destinations, and infrastructure controls are collapsed under Advanced settings. Serving scaling handles decimals, mixed fractions, Unicode fractions, and ranges. Timer formatting observes configuration locale changes. |
| Release | Release shrinking, resource shrinking, and R8 obfuscation are enabled. Backup and cleartext traffic remain disabled. The release output is unsigned. |
| Toolchain | Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, KSP 2.3.12, compile/target API 37, Compose BOM 2026.09, Lifecycle 2.11, WorkManager 2.11.2, Room 2.8.5, OkHttp 5.5, coroutines 1.11, and kotlinx.serialization 1.11. |
| Supply chain | Versions are centralized in `gradle/libs.versions.toml`; dependency locks, SHA-256 verification metadata, and the official Gradle 9.7.1 distribution and wrapper-JAR checksums are committed. CI validates the wrapper and runs tests/lint/debug/release builds; Dependabot covers Gradle and Actions. |

## Original implementation plan comparison

| Planned item | Result | Difference / rationale |
| --- | --- | --- |
| Extract `MainScreen.kt`, `RecipeEditorScreen.kt`, and `RecipeDetailScreen.kt` from `MainActivity.kt` | **Implemented** | The scaffold/library/inbox/settings, detail, and editor composables now live in dedicated `ui/screens` files. `MainActivity.kt` is about 670 lines and retains lifecycle, share-intent, root-state, and root-composition responsibilities. |
| Reduce OkHttp timeouts | **Implemented with adjustment** | Generation is bounded at 75 seconds total, key diagnostics at 12 seconds, and file upload at 5 minutes because videos cannot reliably use a blanket short limit. With two keys configured, each key receives one generation attempt so the paid fallback is not delayed by repeated free-key attempts. |
| Preserve coroutine cancellation | **Implemented** | Network helpers, resolver/worker paths, and UI extraction launches explicitly rethrow cancellation. Best-effort remote cleanup intentionally runs non-cancellably. |
| Replace manual `org.json` parsing with `kotlinx.serialization` | **Implemented** | Gemini request/response, recipe, file-upload/status, and error envelopes are typed. The API transport is injectable and covered by MockWebServer fixtures for success, malformed JSON, safe HTTP mapping, retry, cancellation, upload/polling, and remote cleanup. |
| Add Gradle version catalog | **Implemented** | Plugins and libraries are referenced through `libs.versions.toml`. |
| Upgrade outdated dependencies | **Implemented** | Current stable versions listed above were selected as of the verification date and compiled together. |
| Add dependency injection | **Implemented without framework** | Constructor/factory injection removes the ViewModel singleton lookup. Hilt/Koin was not added because the app currently has one repository graph and does not need framework overhead. |
| Build/test/lint verification | **Implemented** | See exact evidence below. |
| Manual device verification | **Pending** | No connected emulator/device was available, so share-sheet flows, live Instagram/Gemini behavior, process recreation, dark/large-font screenshots, and the Room migration test were not executed on Android. |

## Remaining known gaps

1. Run the Room migration and Compose accessibility instrumentation suites on API 26 and API 37 devices/emulators, then manually exercise text/video shares and live Gemini 3.8 extraction.
2. Move remaining user-facing hard-coded strings into resources and run pseudolocale, RTL, TalkBack, and 200% font-scale visual checks.
3. Review the 30 non-blocking lint warnings (primarily unused legacy color resources, KTX suggestions, and storage-allocation guidance). Lint has zero errors.
4. Run a dedicated dependency vulnerability scanner in CI or locally. OSV-Scanner, Trivy, and Grype were not installed on this machine; locks/checksums and Dependabot improve supply-chain control but are not a CVE scan.
5. Obtain an independent whole-package code/security review. Automated checks and slice reviews do not satisfy the remaining whole-package governance requirement.

## Verification evidence

Executed locally on 2026-09-14 with the checksum-verified Gradle 9.7.1 distribution, JDK 17, Android Platform 37, and Build Tools 37.0.0:

```powershell
gradle.bat testDebugUnitTest :app:compileDebugAndroidTestKotlin lintDebug assembleDebug assembleRelease
gradle.bat :app:dependencies --write-locks
```

Results:

- 13 unit tests passed; 0 failures, 0 errors, 0 skipped.
- Android instrumentation sources compiled, including the Room v1-to-v2 migration and recipe-card accessibility tests.
- Lint passed with 0 errors and 30 warnings.
- Debug APK built successfully.
- Minified/resource-shrunk unsigned release APK built successfully; R8 mapping output was generated.
- `git diff --check` passed.
- Active source/docs scan found no Gemini 1.x/2.x model IDs, model-picker constants, Instagram session manager references, WebView/cookie code, blocking OkHttp `execute()` calls, private-key markers, Gemini-key-shaped literals, or `sessionid=` literals. One historical changelog sentence may describe older behavior explicitly as historical.

Artifacts and reports are under `app/build/` and are intentionally not committed. The release APK is not installable as a production release until the owner separately authorizes and supplies signing/release configuration.

## Follow-up verification: screen extraction and generated IDs

Executed locally on 2026-09-14:

```powershell
.\gradlew.bat wrapper --gradle-version 9.7.1 --distribution-type bin
.\gradlew.bat testDebugUnitTest :app:compileDebugAndroidTestKotlin lintDebug
git diff --check
```

Results:

- The Gradle 9.7.1 wrapper distribution and bootstrap JAR match Gradle's published SHA-256 checksums.
- 14 unit tests passed, including a regression test proving generated recipe IDs and normalized tags are propagated after persistence.
- Android instrumentation sources compiled.
- Lint passed with 0 errors and 30 warnings.
- `git diff --check` passed.
- An independent reviewer confirmed the generated-ID blocker was resolved and recorded no remaining blocking findings in this follow-up slice. The regression test uses a fake `RecipeStore`; full DAO-to-UI behavior remains covered only by compilation and manual-review evidence until device tests run.

## Follow-up verification: lifecycle-safe local-video extraction

Executed locally on 2026-09-14:

```powershell
.\gradlew.bat testDebugUnitTest :app:compileDebugAndroidTestKotlin lintDebug
git diff --check
```

Results:

- 17 unit tests passed, including completion retention/acknowledgement, duplicate-start suppression, and error-state regression tests for the ViewModel-owned extraction controller.
- Android instrumentation sources compiled.
- Lint passed with 0 errors and 30 warnings.
- `git diff --check` passed.
- An independent reviewer recorded no blocking findings for Activity recreation ownership, synchronous accepted-video handoff, cancellation propagation, non-cancellable cleanup, off-main file copying, state delivery, or generated Room identity.
- Device-level Activity recreation, cancellation/cleanup, provider-specific URI behavior, and live Gemini upload remain pending because no emulator/device or live credentials were used. Providers that return a missing or generic MIME type may still be rejected by the existing strict video validation.


## Follow-up verification: typed Gemini transport

Executed locally on 2026-09-14:

```powershell
.\gradlew.bat :app:testDebugUnitTest --write-locks --write-verification-metadata sha256
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:lintDebug
git diff --check
```

Results:

- `org.json` was removed from the Gemini extraction path and replaced with typed kotlinx.serialization 1.11 DTOs for generation, recipe, error, upload, and status payloads.
- 26 unit tests passed. Nine MockWebServer tests cover typed success payloads, malformed JSON, safe HTTP error mapping without upstream-message leakage, retry, text and post-upload cancellation, video upload/polling/use/deletion, missing-upload-URI cleanup, and cleanup after a malformed generation response.
- Android instrumentation sources compiled.
- Lint passed with 0 errors and 30 warnings.
- Dependency locks and SHA-256 verification metadata include the new serialization plugin/runtime artifacts.
- `git diff --check` passed.
- An independent reviewer found two cleanup/cancellation edge cases; both were repaired and covered by regression fixtures. The reviewer then recorded no blocking findings. This follow-up is **Reviewed**, **Verified**, and **Accepted**. Live Gemini and device behavior remain pending and were not inferred from JVM fixtures.

## Follow-up verification: background Reel completion and backup Gemini key

Executed locally on 2026-09-14:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:lintDebug
git diff --check
```

Results:

- Accepted text shares enqueue unique, network-constrained WorkManager extraction after URL validation and confirmation.
- Successful background extraction now replaces the processing draft with a `Saved` recipe, so it leaves Inbox and appears in the cookbook.
- Failed drafts can be reclaimed and queued again when the same link is re-shared; already-saved recipes remain deduplicated.
- A primary and optional backup Gemini key are stored encrypted. Backup failover is limited to authentication/permission, quota, model-availability, and service-availability failures; invalid content and cancellation do not consume the backup key.
- 33 unit tests passed, including five background-state/requeue tests and two key-fallback MockWebServer tests.
- Android instrumentation sources compiled. Lint passed with zero errors; warning count is recorded after the final review run.
- This follow-up is **Verified** and remains **Draft** pending independent review. Live Instagram/Gemini and device-level WorkManager behavior remain pending.

## Follow-up verification: free-primary/paid-fallback and consumer shell

Executed locally on 2026-09-15:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
git diff --check
```

Results:

- The first encrypted key is explicitly presented and used as the free primary; the second is the optional paid fallback.
- Key diagnostics run concurrently with a 12-second deadline. Generation and Files API timeouts, authentication/permission rejection, quota exhaustion, model unavailability, and Gemini service failures can hand off to the paid key.
- When two keys are configured, each gets one generation attempt. Gemini failures do not receive an additional WorkManager retry; non-Gemini resolver/network failures retain one background retry.
- Cookbook search is integrated into the primary screen, bottom navigation is reduced to Cookbook/Imports/Settings, Add recipe uses a focused choice sheet, recipe-card taps open details directly, and technical controls are collapsed under Advanced settings.
- 35 JVM unit tests passed with zero failures, including generation-timeout and video-upload-timeout free-to-paid fallback regressions.
- Lint passed with zero errors. The debug APK assembled successfully.
- `git diff --check` passed; line-ending notices for the pre-existing mixed working tree remain non-blocking.
- An independent reviewer found two blocking retry/fallback gaps. Both were repaired and re-reviewed with no blocking findings. This bounded follow-up is **Reviewed**, **Verified**, and **Accepted**.
- Live-key, device, share-sheet, and visual screenshot verification remain pending because no emulator/device or production credentials were used.
