package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.ui.player.synchronizedTickerIndex

/** Follow audio timestamps exactly; no animation lead-in that can skip a rapper's last words. */
internal fun flowLyricIndex(lines: List<LyricsEntry>, positionMs: Long, offsetMs: Long): Int =
    if (LyricsUtils.isUnsynced(lines)) -1
    else synchronizedTickerIndex(lines, positionMs + offsetMs)

internal enum class FlowLyricStatus { LOADING, WAITING, LINE, UNAVAILABLE, ENDED, PREVIEW }

internal fun flowLyricStatus(
    loading: Boolean, lines: List<LyricsEntry>, index: Int, positionMs: Long, preview: Boolean = false,
): FlowLyricStatus = when {
    preview -> FlowLyricStatus.PREVIEW
    lines.isEmpty() -> if(loading) FlowLyricStatus.LOADING else FlowLyricStatus.UNAVAILABLE
    index < 0 -> FlowLyricStatus.WAITING
    index >= lines.size -> FlowLyricStatus.ENDED
    index < lines.lastIndex && positionMs > LyricsUtils.lineEndMs(lines,index) &&
        lines[index+1].time - LyricsUtils.lineEndMs(lines,index) >= 1_500L -> FlowLyricStatus.WAITING
    else -> FlowLyricStatus.LINE
}
