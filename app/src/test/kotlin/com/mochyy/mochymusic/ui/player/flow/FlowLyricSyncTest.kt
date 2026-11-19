package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.lyrics.LyricsUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowLyricSyncTest {
    private val lines = listOf(
        LyricsEntry(1_000, "First"), LyricsEntry(5_000, "Second"), LyricsEntry(10_000, "Third"),
    )

    @Test fun usesDetailLeadInAndKeepsAnUnfinishedWord() {
        val timed = listOf(
            LyricsEntry(1_000, "First", listOf(com.mochyy.mochymusic.lyrics.WordTimestamp("First", 1.0, 4.95))),
            LyricsEntry(5_000, "Next"),
        )
        assertEquals(0, flowLyricIndex(timed, 4_850, 0))
        assertEquals(1, flowLyricIndex(timed, 4_950, 0))
        assertEquals(1, flowLyricIndex(lines, 4_850, 0))
    }
    @Test fun seekingInEitherDirectionSelectsDestinationDirectly() {
        assertEquals(2, flowLyricIndex(lines, 11_000, 0))
        assertEquals(0, flowLyricIndex(lines, 2_000, 0))
        assertEquals(1, flowLyricIndex(lines, 6_000, 0))
    }
    @Test fun offsetEditsChangeLineWithoutAdvancingPlayback() {
        assertEquals(0, flowLyricIndex(lines, 4_000, 0))
        assertEquals(1, flowLyricIndex(lines, 4_000, 1_000))
        assertEquals(0, flowLyricIndex(lines, 4_000, -1_000))
    }
    @Test fun pauseKeepsSelectionStable() {
        repeat(20) { assertEquals(1, flowLyricIndex(lines, 6_000, 0)) }
    }
    @Test fun newSongUsesItsOwnTimestamps() {
        val next = listOf(LyricsEntry(20_000, "New song"), LyricsEntry(25_000, "Next"))
        assertEquals(-1, flowLyricIndex(next, 0, 0))
        assertEquals(0, flowLyricIndex(next, 21_000, 0))
    }
    @Test fun plainLyricsAndMissingLyricsHaveNoActiveLine() {
        assertEquals(-1, flowLyricIndex(emptyList(), 8_000, 0))
        assertEquals(-1, flowLyricIndex(listOf(LyricsEntry(0, "Plain"), LyricsEntry(0, "Text")), 8_000, 0))
    }
}
