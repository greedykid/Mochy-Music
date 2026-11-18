package com.mochyy.mochymusic.ui.player.flow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.ui.player.drizzy.ColorField
import com.mochyy.mochymusic.ui.player.drizzy.NumberField
import com.mochyy.mochymusic.ui.player.drizzy.Toggle
import com.mochyy.mochymusic.ui.player.menu.MenuSheetSurface
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
internal fun FlowOptionsSheet(onDismiss:()->Unit) {
    var raw by rememberPreference(FlowOptionsKey,"")
    val options=remember(raw){runCatching{FlowOptions.decode(raw)}.getOrDefault(FlowOptions())}
    val save:(FlowOptions)->Unit={raw=FlowOptions.encode(it)}
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    var reset by remember{mutableStateOf(false)}
    MenuSheetSurface(onDismiss=onDismiss,wrapHeight=true) { _ ->
        Column(Modifier.fillMaxWidth().heightIn(max=620.dp).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Flagship Flow",fontSize=22.sp,fontWeight=FontWeight.Bold)
            Text("Changes save automatically. Use the native cover/background tiles in Edit player for artwork and background.",fontSize=13.sp)
            Toggle("Show current lyric line",options.showLyrics){save(options.copy(showLyrics=it))}
            NumberField("Lyric size",options.lyricSize,10f..30f){save(options.copy(lyricSize=it))}
            Toggle("Show progress",options.showProgress){save(options.copy(showProgress=it))}
            NumberField("Duration track thickness",options.trackThickness,2f..20f){save(options.copy(trackThickness=it))}
            Toggle("Show timestamps",options.showTimes){save(options.copy(showTimes=it))}
            Toggle("Show remaining time",options.remainingTime){save(options.copy(remainingTime=it))}
            Toggle("Show playback controls",options.showTransport){save(options.copy(showTransport=it))}
            NumberField("Playback icon scale",options.iconScale,.65f..1.3f){save(options.copy(iconScale=it))}
            NumberField("Gap above playback controls",options.transportGap,0f..80f){save(options.copy(transportGap=it))}
            Toggle("Show volume",options.showVolume){save(options.copy(showVolume=it))}
            NumberField("Volume track thickness",options.volumeThickness,2f..20f){save(options.copy(volumeThickness=it))}
            NumberField("Gap above volume section",options.volumeGap,0f..80f){save(options.copy(volumeGap=it))}
            Toggle("Show lyrics / output / queue buttons",options.showButtons){save(options.copy(showButtons=it))}
            NumberField("Gap above shortcut buttons",options.footerGap,0f..60f){save(options.copy(footerGap=it))}
            Toggle("Show output device name",options.showDevice){save(options.copy(showDevice=it))}
            NumberField("Gap above device name",options.deviceGap,0f..50f){save(options.copy(deviceGap=it))}
            NumberField("Bottom padding",options.bottomGap,0f..50f){save(options.copy(bottomGap=it))}
            ColorField("Lyrics and bar color",options.activeColor){save(options.copy(activeColor=it))}
            ColorField("Unfilled bar color",options.inactiveColor){save(options.copy(inactiveColor=it))}
            TextButton(onClick={save(FlowOptions())}){Text("Reset Flow layout")}
            TextButton(onClick={reset=true}){Text("Reset Flow layout, artwork and background")}
            TextButton(onClick=onDismiss){Text("Done")}
        }
    }
    if(reset)AlertDialog(onDismissRequest={reset=false},title={Text("Reset Flagship Flow?")},text={Text("Restore this style's layout and native appearance defaults.")},
        confirmButton={TextButton(onClick={scope.launch{context.dataStore.edit{prefs->FlowPreferenceOverrides.values.forEach{prefs.remove(it)};prefs.remove(FlowOptionsKey)}};reset=false}){Text("Reset")}},
        dismissButton={TextButton(onClick={reset=false}){Text("Cancel")}})
}
