package com.mochyy.mochymusic.ui.player.drizzy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mochyy.mochymusic.utils.rememberPreference

internal val DrizzyDesignKey = stringPreferencesKey("drizzy_design_v1")
internal val DrizzyPresetsKey = stringPreferencesKey("drizzy_presets_v1")

@Composable
internal fun rememberDrizzyDesign(): DrizzyDesign {
    val raw by rememberPreference(DrizzyDesignKey, "")
    return remember(raw) { runCatching { DrizzyDesign.decode(raw) }.getOrDefault(DrizzyDesign()) }
}
