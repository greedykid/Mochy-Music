package com.mochyy.mochymusic.ui.screens.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.constants.PlayerStyleKey
import com.mochyy.mochymusic.ui.theme.AppThemePreset
import com.mochyy.mochymusic.ui.theme.AppThemePresetKey
import com.mochyy.mochymusic.ui.theme.LocalAppThemePreset
import com.mochyy.mochymusic.ui.theme.MochyTheme
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.launch

@Composable
internal fun AppThemeSection() {
    val current = LocalAppThemePreset.current
    var selected by remember(current) { mutableStateOf(current) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("App theme", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemePreset.entries.forEach { preset ->
                FilterChip(
                    selected = selected == preset,
                    onClick = { selected = preset },
                    label = { Text(preset.label) },
                )
            }
        }
        MochyTheme(preset = selected) {
            val colors = MaterialTheme.colorScheme
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(72.dp)
                            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.primary)), RoundedCornerShape(12.dp)),
                    )
                    Text("Playlist", color = colors.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                    Box(
                        Modifier.fillMaxWidth().height(8.dp)
                            .background(colors.outlineVariant, RoundedCornerShape(4.dp)),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(72.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(36.dp).background(colors.primary, RoundedCornerShape(8.dp)))
                    }
                    Text("Player", color = colors.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (selected == AppThemePreset.FLOW) "Flagship Flow" else "Flagship",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Button(onClick = {
            val preset = selected
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs[AppThemePresetKey] = preset.name
                    prefs[PlayerStyleKey] = preset.player.name
                }
            }
        }) {
            Text("Apply")
        }
    }
}
