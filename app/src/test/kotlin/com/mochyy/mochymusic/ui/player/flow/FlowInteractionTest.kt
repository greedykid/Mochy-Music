package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.lyrics.WordTimestamp
import com.mochyy.mochymusic.ui.player.*
import org.junit.Assert.*
import org.junit.Test

class FlowInteractionTest {
    @Test fun repeatedFooterTapClosesCurrentPage() {
        for (page in listOf(PlayerPage.LYRICS, PlayerPage.QUEUE)) {
            val opened = toggledPlayerPage(PlayerPage.NONE, page)
            assertEquals(page, opened)
            assertEquals(PlayerPage.NONE, toggledPlayerPage(opened, page))
        }
    }
    @Test fun otherFooterButtonSwitchesPagesDirectly() {
        assertEquals(PlayerPage.QUEUE, toggledPlayerPage(PlayerPage.LYRICS, PlayerPage.QUEUE))
        assertEquals(PlayerPage.LYRICS, toggledPlayerPage(PlayerPage.QUEUE, PlayerPage.LYRICS))
    }
    @Test fun handleReturnsForShortPullAndClosesForLongPull() {
        assertFalse(shouldCloseHandle(20f, 0f))
        assertTrue(shouldCloseHandle(90f, 0f))
    }
    @Test fun flickDirectionOverridesHandleDistance() {
        assertFalse(shouldCloseHandle(100f, -800f))
        assertTrue(shouldCloseHandle(10f, 800f))
        assertFalse(shouldCloseHandle(0f, 0f))
    }
    @Test fun karaokeSweepAdvancesInsideEachTimedWord() {
        val text = "Hello world"
        val ranges = karaokeTimedRanges(text, listOf(
            WordTimestamp("Hello", 1.0, 2.0), WordTimestamp("world", 2.2, 3.2),
        ))
        val head = karaokeHeadFor(text)
        head.advance(ranges, 1.5, .016f, false)
        assertEquals(2.5f, head.offset.floatValue, .01f)
        head.advance(ranges, 2.7, .016f, false)
        assertEquals(8.5f, head.offset.floatValue, .01f)
        // Seeking backwards recomputes the sweep instead of playing through missed words.
        head.advance(ranges, 1.2, .016f, false)
        assertEquals(1f, head.offset.floatValue, .01f)
    }
    @Test fun missingWordTimingDoesNotInventKaraoke() {
        assertTrue(karaokeTimedRanges("Plain line", emptyList()).isEmpty())
    }
}
