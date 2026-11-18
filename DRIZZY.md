# Drizzy player and component studio

Drizzy is a new player style in the style selector. Hold the open player, choose **Drizzy**, then tap the edit button. The small tune button at the top right of Drizzy also opens **Drizzy Studio**, even if all editable components are hidden.

## Default design

Full-width artwork fades into charcoal. Audio quality and song information precede a thick, handle-free duration track. The five transport actions are back 10 seconds, previous, play/pause in a white circle, next, and forward 10 seconds. The favorite button is a heart. Timed lyrics sit below transport and can be tapped to seek. A thick, handle-free media-volume track precedes the lyrics/output/queue utility row. Queue has no text label. Artwork and metadata come from the current song; no sample album art is bundled.

## Editing

The editor has a live renderer preview, an expanded layout view, and a component inspector. Tap a section in the preview or choose a component chip. Drag a section to adjust its position. Use the inspector's sliders or numeric **Set** inputs for exact values.

- Sections: artwork, audio quality, song information, duration, transport, lyrics, volume, utility row, device name.
- Individual parts: title, artist, favorite, menu, all five transport buttons, and all three utility buttons.
- Reorder sections and buttons with **Move earlier / left** and **Move later / right**. Offsets, opacity, section width, section spacing, text size/color/weight/alignment/line count, artwork height/crop/corners/shadow, and button size/color/fill/radius/action/label are editable.
- Track thickness, unfilled-track color, roundness, optional handle and duration timestamps are editable. Lyrics height and active/inactive colors are editable.
- Background color, album-derived color, backdrop blur/dim, cover fade and side padding are editable.
- Portrait and landscape are independent. Copying portrait to landscape asks before replacing it.
- Changes save automatically. Undo/redo records completed changes, with a bounded 80-step history for the current editor session. Reset a component or reset the whole design.
- Save up to 20 named presets. Export/import the complete two-orientation design as JSON using the system file picker. Imports reject unsupported versions and oversized files, and normalize unknown components, duplicate ordering and unsafe values.

Component movement stays within bounded offsets; this is a structured native component editor, not an arbitrary-code or pixel-canvas editor. Long content scrolls instead of forcing all elements to overlap on small screens or at large text sizes. The fixed recovery tune button is not editable.

## Playback behavior

Queue and full lyrics use Mochy's existing overlays. Output opens the existing device-output sheet. Media volume follows hardware volume changes. Seek actions clamp to the known song duration; timed-line seeks account for the lyrics offset. Missing synchronized lyrics show an explanatory fallback. Plain, untimed lyrics remain available through the full lyrics view. The native icon set and font rendering may differ slightly from design images.

## Build and validation

Branch: `drizzy-player`. The GitHub Actions debug build runs `DrizzyDesignTest` before assembling the universal GMS debug APK. Tests cover approved defaults, JSON round trips, malformed values, unknown version/oversized imports, backward-compatible defaults, independent orientations and bounded undo/redo. The signing key is stored only in the repository's `DRIZZY_DEBUG_KEYSTORE` Actions secret.

Device checks: select/leave Drizzy; open editor from selector and tune; play/pause/skip/seek; lyrics loading and offset-aware line seeks; queue; hardware/media volume; output sheet; portrait/landscape; long titles and large font scale; hide/reorder all parts and recover; save/load presets; import/export; restart persistence.
