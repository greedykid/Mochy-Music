package com.mochyy.mochymusic.ui.player.flow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clipToBounds
import com.mochyy.mochymusic.ui.component.CircularHighlight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.lyrics.LyricsEntry
import com.mochyy.mochymusic.ui.player.PlaybackClock
import com.mochyy.mochymusic.ui.player.TickerLine
import com.mochyy.mochymusic.constants.LyricsMotionStyle
import com.mochyy.mochymusic.constants.LyricsMotionStyleKey
import com.mochyy.mochymusic.constants.LyricsHighBloomKey
import com.mochyy.mochymusic.ui.player.LyricsInterludeDots
import com.mochyy.mochymusic.ui.player.LyricRow
import com.mochyy.mochymusic.ui.player.activeInterludeOf
import com.mochyy.mochymusic.ui.player.buildLyricRows
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.ui.player.rememberMusicVolume
import com.mochyy.mochymusic.ui.player.studio.PlayerMenuAction
import com.mochyy.mochymusic.ui.player.styles.rememberStyleLyrics
import com.mochyy.mochymusic.ui.player.styles.rememberSkinAudio

internal fun flowColor(raw:String)=runCatching{Color(android.graphics.Color.parseColor(raw))}.getOrDefault(Color.White)

/** A handle-free rounded track, using the native slider's seek/volume interaction. */
@Composable
internal fun FlowTrack(fraction:()->Float,thickness:Float,active:Color,inactive:Color,modifier:Modifier=Modifier) {
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val h=thickness.dp.toPx().coerceAtMost(size.height)
        val top=(size.height-h)/2
        drawRoundRect(inactive,Offset(0f,top),Size(size.width,h),CornerRadius(h/2))
        val f=fraction().coerceIn(0f,1f)
        if(f>0)drawRoundRect(active,Offset(0f,top),Size(size.width*f,h),CornerRadius(h/2))
    }
}

@Composable
internal fun FlowLyricTicker(options:FlowOptions,active:Boolean,onOpen:()->Unit,modifier:Modifier=Modifier,preview:Boolean=false,onPageFrom:(Rect)->Unit={}) {
    if(!options.showLyrics)return
    val connection=LocalPlayerConnection.current
    val lyrics=rememberStyleLyrics(readings=active&&!preview)
    val info=connection?.uiState?.trackInfo?.collectAsState()?.value
    val lines=lyrics.timeline.takeIf{lyrics.mediaId==info?.mediaId}.orEmpty()
    // Use the same authoritative progress and offset as LyricsScreen. A line only needs the
    // 66 ms playback updates; a cached drawing clock could hold a stale line on pause/offset edits.
    val progress=connection?.progressState?.collectAsState()
    val offset=connection?.lyricsOffsetMs?.collectAsState()
    val index by remember(lines,progress,offset){derivedStateOf{
        flowLyricIndex(lines,progress?.value?.position?:0L,offset?.value?.toLong()?:0L)
    }}
    val interludes = remember(lines) { buildLyricRows(lines).filterIsInstance<LyricRow.Interlude>() }
    val gap by remember(interludes, progress, offset) { derivedStateOf {
        val position = (progress?.value?.position ?: 0L) + (offset?.value?.toLong() ?: 0L)
        val afterIndex = activeInterludeOf(interludes, position)
        interludes.firstOrNull { it.afterIndex == afterIndex }
    } }
    val status = flowLyricStatus(lyrics.loading, lines, index, preview, waiting = gap != null)
    val origin=remember{arrayOfNulls<Rect>(1)}
    val entry = if (status == FlowLyricStatus.LINE) lines.getOrNull(index.coerceAtLeast(0)) else null
    val line = if (preview) "Your current lyric line appears here" else entry?.text.orEmpty()
    val frame = FlowLyricFrame(info?.mediaId, index, line, entry, status, gap,
        LyricsUtils.lineEndMs(lines, index.coerceAtLeast(0)))
    val (motionStyle) = rememberEnumPreference(LyricsMotionStyleKey, defaultValue = LyricsMotionStyle.MOTION)
    val (highBloom) = rememberPreference(LyricsHighBloomKey, defaultValue = true)
    val clock=remember(connection){PlaybackClock({connection?.player},{connection?.lyricsOffsetMs?.value?.toLong()?:0L})}
    val open={origin[0]?.let(onPageFrom);onOpen()}
    Row(modifier.fillMaxWidth().padding(horizontal=32.dp).heightIn(min=40.dp)
        .onGloballyPositioned{if(!preview)origin[0]=it.boundsInRoot()}
        .clickable(enabled=active&&!preview,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=open),
        verticalAlignment=Alignment.CenterVertically) {
        AnimatedContent(targetState=frame,modifier=Modifier.weight(1f).height(40.dp).clipToBounds(),contentAlignment=Alignment.CenterStart,
            transitionSpec={
                if(!active||preview) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else if(initialState.status!=FlowLyricStatus.LINE || targetState.status!=FlowLyricStatus.LINE) {
                    (fadeIn(tween(160)) togetherWith fadeOut(tween(120))).using(null)
                } else {
                    val direction=if(targetState.song==initialState.song&&targetState.index<initialState.index)-1 else 1
                    val sameSong=targetState.song==initialState.song
                    val incoming=fadeIn(tween(260,easing=FastOutSlowInEasing))+
                        slideInVertically(tween(280,easing=FastOutSlowInEasing)){if(sameSong)it*direction/3 else 0}
                    val outgoing=fadeOut(tween(140))+
                        slideOutVertically(tween(220,easing=FastOutSlowInEasing)){if(sameSong)-it*direction/3 else 0}
                    (incoming togetherWith outgoing).using(null)
                }
            },label="Flow lyric line") { shown ->
            val entry=shown.entry
            val interlude = shown.gap
            if (active && !preview && interlude != null) {
                LyricsInterludeDots(startMs = interlude.startMs, endMs = interlude.endMs,
                    positionProvider = clock, alignment = com.mochyy.mochymusic.ui.player.LyricsAlignment.START,
                    bloom = motionStyle.shaded, highBloom = highBloom,
                    rowHeight = 40.dp, horizontalInset = 0.dp)
            } else if(active && !preview && entry!=null) {
                TickerLine(entry=entry,dim=false,
                    lineEndMs=shown.endMs,positionProvider=clock,
                    motionStyle=motionStyle,waveEnabled=motionStyle.shaded,highBloom=highBloom,morph=null,
                    fontSize=options.lyricSize.sp,ink=flowColor(options.activeColor),snapOnSeek=true)
            } else if(shown.status!=FlowLyricStatus.LINE && shown.status!=FlowLyricStatus.PREVIEW) {
                FlowLyricPlaceholder(shown.status,flowColor(options.activeColor),options.lyricSize.sp,active&&!preview)
            } else Text(shown.text,color=flowColor(options.activeColor),fontSize=options.lyricSize.sp,fontWeight=FontWeight.SemiBold,
                maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Box(Modifier.size(40.dp).clickable(enabled=active&&!preview,interactionSource=remember{MutableInteractionSource()},indication=CircularHighlight,onClick=open),
            contentAlignment=Alignment.Center) {
            Icon(painterResource(R.drawable.navigate_next),"Open full lyrics",tint=flowColor(options.activeColor).copy(alpha=.65f),modifier=Modifier.size(22.dp))
        }
    }
}

private data class FlowLyricFrame(val song:String?,val index:Int,val text:String,val entry:LyricsEntry?,val status:FlowLyricStatus,val gap:LyricRow.Interlude?,val endMs:Long)

internal val FlowFooterActions=listOf(PlayerMenuAction.LYRICS,PlayerMenuAction.OUTPUT,PlayerMenuAction.QUEUE)

/** Bottom volume, three independent shortcuts, then the actual selected output's name. */
@Composable
internal fun FlowBottomBar(
    options:FlowOptions,onAction:(PlayerMenuAction)->Unit,modifier:Modifier=Modifier,
    active:Boolean=true,preview:Boolean=false,onPageFrom:(Rect)->Unit={},
    isSelected:(PlayerMenuAction)->Boolean={false},
) {
    val volume=rememberMusicVolume(listening=active&&!preview)
    val audio=rememberSkinAudio(needed=options.showDevice,preview=preview)
    val ink=flowColor(options.activeColor)
    val secondary=flowColor(options.inactiveColor)
    val rects=remember{arrayOfNulls<Rect>(3)}
    Column(modifier.fillMaxWidth().padding(horizontal=32.dp)) {
        if(options.showVolume)Row(Modifier.fillMaxWidth().height(40.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(painterResource(if(volume.shown==0f)R.drawable.volume_off else R.drawable.volume_down),null,tint=ink,modifier=Modifier.size(20.dp))
            Slider(value=if(preview).6f else volume.shown,onValueChange=volume::drag,onValueChangeFinished=volume::release,
                enabled=active&&!preview,thumb={Spacer(Modifier.size(0.dp))},
                track={FlowTrack({if(preview).6f else volume.shown},options.volumeThickness,ink,secondary)},
                modifier=Modifier.weight(1f).padding(horizontal=12.dp))
            Icon(painterResource(R.drawable.volume_up),null,tint=ink,modifier=Modifier.size(22.dp))
        }
        if(options.showButtons) {
            Spacer(Modifier.height(options.footerGap.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
                FlowFooterActions.forEachIndexed { i,action ->
                    val isActive = isSelected(action)
                    val label = when (action) {
                        PlayerMenuAction.LYRICS -> if (isActive) "Close lyrics" else "Open lyrics"
                        PlayerMenuAction.OUTPUT -> "Change playback device"
                        else -> if (isActive) "Close queue" else "Open queue"
                    }
                    val interaction = remember(action) { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    val emphasis by animateFloatAsState(if (isActive) 1f else 0f, tween(180), label = "Flow shortcut active")
                    val focus by animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "Flow shortcut focus")
                    val iconModifier = Modifier.size(28.dp).graphicsLayer {
                        alpha = .72f + .28f * maxOf(emphasis, focus)
                        scaleX = 1f + .10f * emphasis + .04f * focus
                        scaleY = scaleX
                    }
                    Box(
                        Modifier.size(48.dp).onGloballyPositioned { if (!preview) rects[i] = it.boundsInRoot() }
                            .semantics { selected = isActive }
                            .clickable(enabled = active && !preview, interactionSource = interaction, indication = null) {
                                rects[i]?.let(onPageFrom)
                                onAction(action)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (action == PlayerMenuAction.OUTPUT) Icon(Icons.Rounded.Cast, label, tint = ink, modifier = iconModifier)
                        else Icon(painterResource(if (action == PlayerMenuAction.LYRICS) R.drawable.lyrics else R.drawable.apple_queue),
                            label, tint = ink, modifier = iconModifier)
                    }
                }
            }
        }
        if(options.showDevice) {
            Spacer(Modifier.height(options.deviceGap.dp))
            Text(audio?.fill("{device}").orEmpty(),color=ink.copy(alpha=.65f),fontSize=12.sp,
                maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.align(Alignment.CenterHorizontally).height((18f*LocalDensity.current.fontScale.coerceAtLeast(1f)).dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun flowBottomInset(options:FlowOptions)=WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()+options.bottomGap.dp

@Composable
internal fun flowBottomFootprint(options:FlowOptions)=flowBottomInset(options)+options.footerHeight(LocalDensity.current.fontScale).dp
