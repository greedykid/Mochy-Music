# Flagship Flow

A native Flagship-derived style, available beside Drizzy in the player style selector. It uses the actual Flagship cover, ambient/frost background, song-info row, animated transport glyphs, native seek handling (including buffering and pending seeks), queue, lyrics pages and device-output sheet.

## Default layout

Cover/background and song information retain the native Flagship appearance. Below the song information:

1. One synchronized lyric line, truncated with an ellipsis, and a chevron. Tapping opens full lyrics. If synchronized lyrics are unavailable, the row says **Open lyrics**.
2. Thick, rounded, handle-free duration track. Elapsed time on the left; remaining time on the right.
3. Native three-button previous / play-pause / next row, with no circle around play.
4. Handle-free media-volume bar with speaker icons.
5. Unlabelled lyrics / cast-shaped output picker / queue shortcuts.
6. The actual output device's name.

The middle shortcut invokes native `PlayerMenuAction.OUTPUT`, opening **Change playback**. It is not a Cast-only shortcut. The lyrics row and footer buttons supply their measured bounds to the native page transition.

## Editing

Hold the player, choose **Flagship Flow**, then **Edit**. Cover/background and middle/menu editing use the existing Flagship editor. The controls/seek-bar/bottom-bar tiles open Flow layout options. A tune button at top right also opens these options.

Flow options include visibility for lyric/progress/transport/volume/shortcuts/device text, lyric size, track thickness and colors, timestamps/remaining-time mode, playback icon scale, gaps between sections and bottom padding. Changes save automatically. Reset only the Flow layout, or reset the layout and native appearance together.

The native appearance keys are explicitly remapped to `flagship_flow.*` while this style is active. Account settings, playback settings and selected player style remain shared. Each selector preview has its own preference scope. Flow does not overwrite the original Flagship or Drizzy design.

Long control content scrolls within the available area on short screens. The native cover/background and controls move with the existing editor. Flow uses its vertically stacked control layout in landscape as well; it does not switch to the original Flagship two-column landscape layout.

## Validation

GitHub Actions builds the universal GMS debug APK and runs both `DrizzyDesignTest` and `FlowOptionsTest`. Flow tests cover native editor/style registration, reference defaults, OUTPUT shortcut binding, appearance-key isolation, option round trips, default compatibility, malformed option normalization and bottom-section sizing.

Device checks still required: select Flow/Flagship/Drizzy and compare appearance; edit cover/background independently; restart persistence; native pending seek/buffering; synchronized and missing lyrics; lyrics/queue open/close transitions; output switching and hardware volume; short screens, landscape and large text. The debug signer is the same persistent fork key used by the Drizzy APK.

## Smooth lyric and circular feedback update

Current-line changes use a short fade/vertical slide. Backward seeks reverse the slide; changing songs crossfades. A clipped one-line viewport keeps outgoing text clear of the duration bar. Previews and inactive players switch without animation.

Flagship Flow opts in to centered circular press feedback for native playback and song-info buttons. The footer shortcuts, lyric chevron and Flow settings button use the same feedback. Held skip controls retain their speed badges and hold behavior. Other styles keep their original feedback. Animation jobs stop with their indication nodes.

The companion Looks 1.1 ZIP still selects `FLAGSHIP_FLOW`; the animation code requires the updated fork APK, not just a manifest update.

## Scroll and synchronization update

Flow's compact-screen scroll container now sits inside its visibility layer and stops accepting scroll while a lyrics/queue page is open. The hidden ticker also lets touches through. This keeps page lists from competing with an invisible control container.

The single-line ticker reads the same progress state and per-song offset as the full lyrics page, including seek/pause and offset changes. Untimed lyrics keep the full-lyrics shortcut. Manual lyric scrolling suspends follow until the Follow lyrics button or a timed line is tapped; a line tap compensates for the song offset when seeking. Queue drag handles and list state are preserved.

CI adds six timing regression cases; actual touch behavior still needs device verification. The Looks v1.1 selector remains compatible without reimporting.

## Word sync, transitions and optimized update builds

- Lyrics/queue footer buttons toggle their own page and switch directly to the other page. The Flow footer stays reachable during immersive list scrolling, with permanent list clearance.
- Flow's lyric strip reuses the native karaoke renderer: actual word/syllable timestamps drive the sweep; long lines follow the sung text horizontally with soft edges. Untimed words retain line timing. Font size and foreground colour remain configurable. Per-frame work is in drawing/layers, and the renderer is removed while the strip is hidden.
- Flow pages fade/slide as one surface rather than clipping into a Flagship pill. Per-row card morphs are skipped; cover accent extraction is cached across reopening. The native Flagship style keeps its existing transition.
- Flow's top handle owns its drag, follows the finger via the sheet's hold/release API, and returns or closes using density-independent distance/velocity thresholds. Cancellation returns it open.
- `optimized` inherits release R8/resource shrinking and disables debugging/source information while keeping `.debug` and the persistent fork signing key. It is an update to previous fork APKs. CI builds universal and ARM64 variants, retains mappings and test XML, plus a debug fallback. ARM64 is only for compatible 64-bit ARM devices.
- Device checks still needed: repeated toggles, switching mid-animation, finger tracking/reversal/cancel, word timing/seek/pause, long lines and RTL, scrolling both lists, playback/output/modules/preferences after updating. No device frame-time measurements have been collected here.

## Adaptive ticker follow and waiting states

- The shared ticker measures word edges once with the displayed text layout and follows real timestamps directly. The active word stays in the viewport; short word durations accelerate the follow, upcoming words are anticipated, and the last word is fully visible when it begins (where it fits). Oversized words follow their sung portion. Seeking snaps to the recomputed destination.
- Tickers no longer inherit the full lyrics page's 300 ms animation lead-in, which could replace a rapid last word before its timestamp finished. Full lyrics page animation is unchanged. Line-only lyrics scroll earlier without inventing word timing.
- Flow distinguishes loading, intro/instrumental wait, active lyrics, and unavailable/finished lyrics. Loading has a label with pulsing dots; waits have three pulsing dots; unavailable/finished lyrics retain the static full-lyrics shortcut. All states use the same 40 dp viewport and fade between placeholder/text; hidden ticker animations are not mounted. Completion tracking ends loading even when a provider returns no connection/no lyrics.
- CI now tests the optimized universal variant and builds optimized universal/ARM64 in separate matrix jobs. It avoids compiling an extra debug variant and retains each variant's mappings and optimized unit-test XML. Existing earlier debug fallback APKs remain available separately.
- Regression tests cover 24/60/120 Hz rapid word visibility, whole last-word exposure, slow/fast response, reverse seek, short/unmeasured text, oversized words, RTL end position, exact line boundaries, and placeholder states. Device gesture/frame-time checks are still required.
