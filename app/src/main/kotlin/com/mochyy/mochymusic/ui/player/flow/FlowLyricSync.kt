package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.lyrics.LyricsUtils

/** Shared line timing rules with the full lyrics page, including its lead-in and outro. */
internal fun flowLyricIndex(lines: List<LyricsEntry>, positionMs: Long, offsetMs: Long): Int =
    if (LyricsUtils.isUnsynced(lines)) -1
    else LyricsUtils.findCurrentLineIndex(lines, positionMs + offsetMs)
