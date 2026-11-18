package com.mochyy.mochymusic.ui.player.flow

import androidx.datastore.preferences.core.*
import com.mochyy.mochymusic.constants.*

/** Only native player presentation keys are remapped. Playback, accounts and style selection are shared. */
internal val FlowPreferenceOverrides:Map<Preferences.Key<*>,Preferences.Key<*>> = buildMap {
    listOf(PlayerCoverStyleKey,FlagshipBackgroundStyleKey,PlayerBackgroundStyleKey,FlagshipBottomBarStyleKey,
        PlayerBottomBarStyleKey,FlagshipTransportLayoutKey,SeekBarStyleKey,ButtonStyleKey,PlayerEndTimestampKey,
        PlayerMenuSlotsKey,PlayerSideButtonKey,DiscLabelContentKey,DiscNameKey).forEach{put(it,stringPreferencesKey("flagship_flow.${it.name}"))}
    listOf(ShowFormatBadgeKey,ShowFormatSampleRateKey,ShowPlayerBottomCardKey,ShowBigDiscStylesKey,
        DiscRotatingAnimationKey,DiscRealisticModeKey,CanvasThumbnailAnimationKey,PlayVideoBackgroundKey,YtVideoBackgroundLyricsSyncKey).forEach{put(it,booleanPreferencesKey("flagship_flow.${it.name}"))}
    listOf(PlayerControlsShiftKey,PlayerCardGrowthKey).forEach{put(it,floatPreferencesKey("flagship_flow.${it.name}"))}
}
