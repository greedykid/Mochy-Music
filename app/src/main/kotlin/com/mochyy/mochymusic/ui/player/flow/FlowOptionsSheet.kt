package com.mochyy.mochymusic.ui.player.flow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import com.mochyy.mochymusic.ui.player.studio.OptionSheet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.ui.player.drizzy.ColorField
import com.mochyy.mochymusic.ui.player.drizzy.NumberField
import com.mochyy.mochymusic.ui.player.drizzy.Toggle
import com.mochyy.mochymusic.ui.player.menu.MenuSheetSurface
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.launch

internal enum class FlowEditSection(val title: String) {
    LYRICS("Lyrics & seek bar"), PLAYBACK("Playback"), FOOTER("Footer"), APPEARANCE("Colors"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FlowOptionsSheet(initialSection: FlowEditSection, onDismiss: () -> Unit) {
    var raw by rememberPreference(FlowOptionsKey,"")
    val options=remember(raw){runCatching{FlowOptions.decode(raw)}.getOrDefault(FlowOptions())}
    val save:(FlowOptions)->Unit={raw=FlowOptions.encode(it)}
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    var reset by remember{mutableStateOf(false)}
    var section by remember(initialSection) { mutableStateOf(initialSection) }
    MenuSheetSurface(onDismiss = onDismiss, wrapHeight = true) { _ ->
        OptionSheet(title = "Flagship Flow", subtitle = section.title) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowEditSection.entries.forEach { item ->
                    FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item.title) })
                }
            }
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 8 }) togetherWith
                        (fadeOut(tween(120)) + slideOutVertically(tween(180)) { -it / 8 })
                },
                label = "Flow editor section",
            ) { selected ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (selected) {
                        FlowEditSection.LYRICS -> {
                            Toggle("Show current lyric line",options.showLyrics){save(options.copy(showLyrics=it))}
                            NumberField("Lyric size",options.lyricSize,10f..30f){save(options.copy(lyricSize=it))}
                            Toggle("Show progress",options.showProgress){save(options.copy(showProgress=it))}
                            NumberField("Duration track thickness",options.trackThickness,2f..20f){save(options.copy(trackThickness=it))}
                            Toggle("Show timestamps",options.showTimes){save(options.copy(showTimes=it))}
                            Toggle("Show remaining time",options.remainingTime){save(options.copy(remainingTime=it))}
                        }
                        FlowEditSection.PLAYBACK -> {
                            Toggle("Show playback controls",options.showTransport){save(options.copy(showTransport=it))}
                            NumberField("Playback icon scale",options.iconScale,.65f..1.3f){save(options.copy(iconScale=it))}
                            NumberField("Gap above playback controls",options.transportGap,0f..80f){save(options.copy(transportGap=it))}
                        }
                        FlowEditSection.FOOTER -> {
                            Toggle("Show volume",options.showVolume){save(options.copy(showVolume=it))}
                            NumberField("Volume track thickness",options.volumeThickness,2f..20f){save(options.copy(volumeThickness=it))}
                            NumberField("Gap above volume section",options.volumeGap,0f..80f){save(options.copy(volumeGap=it))}
                            Toggle("Show lyrics / output / queue buttons",options.showButtons){save(options.copy(showButtons=it))}
                            NumberField("Gap above shortcut buttons",options.footerGap,0f..60f){save(options.copy(footerGap=it))}
                            Toggle("Show output device name",options.showDevice){save(options.copy(showDevice=it))}
                            NumberField("Gap above device name",options.deviceGap,0f..50f){save(options.copy(deviceGap=it))}
                            NumberField("Bottom padding",options.bottomGap,0f..50f){save(options.copy(bottomGap=it))}
                        }
                        FlowEditSection.APPEARANCE -> {
                            ColorField("Lyrics and bar color",options.activeColor){save(options.copy(activeColor=it))}
                            ColorField("Unfilled bar color",options.inactiveColor){save(options.copy(inactiveColor=it))}
                            TextButton(onClick={save(FlowOptions())}){Text("Reset Flow layout")}
                            TextButton(onClick={reset=true}){Text("Reset Flow layout, artwork and background")}
                        }
                    }
                }
            }
        }
    }
    if(reset)AlertDialog(onDismissRequest={reset=false},title={Text("Reset Flagship Flow?")},text={Text("Restore this style's layout and native appearance defaults.")},
        confirmButton={TextButton(onClick={scope.launch{context.dataStore.edit{prefs->FlowPreferenceOverrides.values.forEach{prefs.remove(it)};prefs.remove(FlowOptionsKey)}};reset=false}){Text("Reset")}},
        dismissButton={TextButton(onClick={reset=false}){Text("Cancel")}})
}
