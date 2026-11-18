package com.mochyy.mochymusic.ui.player.styles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.ui.player.drizzy.*
import com.mochyy.mochymusic.ui.player.flagship.AnimatedPlayGlyph
import com.mochyy.mochymusic.ui.player.flagship.AnimatedSkipGlyph
import com.mochyy.mochymusic.ui.player.flagship.SkipCycleState
import com.mochyy.mochymusic.ui.player.menu.DeviceOutputSheet
import com.mochyy.mochymusic.ui.player.rememberMusicVolume
import com.mochyy.mochymusic.utils.makeTimeString
import kotlinx.coroutines.launch

internal fun drizzyColor(raw: String): Color = runCatching { Color(android.graphics.Color.parseColor(raw)) }.getOrDefault(Color.White)

/** Native controls and timed lyrics, presented using a separate, portable Drizzy design. */
@Composable
internal fun DrizzyPlayerStyle(
    actions: StyleActions, modifier: Modifier = Modifier, preview: Boolean = false,
    designOverride: DrizzyDesign? = null, orientationOverride: Boolean? = null,
    selected: String? = null, onSelect: ((String) -> Unit)? = null,
    onMove: ((String, Float, Float) -> Unit)? = null,
) {
    val design = designOverride ?: rememberDrizzyDesign()
    val track = rememberStyleTrack()
    val connection = LocalPlayerConnection.current
    val tones = rememberStyleTones()
    val position = rememberLivePosition(animated = !preview)
    val duration = rememberDurationMs()
    val seconds = rememberPlayedSeconds(position)
    val lyrics = rememberStyleLyrics(readings = !preview)
    val audio = rememberSkinAudio(needed = true, preview = preview)
    val showing = rememberStyleShowing()
    val volume = rememberMusicVolume(listening = !preview && showing())
    var output by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf(false) }
    val interactive = LocalStyleInteractive.current
    val transport = connection?.uiState?.transportState?.collectAsState()?.value
    val info = connection?.uiState?.trackInfo?.collectAsState()?.value
    val lines = lyrics.lines.takeIf { lyrics.mediaId == info?.mediaId }.orEmpty()
    val previewLines = remember { listOf(LyricsEntry(0,"The lights are fading slowly"), LyricsEntry(84000,"I hear the night around me"), LyricsEntry(110000,"And let the silence carry on")) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val wide = orientationOverride ?: (screenWidth > screenHeight)
        val layout = design.layout(wide)
        val base = if (layout.dynamic) tones.remaining else drizzyColor(layout.background)
        val density = LocalDensity.current
        val perform: (String) -> Unit = perform@{ action ->
            if (preview || !interactive()) return@perform
            val player = connection?.player
            when (action) {
                "play" -> if (transport?.playbackState == Player.STATE_ENDED) { connection?.seekTo(0); player?.play() } else connection?.togglePlayPause()
                "previous" -> connection?.seekToPrevious()
                "next" -> connection?.seekToNext()
                "back10" -> connection?.seekTo(((player?.currentPosition ?: 0)-10_000).coerceAtLeast(0))
                "forward10" -> if (duration > 1) connection?.seekTo(((player?.currentPosition ?: 0)+10_000).coerceIn(0,duration))
                "like" -> connection?.toggleLike()
                "lyrics" -> actions.onLyrics?.invoke()
                "queue" -> actions.onQueue?.invoke()
                "device" -> output = true
                "shuffle" -> player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
                "repeat" -> player?.toggleRepeatMode()
                "menu" -> actions.onOtherMenu()
                "edit" -> editor = true
            }
        }
        Box(Modifier.fillMaxSize().background(base)) {
            if (layout.blur > 0) {
                AsyncImage(track.artwork, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(layout.blur.dp).alpha(1-layout.dim))
                Box(Modifier.fillMaxSize().background(base.copy(alpha=layout.dim)))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                layout.order.forEach { id ->
                    val part = layout.part(id)
                    if (part.visible || onSelect != null) {
                        var dragX by remember(id) { mutableFloatStateOf(0f) }
                        var dragY by remember(id) { mutableFloatStateOf(0f) }
                        val anchor = when(part.align) { "center" -> Alignment.Center; "end" -> Alignment.CenterEnd; else -> Alignment.CenterStart }
                        Box(Modifier.fillMaxWidth().padding(horizontal=if(id=="cover")0.dp else layout.padding.dp).padding(top=part.gap.dp),contentAlignment=anchor) {
                            val edit = if(onSelect != null) Modifier
                                .border(if(selected==id)2.dp else .5.dp,if(selected==id)Color(0xFF93DBFF)else Color.White.copy(alpha=.2f))
                                .pointerInput(id,onSelect){detectTapGestures(onTap={onSelect(id)})}
                                .pointerInput(id,part.x,part.y){
                                    detectDragGestures(onDragStart={onSelect(id)},onDragCancel={dragX=0f;dragY=0f},onDragEnd={
                                        onMove?.invoke(id,part.x+dragX,part.y+dragY);dragX=0f;dragY=0f
                                    }){change,amount->change.consume();dragX+=amount.x/density.density;dragY+=amount.y/density.density}
                                } else Modifier
                            Box(Modifier.fillMaxWidth(part.width).offset((part.x+dragX).dp,(part.y+dragY).dp)
                                .alpha(if(part.visible)part.opacity else .25f).then(edit)) {
                                when(id) {
                                    "cover" -> Box {
                                        val height=if(part.height>0)part.height.dp else minOf(screenWidth,screenHeight*0.42f)
                                        AsyncImage(track.artwork,"Album artwork",contentScale=if(part.fit=="fit")ContentScale.Fit else ContentScale.Crop,
                                            modifier=Modifier.fillMaxWidth().height(height)
                                                .then(if(part.shadow)Modifier.shadow(16.dp,RoundedCornerShape(part.radius.dp))else Modifier)
                                                .clip(RoundedCornerShape(part.radius.dp)))
                                        if(layout.fade>0)Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(layout.fade.dp.coerceAtMost(height))
                                            .background(Brush.verticalGradient(listOf(Color.Transparent,base))))
                                    }
                                    "metadata" -> DrizzyText(audio?.fill("{codec} · {khz} kHz · {kbps} kbps").orEmpty(),part)
                                    "song" -> Row(verticalAlignment=Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            for((name,value)in listOf("title" to track.title,"artist" to track.artist)) {
                                                val p=layout.part(name)
                                                if(p.visible)DrizzyText(value,p,Modifier.padding(top=p.gap.dp).offset(p.x.dp,p.y.dp).alpha(p.opacity)
                                                    .then(if(onSelect!=null)Modifier.clickable{onSelect(name)}else Modifier))
                                            }
                                        }
                                        listOf("love","menu").forEach { name -> DrizzyButton(name,layout.part(name),transport?.isPlaying==true,info?.liked==true,preview,onSelect,perform) }
                                    }
                                    "progress" -> Column {
                                        DrizzyBar(part,{(position.positionMs().toFloat()/duration).coerceIn(0f,1f)},"Song position",!preview&&onSelect==null&&duration>1,
                                            onChange={connection?.seekTo((it*duration).toLong().coerceIn(0,duration))})
                                        if(part.times)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                            Text(makeTimeString(seconds*1000),color=drizzyColor(part.secondary),fontSize=11.sp)
                                            Text("−"+makeTimeString((duration-seconds*1000).coerceAtLeast(0)),color=drizzyColor(part.secondary),fontSize=11.sp)
                                        }
                                    }
                                    "transport","footer" -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                                        val ordered=if(id=="transport")layout.transport else layout.footer
                                        ordered.filter{layout.part(it).visible}.forEach{name->
                                            Box(Modifier.weight(if(name=="play")1.3f else 1f),contentAlignment=Alignment.Center){
                                                DrizzyButton(name,layout.part(name),transport?.isPlaying==true,info?.liked==true,preview,onSelect,perform)
                                            }
                                        }
                                    }
                                    "lyrics" -> Column {
                                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                            Text("LYRICS",fontSize=10.sp,color=drizzyColor(part.color),modifier=Modifier.weight(1f))
                                            Icon(painterResource(R.drawable.fullscreen),"Open full lyrics",tint=drizzyColor(part.color),
                                                modifier=Modifier.size(44.dp).clickable(enabled=!preview&&onSelect==null){perform("lyrics")}.padding(10.dp))
                                        }
                                        DrizzyLyrics(if(preview)previewLines else lines,part,position,preview,duration,onSelect==null){time->
                                            if(!preview&&interactive())connection?.let{it.seekTo((time-it.lyricsOffsetMs.value).coerceIn(0,duration))}
                                        }
                                    }
                                    "volume" -> Row(verticalAlignment=Alignment.CenterVertically) {
                                        Icon(painterResource(R.drawable.volume_down),"Lower volume",tint=drizzyColor(part.color),modifier=Modifier.size(24.dp))
                                        Spacer(Modifier.width(10.dp))
                                        DrizzyBar(part,{if(preview).6f else volume.shown},"Media volume",!preview&&onSelect==null,Modifier.weight(1f),
                                            onChange={volume.drag(it)},onFinish=volume::release)
                                        Spacer(Modifier.width(10.dp))
                                        Icon(painterResource(R.drawable.volume_up),"Higher volume",tint=drizzyColor(part.color),modifier=Modifier.size(24.dp))
                                    }
                                    "device" -> DrizzyText(audio?.fill("{device}").orEmpty(),part)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            // A recovery route remains available after hiding/moving every editable component.
            if(!preview)Icon(painterResource(R.drawable.tune),"Customize Drizzy",tint=Color.White,
                modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp).background(Color.Black.copy(alpha=.4f),RoundedCornerShape(24.dp))
                    .size(44.dp).clickable(enabled=interactive()){editor=true}.padding(11.dp))
        }
    }
    if(output&&!preview)DeviceOutputSheet{output=false}
    if(editor&&!preview)DrizzyEditor(actions=actions,onDismiss={editor=false})
}

@Composable
private fun DrizzyText(value:String,part:DrizzyPart,modifier:Modifier=Modifier) {
    Text(value,modifier.fillMaxWidth(),color=drizzyColor(part.color),fontSize=part.size.coerceIn(8f,96f).sp,
        fontWeight=if(part.bold)FontWeight.Bold else FontWeight.Normal,maxLines=part.lines,overflow=TextOverflow.Ellipsis,
        textAlign=when(part.align){"center"->TextAlign.Center;"end"->TextAlign.End;else->TextAlign.Start})
}

@Composable
private fun DrizzyButton(id:String,part:DrizzyPart,playing:Boolean,liked:Boolean,preview:Boolean,onSelect:((String)->Unit)?,perform:(String)->Unit) {
    if(!part.visible)return
    val action=part.action.ifEmpty{id}
    val tint=drizzyColor(part.color)
    val cycle=remember{SkipCycleState()}
    val scope=rememberCoroutineScope()
    val touch=part.size.coerceAtLeast(48f).dp
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(touch).offset(part.x.dp,part.y.dp).alpha(part.opacity)) {
        Box(Modifier.size(touch).clip(RoundedCornerShape(part.radius.dp)).background(drizzyColor(part.fill))
            .clickable(enabled=!preview||onSelect!=null){if(onSelect!=null)onSelect(id)else{
                if(action=="previous"||action=="next")scope.launch{cycle.step()};perform(action)
            }}.semantics{contentDescription=DrizzyDesign.names[id]?:action},contentAlignment=Alignment.Center) {
            val iconSize=part.size.dp
            when(action) {
                "play"->AnimatedPlayGlyph(playing,tint,iconSize*.48f)
                "previous","next"->AnimatedSkipGlyph(cycle,action=="previous",tint,iconSize)
                "back10","forward10"->Icon(painterResource(R.drawable.flagship_ten_seconds),null,tint=tint,
                    modifier=Modifier.size(iconSize))
                else->Icon(painterResource(when(action){
                    "like"->if(liked)R.drawable.favorite else R.drawable.favorite_border
                    "lyrics"->R.drawable.lyrics;"queue"->R.drawable.queue_music;"device"->R.drawable.headset_applemusic
                    "shuffle"->R.drawable.shuffle;"repeat"->R.drawable.repeat;"edit"->R.drawable.tune;else->R.drawable.more_horiz
                }),null,tint=tint,modifier=Modifier.size(iconSize))
            }
        }
        if(part.label)Text(DrizzyDesign.names[id]?:action,fontSize=9.sp,color=tint,maxLines=1)
    }
}

/** Track thickness is independent of the accessible 44dp touch target. */
@Composable
private fun DrizzyBar(part:DrizzyPart,fraction:()->Float,label:String,enabled:Boolean,modifier:Modifier=Modifier,onChange:(Float)->Unit,onFinish:()->Unit={}) {
    val interactive=LocalStyleInteractive.current
    val change by rememberUpdatedState(onChange)
    val finish by rememberUpdatedState(onFinish)
    var dragged by remember{mutableStateOf<Float?>(null)}
    val gestures=if(enabled)Modifier.pointerInput(enabled,interactive){
        detectTapGestures{if(interactive()){change((it.x/size.width.coerceAtLeast(1)).coerceIn(0f,1f));finish()}}
    }.pointerInput(enabled,interactive){
        detectDragGestures(onDragEnd={finish();dragged=null},onDragCancel={finish();dragged=null}){event,_->
            if(interactive()){event.consume();val v=(event.position.x/size.width.coerceAtLeast(1)).coerceIn(0f,1f);dragged=v;change(v)}
        }
    }else Modifier
    Canvas(modifier.fillMaxWidth().height(44.dp).then(gestures).semantics{
        contentDescription=label;progressBarRangeInfo=ProgressBarRangeInfo(fraction(),0f..1f)
        if(enabled)setProgress{v->if(interactive()){change(v.coerceIn(0f,1f));finish();true}else false}
    }){
        val f=(dragged?:fraction()).coerceIn(0f,1f)
        val h=part.size.dp.toPx().coerceAtMost(size.height)
        val r=CornerRadius(part.radius.dp.toPx().coerceAtMost(h/2))
        val top=(size.height-h)/2
        drawRoundRect(drizzyColor(part.secondary),Offset(0f,top),Size(size.width,h),r)
        if(f>0)drawRoundRect(drizzyColor(part.color),Offset(0f,top),Size(size.width*f,h),r)
        if(part.thumb)drawCircle(drizzyColor(part.color),h*.75f,Offset((size.width*f).coerceIn(0f,size.width),size.height/2))
    }
}

@Composable
private fun DrizzyLyrics(lines:List<LyricsEntry>,part:DrizzyPart,position:LivePosition,preview:Boolean,duration:Long,clickable:Boolean,onSeek:(Long)->Unit) {
    val clock=position.withLyricsOffset()
    val current by remember(lines,clock,preview){derivedStateOf{if(preview)1 else LyricsUtils.findCurrentLineIndex(lines,clock.positionMs())}}
    val state=androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(current,lines){if(lines.isNotEmpty())state.animateScrollToItem((current-1).coerceIn(0,lines.lastIndex))}
    if(lines.isEmpty())Text("No synced lyrics available. Open full lyrics to choose a source.",color=drizzyColor(part.secondary),fontSize=14.sp,
        modifier=Modifier.heightIn(min=part.height.coerceAtLeast(60f).dp))
    else androidx.compose.foundation.lazy.LazyColumn(state=state,userScrollEnabled=false,verticalArrangement=Arrangement.spacedBy(10.dp),
        modifier=Modifier.fillMaxWidth().height(part.height.coerceAtLeast(60f).dp)){
        items(lines.size,key={it}){i->
            val active=i==current
            Text(lines[i].text,color=drizzyColor(if(active)part.color else part.secondary),fontSize=part.size.coerceIn(8f,96f).sp,
                fontWeight=if(active&&part.bold)FontWeight.Bold else FontWeight.Normal,maxLines=part.lines,overflow=TextOverflow.Ellipsis,
                textAlign=when(part.align){"center"->TextAlign.Center;"end"->TextAlign.End;else->TextAlign.Start},
                modifier=Modifier.fillMaxWidth().clickable(enabled=!preview&&clickable&&duration>1){onSeek(lines[i].time)}.padding(vertical=4.dp))
        }
    }
}
