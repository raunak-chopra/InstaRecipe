# InstaRecipe — Kitchen Journal design system

**State:** Draft — implemented in the current UI, pending independent visual and device review
**Updated:** 2026-09-16

## Product idea

InstaRecipe is a personal cookbook made from recipes people find in their social feed. The
experience is a contemporary kitchen journal: warm, editorial, vivid when it matters, and calm
when someone is cooking. The working brand system is **Kitchen Journal + Save-to-Stove + Practical
Index**, expressed in the shorthand **Saved. Sorted. Cooked.**

The product journey is expressed as **Saved → Sorted → Ready → Cooked → Kept**. “Kept” is the
emotional end state; “Sorted” is the practical promise that turns a saved post into usable cookbook
content.

The implementation uses four primary destinations—Shelf, Collections, Saved, and You—with Imports
retained as a focused utility route during recipe creation. Shelf is a personal cookbook index:
named food categories appear as chapters, while personal statuses such as `Saved to try`
stay text-led rather than borrowing unrelated imagery. It remains local-first and supports
Instagram/video import, recipe review, saving, search, source-backed recipe facts, serving scaling,
checklists, cooking mode, timers, and private cook-photo capture after completion.

## Visual direction

- **Base:** warm paper, generous margins, thin rules, restrained shapes, and a clear reading measure.
- **Character:** platform serif for display recipe/feature titles; licensed Elvara Sans for UI,
  recipe steps, metadata, and controls.
- **Practicality:** index-like labels, chapter tabs, source strips, clear metadata, numbered steps,
  and status cues that make the next cooking action obvious.
- **Vibrancy:** one intentional tomato/leaf/butter move at a time. Standard recipe cards stay
  paper-led; the Home feature and Collections covers use the reviewed food photographs recorded in
  `ASSET_PROVENANCE.md`.
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
- Mobile display title is 40–48sp; body is 17sp with 28sp line height; cooking instructions are
  21sp with 32sp line height.
- Default surfaces use 0–4dp corners. Tactile controls use 12dp corners and at least 48dp height.
- Elevation is zero by default; rules carry most of the hierarchy.

## Motion

`MotionMode` is persistent in local preferences and appears under You → Appearance:

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
- Text is allowed to reflow at 200% font scale; no essential recipe content is color-only.
- Informative imagery must receive provenance and descriptive alt/content text before replacing the
  current decorative placeholder.

## Product and content guardrails

Recipe claims, dietary/allergen/nutrition/safety/storage guidance, substitutions, source attribution,
and photography rights remain evidence-backed. The app preserves the existing extraction warning and
does not infer these facts from a title or visual placeholder.

Editorial Home and Collection imagery is tracked in `ASSET_PROVENANCE.md`. Per-recipe cards do not
reserve a large image area when no verified photo exists. A user's own cook photo may appear as a
clearly labelled personal image; imported recipe imagery must still carry its own source,
attribution, and usage-right record before it is shown as recipe photography.
