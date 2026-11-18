package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.ui.player.*
import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.lyrics.WordTimestamp
import org.junit.Assert.*
import org.junit.Test

class TickerScrollFollowTest {
    private val rap=(0..9).map { TickerWordBounds(it*80L,(it+1)*80L,it*100f,(it+1)*100f) }
    @Test fun fastWordsStayVisibleAtDifferentFrameRates() {
        for(hz in listOf(24,60,120)) {
            val follower=TickerScrollFollower()
            for(frame in 0 until hz) {
                val ms=frame*1000L/hz
                if(ms>=800) break
                val word=rap.last { it.startMs<=ms }
                val plan=tickerScrollPlan(rap,ms,200f,1000f)
                val scroll=follower.advance(plan,1f/hz,200f)
                assertTrue("$hz Hz, $ms ms",word.right-scroll<=200.1f)
                assertTrue(word.left-scroll>=-.1f)
            }
        }
    }
    @Test fun entireLastWordIsVisibleWhenItStarts() {
        val plan=tickerScrollPlan(rap,720,200f,1000f)
        val follower=TickerScrollFollower()
        follower.advance(tickerScrollPlan(rap,640,200f,1000f),.016f,200f)
        val scroll=follower.advance(plan,.016f,200f)
        assertEquals(800f,scroll,.01f)
        assertTrue(rap.last().right-scroll<=200f)
    }
    @Test fun slowWordsKeepGentlerMotion() {
        val slow=listOf(TickerWordBounds(0,2000,300f,400f),TickerWordBounds(2000,4000,400f,500f))
        assertTrue(tickerScrollPlan(slow,1000,200f,500f).followSeconds > tickerScrollPlan(rap,400,200f,1000f).followSeconds)
    }
    @Test fun seekBackwardsRecomputesViewportImmediately() {
        val f=TickerScrollFollower()
        f.advance(tickerScrollPlan(rap,750,200f,1000f),.016f,200f)
        assertEquals(0f,f.advance(tickerScrollPlan(rap,0,200f,1000f),.016f,200f,seek=true),.01f)
    }
    @Test fun shortAndUnmeasuredTextNeverScrollsOutsideBounds() {
        assertEquals(0f,tickerScrollPlan(rap,750,1200f,1000f).target,0f)
        assertEquals(0f,tickerScrollPlan(emptyList(),750,0f,1000f).target,0f)
    }
    @Test fun oversizedWordDoesNotCreateInvalidClamps() {
        val p=tickerScrollPlan(listOf(TickerWordBounds(0,200,0f,1000f)),100,200f,1000f)
        assertTrue(p.target in 0f..800f)
        val value=TickerScrollFollower().advance(p,.016f,200f)
        assertTrue(value.isFinite())
        assertTrue(500f-value in 30f..170f)
    }
    @Test fun rtlFinishesAtLeftEndOfText() {
        val words=listOf(TickerWordBounds(0,100,400f,500f,true),TickerWordBounds(100,200,0f,100f,true))
        val p=tickerScrollPlan(words,150,200f,500f)
        assertEquals(0f,p.target,.01f)
    }
    @Test fun loadingAndMissingLyricsAreDifferentStates() {
        assertEquals(FlowLyricStatus.LOADING,flowLyricStatus(true,emptyList(),-1,0))
        assertEquals(FlowLyricStatus.UNAVAILABLE,flowLyricStatus(false,emptyList(),-1,0))
    }
    @Test fun introAndOutroDoNotPretendToBeLoading() {
        val lines=listOf(LyricsEntry(1000,"First"),LyricsEntry(2000,"Second"))
        assertEquals(FlowLyricStatus.WAITING,flowLyricStatus(false,lines,-1,500))
        assertEquals(FlowLyricStatus.LINE,flowLyricStatus(false,lines,0,1000))
        assertEquals(FlowLyricStatus.ENDED,flowLyricStatus(false,lines,2,8000))
    }
    @Test fun instrumentalGapWaitsForNextVocal() {
        val lines=listOf(LyricsEntry(0,"First",listOf(WordTimestamp("First",0.0,1.0))),LyricsEntry(5000,"Second"))
        assertEquals(FlowLyricStatus.WAITING,flowLyricStatus(false,lines,0,3000))
        assertEquals(FlowLyricStatus.LINE,flowLyricStatus(false,lines,1,5000))
    }
    @Test fun nextLineDoesNotReplaceQuickLastWordEarly() {
        val lines=listOf(LyricsEntry(0,"Fast end"),LyricsEntry(800,"Next"))
        assertEquals(0,synchronizedTickerIndex(lines,799))
        assertEquals(1,synchronizedTickerIndex(lines,800))
    }
}
