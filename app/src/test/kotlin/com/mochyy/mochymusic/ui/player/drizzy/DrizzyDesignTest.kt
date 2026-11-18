package com.mochyy.mochymusic.ui.player.drizzy

import org.junit.Assert.*
import org.junit.Test

class DrizzyDesignTest {
    @Test fun defaultDesignKeepsApprovedControls() {
        val d=DrizzyDesign()
        assertEquals(listOf("back10","previous","play","next","forward10"),d.portrait.transport)
        assertFalse(d.portrait.part("progress").thumb)
        assertFalse(d.portrait.part("volume").thumb)
        assertFalse(d.portrait.part("queueButton").label)
        assertEquals("like",d.portrait.part("love").action)
        assertTrue(d.portrait.order.indexOf("lyrics")>d.portrait.order.indexOf("transport"))
    }
    @Test fun customPortraitAndLandscapeRoundTripIndependently() {
        val d=DrizzyDesign().let { it.copy(portrait=it.portrait.change("title",DrizzyPart(size=31f,color="#AABBCC")),landscape=it.landscape.copy(background="#000000")) }
        assertEquals(d,DrizzyDesign.decode(DrizzyDesign.encode(d)))
        assertEquals(31f,d.portrait.part("title").size,0f)
        assertEquals(25f,d.landscape.part("title").size,0f)
    }
    @Test fun importingMalformedValuesCannotHideRecoveryOrBreakLayout() {
        val bad=DrizzyDesign(portrait=DrizzyLayout(order=listOf("lyrics","lyrics","unknown"),transport=listOf("play","play"),padding=-5f,
            parts=mapOf("progress" to DrizzyPart(width=4f,size=-30f,color="nope",opacity=-1f,action="unsupported"),"unknown" to DrizzyPart())))
        val d=DrizzyDesign.decode(DrizzyDesign.encode(bad)).portrait
        assertEquals(DrizzyDesign.sections.toSet(),d.order.toSet())
        assertEquals(d.order.size,d.order.distinct().size)
        assertEquals(5,d.transport.size)
        assertEquals(0f,d.padding,0f)
        assertEquals(1f,d.part("progress").width,0f)
        assertEquals(2f,d.part("progress").size,0f)
        assertEquals("#FFFFFF",d.part("progress").color)
        assertFalse("unknown" in d.parts)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectUnsupportedVersion() {DrizzyDesign.decode("{\"version\":99}")}
    @Test(expected=IllegalArgumentException::class) fun rejectOversizedImport() {DrizzyDesign.decode(" ".repeat(256*1024+1))}
    @Test fun oldMinimalDesignGetsNewDefaults() {
        val d=DrizzyDesign.decode("{\"version\":1}")
        assertEquals(DrizzyDesign(),d)
    }
    @Test fun undoRedoAndNewBranchPreserveWholeDesign() {
        val a=DrizzyDesign();val b=a.copy(portrait=a.portrait.copy(padding=30f));val c=a.copy(landscape=a.landscape.copy(padding=12f))
        val h=DrizzyHistory(a);h.commit(b);assertEquals(a,h.undo());assertEquals(b,h.redo())
        h.undo();h.commit(c);assertFalse(h.canRedo);assertEquals(a,h.undo())
    }
    @Test fun historyIsBounded() {
        val h=DrizzyHistory(DrizzyDesign())
        repeat(100){h.commit(DrizzyDesign(portrait=DrizzyLayout(padding=it.toFloat())))}
        var count=0;while(h.canUndo){h.undo();count++}
        assertEquals(80,count)
    }
}
