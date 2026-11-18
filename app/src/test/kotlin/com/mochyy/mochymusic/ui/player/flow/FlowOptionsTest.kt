package com.mochyy.mochymusic.ui.player.flow

import com.mochyy.mochymusic.constants.*
import com.mochyy.mochymusic.ui.player.studio.PlayerMenuAction
import org.junit.Assert.*
import org.junit.Test

class FlowOptionsTest {
    @Test fun inheritsNativeFlagshipAndEditor() {
        assertTrue(PlayerStyle.FLAGSHIP_FLOW.flagshipFamily)
        assertTrue(PlayerStyle.FLAGSHIP_FLOW.hasFullEditor)
        assertTrue(PlayerStyle.FLAGSHIP_FLOW.editable)
        assertTrue(PlayerStyle.selectorColumns.flatten().contains(PlayerStyle.FLAGSHIP_FLOW))
    }
    @Test fun defaultLayoutHasRemainingTimeAndSingleLineLyrics() {
        val v=FlowOptions()
        assertTrue(v.showLyrics&&v.showProgress&&v.showVolume&&v.showButtons&&v.showDevice)
        assertTrue(v.remainingTime&&v.showTimes)
        assertEquals(7f,v.trackThickness,0f)
        assertEquals(6f,v.volumeThickness,0f)
    }
    @Test fun middleShortcutOpensNativeOutputPicker() {
        assertEquals(listOf(PlayerMenuAction.LYRICS,PlayerMenuAction.OUTPUT,PlayerMenuAction.QUEUE),FlowFooterActions)
    }
    @Test fun preferencesAreSeparateButPlaybackAndSelectionRemainShared() {
        assertFalse(FlowPreferenceOverrides.containsKey(PlayerStyleKey))
        assertEquals("flagship_flow.${PlayerCoverStyleKey.name}",FlowPreferenceOverrides[PlayerCoverStyleKey]?.name)
        assertEquals("flagship_flow.${PlayerControlsShiftKey.name}",FlowPreferenceOverrides[PlayerControlsShiftKey]?.name)
        FlowPreferenceOverrides.forEach{(original,overridden)->assertNotEquals(original.name,overridden.name)}
        assertEquals(FlowPreferenceOverrides.size,FlowPreferenceOverrides.values.map{it.name}.toSet().size)
    }
    @Test fun optionsPersistAndRestore() {
        val v=FlowOptions(lyricSize=22f,showDevice=false,volumeGap=15f,activeColor="#FFEEDD")
        assertEquals(v,FlowOptions.decode(FlowOptions.encode(v)))
    }
    @Test fun oldOptionsUseDefaults() {assertEquals(FlowOptions(),FlowOptions.decode("{}"))}
    @Test fun importedValuesRemainWithinLayoutLimits() {
        val v=FlowOptions(trackThickness=200f,volumeGap=-5f,iconScale=50f,activeColor="invalid").normalized()
        assertEquals(20f,v.trackThickness,0f);assertEquals(0f,v.volumeGap,0f)
        assertEquals(1.3f,v.iconScale,0f);assertEquals("#FFFFFF",v.activeColor)
        assertEquals(1f,FlowOptions(iconScale=Float.NaN).normalized().iconScale,0f)
    }
    @Test fun footerFootprintTracksHiddenComponents() {
        val v=FlowOptions()
        assertEquals(142f,v.footerHeight(),0f)
        assertEquals(0f,v.copy(showVolume=false,showButtons=false,showDevice=false).footerHeight(),0f)
        assertEquals(40f,v.copy(showButtons=false,showDevice=false).footerHeight(),0f)
    }
    @Test fun largeTextReservesDeviceNameHeight() {
        assertEquals(160f,FlowOptions().footerHeight(2f),0f)
    }
    @Test fun shortScreensCompactWithoutChangingStoredOptions() {
        val stored=FlowOptions(transportGap=80f,footerGap=60f)
        val compact=stored.forHeight(390f)
        assertEquals(12f,compact.transportGap,0f)
        assertEquals(8f,compact.footerGap,0f)
        assertEquals(80f,stored.transportGap,0f)
        assertEquals(stored,stored.forHeight(844f))
    }

}
