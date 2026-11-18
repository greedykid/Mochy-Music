package com.mochyy.mochymusic.ui.player.drizzy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mochyy.mochymusic.ui.player.styles.DrizzyPlayerStyle
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** Full-screen editor: renderer preview and inspector use the same design model. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DrizzyEditor(actions: StyleActions, onDismiss: () -> Unit) {
    var raw by rememberPreference(DrizzyDesignKey, "")
    var presetsRaw by rememberPreference(DrizzyPresetsKey, "{}")
    val initial = remember { runCatching { DrizzyDesign.decode(raw) }.getOrDefault(DrizzyDesign()) }
    val history = remember { DrizzyHistory(initial) }
    var design by remember { mutableStateOf(initial) }
    var wide by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("cover") }
    var largePreview by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var presetName by remember { mutableStateOf("") }
    var pendingReset by remember { mutableStateOf(false) }
    var copyFromPortrait by remember { mutableStateOf(false) }
    var presets by remember(presetsRaw) { mutableStateOf(runCatching { Json.decodeFromString<Map<String,String>>(presetsRaw) }.getOrDefault(emptyMap())) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val commit: (DrizzyDesign) -> Unit = { value ->
        val clean=DrizzyDesign.decode(DrizzyDesign.encode(value))
        history.commit(clean);design=history.current;raw=DrizzyDesign.encode(design)
    }
    val current=design.layout(wide)
    val part=current.part(selected)
    fun layout(value: DrizzyLayout) = commit(design.change(wide,value))
    fun update(value: DrizzyPart) = layout(current.change(selected,value))
    fun move(direction:Int) {
        val order=when(selected){in current.order->current.order;in current.transport->current.transport;in current.footer->current.footer;else->emptyList()}.toMutableList()
        val index=order.indexOf(selected);val to=index+direction
        if(index>=0&&to in order.indices) {
            order[index]=order[to];order[to]=selected
            layout(when(selected){in current.order->current.copy(order=order);in current.transport->current.copy(transport=order);else->current.copy(footer=order)})
        }
    }
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)scope.launch{
            notice=runCatching{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(DrizzyDesign.encode(design).toByteArray())}?:error("Cannot write file")};"Design exported."}.getOrElse{"Export failed: ${it.message}"}
        }
    }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch{
            runCatching{withContext(Dispatchers.IO){
                context.contentResolver.openInputStream(uri)?.use{input->
                    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=256*1024){"Design file exceeds 256 KB"};out.write(buffer,0,n)}
                    DrizzyDesign.decode(out.toString("UTF-8"))
                }?:error("Cannot read file")
            }}.onSuccess{commit(it);notice="Design imported."}.onFailure{notice="Import failed: ${it.message}"}
        }
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(color=Color(0xFF151519),contentColor=Color.White,modifier=Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Drizzy Studio",fontWeight=FontWeight.Bold,fontSize=20.sp,modifier=Modifier.weight(1f))
                    TextButton(onClick={design=history.undo();raw=DrizzyDesign.encode(design)},enabled=history.canUndo){Text("Undo")}
                    TextButton(onClick={design=history.redo();raw=DrizzyDesign.encode(design)},enabled=history.canRedo){Text("Redo")}
                    TextButton(onClick=onDismiss){Text("Done")}
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    FilterChip(selected=!wide,onClick={wide=false},label={Text("Portrait")})
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected=wide,onClick={wide=true},label={Text("Landscape")})
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={largePreview=!largePreview}){Text(if(largePreview)"Inspector" else "Layout view")}
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(if(largePreview)480.dp else 215.dp).clipToBounds().background(Color.Black),contentAlignment=Alignment.Center) {
                    val w=if(wide)780.dp else 390.dp
                    val h=if(wide)390.dp else 844.dp
                    val scale=minOf(maxWidth/w,maxHeight/h)
                    DrizzyPlayerStyle(actions,Modifier.requiredSize(w,h).graphicsLayer{scaleX=scale;scaleY=scale},preview=true,
                        designOverride=design,orientationOverride=wide,selected=selected,onSelect={selected=it},
                        onMove={id,x,y->layout(current.change(id,current.part(id).copy(x=x,y=y)))})
                }
                if(!largePreview)Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Tap a component in the preview, or select it below. Drag a section to move it. Changes save automatically.",fontSize=12.sp,color=Color.LightGray)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected=selected=="background",onClick={selected="background"},label={Text("Background")})
                        DrizzyDesign.names.forEach{(id,label)->FilterChip(selected=selected==id,onClick={selected=id},label={Text(label)})}
                    }
                    HorizontalDivider()
                    Text(if(selected=="background")"Background" else DrizzyDesign.names[selected].orEmpty(),fontSize=19.sp,fontWeight=FontWeight.Bold)
                    if(selected=="background") {
                        ColorField("Background color",current.background){layout(current.copy(background=it))}
                        Toggle("Follow album colors",current.dynamic){layout(current.copy(dynamic=it))}
                        NumberField("Side padding",current.padding,0f..48f){layout(current.copy(padding=it))}
                        NumberField("Artwork fade",current.fade,0f..300f){layout(current.copy(fade=it))}
                        NumberField("Background blur",current.blur,0f..40f){layout(current.copy(blur=it))}
                        NumberField("Background dim",current.dim,0f..0.9f){layout(current.copy(dim=it))}
                    } else {
                        Toggle("Visible",part.visible){update(part.copy(visible=it))}
                        Row {
                            TextButton(onClick={move(-1)}){Text("Move earlier / left")}
                            TextButton(onClick={move(1)}){Text("Move later / right")}
                        }
                        NumberField("Horizontal offset",part.x,-120f..120f){update(part.copy(x=it))}
                        NumberField("Vertical offset",part.y,-120f..120f){update(part.copy(y=it))}
                        NumberField("Opacity",part.opacity,0f..1f){update(part.copy(opacity=it))}
                        if(selected in DrizzyDesign.sections) {
                            NumberField("Width",part.width,0.2f..1f){update(part.copy(width=it))}
                            NumberField("Space above",part.gap,0f..64f){update(part.copy(gap=it))}
                        }
                        if(selected in listOf("cover","lyrics"))NumberField(if(selected=="cover")"Height (0 = auto)" else "Lyrics height",part.height,0f..600f){update(part.copy(height=it))}
                        if(selected=="cover") {
                            NumberField("Corner radius",part.radius,0f..100f){update(part.copy(radius=it))}
                            Toggle("Artwork shadow",part.shadow){update(part.copy(shadow=it))}
                            Choice("Artwork fit",part.fit,listOf("crop","fit")){update(part.copy(fit=it))}
                        }
                        if(selected !in listOf("cover","song","transport","footer")) {
                            NumberField(if(selected in listOf("progress","volume"))"Track thickness"else "Text / icon size",part.size,
                                if(selected in listOf("progress","volume"))2f..24f else 8f..96f){update(part.copy(size=it))}
                            ColorField("Main color",part.color){update(part.copy(color=it))}
                        }
                        if(selected in listOf("title","artist","metadata","device","lyrics")) {
                            Toggle("Bold text / active line",part.bold){update(part.copy(bold=it))}
                            NumberField("Maximum text lines",part.lines.toFloat(),1f..4f){update(part.copy(lines=it.roundToInt()))}
                            Choice("Text alignment",part.align,listOf("start","center","end")){update(part.copy(align=it))}
                        }
                        if(selected in DrizzyDesign.sections)Choice("Component alignment",part.align,listOf("start","center","end")){update(part.copy(align=it))}
                        if(selected in listOf("progress","volume","lyrics"))ColorField(if(selected=="lyrics")"Inactive lyrics"else "Unfilled track",part.secondary){update(part.copy(secondary=it))}
                        if(selected in listOf("progress","volume")) {
                            Toggle("Show circular handle",part.thumb){update(part.copy(thumb=it))}
                            NumberField("Track corner radius",part.radius,0f..24f){update(part.copy(radius=it))}
                            if(selected=="progress")Toggle("Show timestamps",part.times){update(part.copy(times=it))}
                        }
                        if(selected in DrizzyDesign.buttons) {
                            ColorField("Button fill",part.fill){update(part.copy(fill=it))}
                            NumberField("Button corner radius",part.radius,0f..100f){update(part.copy(radius=it))}
                            Toggle("Show label",part.label){update(part.copy(label=it))}
                            Choice("Button action",part.action.ifEmpty{selected},DrizzyDesign.actions){update(part.copy(action=it))}
                        }
                        TextButton(onClick={update(DrizzyDesign.defaultPart(selected))}){Text("Reset this component")}
                    }
                    HorizontalDivider()
                    Text("Presets",fontWeight=FontWeight.Bold,fontSize=18.sp)
                    OutlinedTextField(value=presetName,onValueChange={presetName=it.take(60)},label={Text("Preset name")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    Button(onClick={
                        val name=presetName.trim();presets=presets+(name to DrizzyDesign.encode(design));presetsRaw=Json.encodeToString(presets);notice="Preset saved."
                    },enabled=presetName.isNotBlank()&&(presets.size<20||presetName.trim()in presets)){Text("Save preset (up to 20)")}
                    presets.forEach{(name,value)->Row(verticalAlignment=Alignment.CenterVertically){
                        Text(name,modifier=Modifier.weight(1f))
                        TextButton(onClick={runCatching{DrizzyDesign.decode(value)}.onSuccess{commit(it)}.onFailure{notice="Preset is invalid."}}){Text("Load")}
                        TextButton(onClick={presets=presets-name;presetsRaw=Json.encodeToString(presets)}){Text("Delete")}
                    }}
                    Row {
                        TextButton(onClick={importer.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){Text("Import JSON")}
                        TextButton(onClick={exporter.launch("drizzy-design.json")}){Text("Export JSON")}
                    }
                    TextButton(onClick={copyFromPortrait=true}){Text("Copy portrait layout to landscape")}
                    TextButton(onClick={pendingReset=true}){Text("Reset entire Drizzy design")}
                    if(notice.isNotEmpty())Text(notice,color=Color(0xFF9BD8FF),fontSize=13.sp)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if(pendingReset)AlertDialog(onDismissRequest={pendingReset=false},title={Text("Reset Drizzy?")},text={Text("Reset both orientations to the default design. Your saved presets remain available. You can undo this change.")},
            confirmButton={TextButton(onClick={commit(DrizzyDesign());pendingReset=false}){Text("Reset")}},dismissButton={TextButton(onClick={pendingReset=false}){Text("Cancel")}})
        if(copyFromPortrait)AlertDialog(onDismissRequest={copyFromPortrait=false},title={Text("Replace landscape layout?")},text={Text("Copy every portrait component setting to landscape. This change can be undone.")},
            confirmButton={TextButton(onClick={commit(design.copy(landscape=design.portrait));copyFromPortrait=false}){Text("Copy")}},dismissButton={TextButton(onClick={copyFromPortrait=false}){Text("Cancel")}})
    }
}

@Composable
private fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,modifier=Modifier.weight(1f));Switch(checked=value,onCheckedChange=onChange)}
}

@Composable
private fun NumberField(label:String,value:Float,range:ClosedFloatingPointRange<Float>,onChange:(Float)->Unit) {
    var pending by remember(value){mutableFloatStateOf(value.coerceIn(range))}
    var typed by remember(value){mutableStateOf(if(value%1f==0f)value.toInt().toString()else "%.2f".format(java.util.Locale.US,value))}
    Column {
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(label,fontSize=13.sp,modifier=Modifier.weight(1f))
            OutlinedTextField(typed,{typed=it.take(10)},singleLine=true,modifier=Modifier.width(90.dp))
            TextButton(onClick={typed.toFloatOrNull()?.takeIf{it.isFinite()}?.let{onChange(it.coerceIn(range))}}){Text("Set")}
        }
        Slider(value=pending,onValueChange={pending=it},onValueChangeFinished={onChange(pending)},valueRange=range)
    }
}

@Composable
private fun ColorField(label:String,value:String,onChange:(String)->Unit) {
    var text by remember(value){mutableStateOf(value)}
    val valid=text.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))
    Row(verticalAlignment=Alignment.CenterVertically){
        OutlinedTextField(text,{text=it.take(9)},label={Text(label)},supportingText={Text("#RRGGBB or #AARRGGBB")},isError=!valid,singleLine=true,modifier=Modifier.weight(1f))
        TextButton(onClick={onChange(text)},enabled=valid){Text("Apply")}
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choice(label:String,value:String,options:List<String>,onChange:(String)->Unit) {
    Column{Text(label,fontSize=13.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){options.forEach{option->FilterChip(selected=value==option,onClick={onChange(option)},label={Text(option)})}}}
}
