package com.mochyy.mochymusic.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mochyy.mochymusic.constants.PlayerStyle

enum class AppThemePreset(val label: String, val player: PlayerStyle) {
    DEFAULT("Default", PlayerStyle.FLAGSHIP),
    FLOW("Flow", PlayerStyle.FLAGSHIP_FLOW),
}

internal val AppThemePresetKey = stringPreferencesKey("appThemePreset")
internal val LocalAppThemePreset = staticCompositionLocalOf { AppThemePreset.DEFAULT }
internal val FlowAccent = Color(0xFFB9A4FF)
internal val FlowSurface = Color(0xFF121018)
