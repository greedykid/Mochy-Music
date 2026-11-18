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
