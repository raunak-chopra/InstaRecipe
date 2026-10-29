# InstaRecipe — Kitchen Journal design system

**State:** Approved layout revision built as a debug APK; unit tests passed; full device/accessibility inspection and independent review pending
**Updated:** 2026-10-22

**27 October update:** Search reads actual loaded recipe content. Add/Share and draft recovery accept recipe screenshots through the existing Gemini connection. Reel import checks available recipe comments automatically, preserving attribution and uncertainty. Generated decorative assets were retired from app resources; personal cook photos remain. See [current implementation evidence](docs/features-2026-10-27.md). Palette values and fonts are unchanged.

## Product idea

InstaRecipe is a personal cookbook made from recipes people find in their social feed. The
experience is a contemporary kitchen journal: warm, editorial, vivid when it matters, and calm
when someone is cooking. The working brand system is **Kitchen Journal + Save-to-Stove + Practical
Index**, expressed in the shorthand **Saved. Sorted. Cooked.**

The product journey is expressed as **Saved → Sorted → Ready → Cooked → Kept**. “Kept” is the
emotional end state; “Sorted” is the practical promise that turns a saved post into usable cookbook
content.

The implementation has three primary destinations: **Recipes / Imports / Settings**. Recipes is
the single searchable saved library; Imports owns sources and unfinished work. Legacy destination
identities remain only to restore older activity state and redirect to Recipes. InstaRecipe remains
a personal local-first app using the owner's Gemini key. No palette, font, dependency or Room
schema change belongs to this revision. The selected design uses A's recipe-first hierarchy and
C's compact row treatment from the approved concepts (local evidence retained outside this checkpoint).

## Visual direction

- **Base:** warm paper, generous margins, thin rules, restrained shapes, and a clear reading measure.
- **Character:** platform serif for display recipe/feature titles; licensed Elvara Sans for UI,
  recipe steps, metadata, and controls.
- **Practicality:** index-like labels, chapter tabs, source strips, clear metadata, numbered steps,
  and status cues that make the next cooking action obvious.
- **Vibrancy:** one intentional tomato/leaf/butter move at a time. Standard recipe cards stay
  paper-led and title-led. The library has no decorative hero or collection-cover grid.
- **Cooking:** plain sans, large type, screen-awake behavior, visible progress, timers, and no
  promotional or decorative modules in the active step flow.
- **Personal evidence:** a finished cook can be photographed with the camera or chosen from the
  device. The image is copied into app-private storage, labelled as the user's own cook, and never
  presented as source or editorial photography.
- **Boundaries:** no gradients, glass effects, fake gold, heavy shadows, all-caps body copy, or
  novelty fonts in practical content.

## Verbal identity

- **Descriptor:** A kitchen journal for recipes worth making.
- **Primary action:** Save a recipe.
- **Ready state:** Ready to cook.
- **Voice:** warm, concise, editorial, and quietly capable—practical but not clinical; personal but
  not sentimental.

## Semantic colour roles

| Role | Token | Hex | Usage |
| --- | --- | --- | --- |
| Primary paper | `SurfaceCanvas` | `#F5E9D7` | Default app canvas |
| Raised paper | `SurfaceRaised` | `#FFF8EF` | Recipe panels and input surfaces |
| Ink | `SurfaceInk` / `TextPrimary` | `#241D17` | Reading text and dark editorial surface |
| Supporting text | `TextSecondary` | `#62584D` | Metadata and captions |
| Deep tomato | `AccentTomatoDeep` | `#B53C24` | Actions, links, selected emphasis |
| Bright tomato | `AccentTomatoBright` | `#E65A32` | Large poster graphics and artwork |
| Leaf | `AccentLeaf` | `#31583C` | Quiet secondary actions and produce cues |
| Butter | `AccentButter` | `#F2BD45` | Highlight and collection edge only |
| Berry | `AccentBerry` | `#9F3151` | Limited seasonal accent only |
| Rule | `BorderSoft` | `#D8C8B3` | Hairline boundaries |
| Error | `FeedbackError` | `#A52E2A` | Explicit error state |
| Success | `FeedbackSuccess` | `#2F6A48` | Explicit confirmation state |

The Compose theme maps these roles to light/dark schemes. Small text uses the deeper roles; bright
tomato and butter are not used as small body or action text. A local contrast check on 2026-09-16
measured at least 4.82:1 for the lowest checked text/action pair and higher for the remaining
primary, metadata, error, and success pairs on the light surfaces.

## Typography and shape

- Display hierarchy uses the platform serif as the implemented editorial face; feature/collection
  typography remains selective rather than becoming a novelty UI font.
- Elvara Sans is the licensed interface/body family.
- Use existing Material typography roles: headlineSmall for screen/detail headings, titleLarge
  for recipe rows, and the existing recipe reading scale for ingredients and instructions.
  Cooking instructions retain 21sp type with 32sp line height and follow user font scaling.
- Recipe/import cards use `AppRadii.card` (12dp). Existing surface/control tokens remain 4dp/12dp. Controls use at least 48dp touch targets.
- Elevation is zero by default; rules carry most of the hierarchy.

## Motion

`MotionMode` is persistent in local preferences and appears under Settings → Appearance:

- **Standard** — gentle page and recipe transitions.
- **Reduced** — less movement with essential feedback only.
- **Off** — all app animations disabled; state changes remain immediate and readable.

When no app choice exists, the OS animation setting supplies the initial Standard/Reduced default.
The app never silently changes an explicit Off selection. Cooking steps, ingredient disclosure,
card feedback, and tab changes honor the selected mode.

## Accessibility guardrails

- Interactive controls expose labels/state and use 48dp minimum targets.
- Recipe cards keep the whole-card open action separate from favorite, cooked, cook-now, and retry
  controls so keyboard and screen-reader users can reach each action.
- Checked ingredients use both a checkbox mark and text decoration/state.
- Retry/loading/error messages use explicit copy and polite live-region semantics where state changes.
- Text should reflow at 200% font scale; native inspection of this revision is still pending. No essential recipe state is intentionally color-only.
- Show a labelled private cook photo when available. Do not reserve a decorative image placeholder in recipe detail.

## Product and content guardrails

Recipe claims, dietary/allergen/nutrition/safety/storage guidance, substitutions, source attribution,
and photography rights remain evidence-backed. The app preserves the existing extraction warning and
does not infer these facts from a title or visual placeholder.

Editorial Home and Collection imagery is tracked in `ASSET_PROVENANCE.md`. Per-recipe cards do not
reserve a large image area when no verified photo exists. A user's own cook photo may appear as a
clearly labelled personal image; imported recipe imagery must still carry its own source,
attribution, and usage-right record before it is shown as recipe photography.

## Interaction contracts in the approved revision

- **Recipes:** visible search; All, Favorites, To cook and Cooked views; category/tag/diet/quick
  filters and sorting in a sheet. Empty, no-results and failed-search states provide recovery.
- **Add:** Instagram link, recipe text, local video and manual recipe paths use existing services.
  Missing-key text and link sources can remain drafts; manual entry needs no key.
- **Imports:** Working disables duplicate primary action. Connection offers Settings and manual
  editing. Source and partial states explain what is missing. Only review-ready content shows
  ingredient/step counts. A draft opens its editor directly, rather than through detail.
- **Review:** title, ingredients and method are primary. Optional metadata follows; recovery
  tools appear first for empty linked drafts. Save to recipes enforces recipe quality. Save draft
  accepts unfinished content, waits for persistence and retains fields after failure. Back/Cancel
  guard unsaved edits. Editor extraction cancels on exit; background import completion waits
  instead of replacing an open editor.
- **Detail:** compact recipe title, optional cook photo, section jumps, wrapping actions and a
  sticky cook control. Delete lives in the menu with confirmation. Cooked toggling offers Undo.
  Incomplete/unreviewed recipes cannot start cooking.
- **Cooking:** retain step navigation, ingredient checklist, scaling, screen-awake and volume-key
  controls. One active session restores on return. Deadline-based timing survives suspension and
  process recreation within the same boot. Timer sound requires cooking mode to remain open;
  background notifications/alarms and multiple simultaneous sessions are outside this revision.
- **Completion:** mark cooked without forcing a photo chooser. Add/replace/remove personal cook
  photos remain explicit recipe actions.

Foreground/container pairs must come from the same existing semantic scheme. Never put a fixed
light container under a dark-scheme foreground. Colour values in Color.kt and mappings in Theme.kt
remain unchanged. Token contrast calculations are in the concept evidence; full runtime contrast,
TalkBack, focus, IME/inset and adaptive-layout checks remain [pending](docs/ux-implementation-verification-2026-10-22.md).
