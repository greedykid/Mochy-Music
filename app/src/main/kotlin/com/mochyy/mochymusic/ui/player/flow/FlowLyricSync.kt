package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.ui.player.lyricAnchorIndex

/** Use the full page’s anchor, including words that continue into the next line’s lead-in. */
internal fun flowLyricIndex(lines: List<LyricsEntry>, positionMs: Long, offsetMs: Long): Int =
    if (LyricsUtils.isUnsynced(lines)) -1
    else lyricAnchorIndex(lines, positionMs + offsetMs)

internal enum class FlowLyricStatus { LOADING, WAITING, LINE, UNAVAILABLE, ENDED, PREVIEW }

internal fun flowLyricStatus(
    loading: Boolean, lines: List<LyricsEntry>, index: Int, preview: Boolean = false, waiting: Boolean = false,
): FlowLyricStatus = when {
    preview -> FlowLyricStatus.PREVIEW
    lines.isEmpty() -> if(loading) FlowLyricStatus.LOADING else FlowLyricStatus.UNAVAILABLE
    waiting -> FlowLyricStatus.WAITING
    index >= lines.size -> FlowLyricStatus.ENDED
    else -> FlowLyricStatus.LINE
}
