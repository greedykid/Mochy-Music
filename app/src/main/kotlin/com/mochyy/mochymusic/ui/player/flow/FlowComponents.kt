package com.mochyy.mochymusic.ui.player.flow

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.ui.player.rememberMusicVolume
import com.mochyy.mochymusic.ui.player.studio.PlayerMenuAction
import com.mochyy.mochymusic.ui.player.styles.rememberLivePosition
import com.mochyy.mochymusic.ui.player.styles.rememberStyleLyrics
import com.mochyy.mochymusic.ui.player.styles.rememberSkinAudio
import com.mochyy.mochymusic.ui.player.styles.withLyricsOffset

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
internal fun FlowLyricTicker(options:FlowOptions,active:Boolean,onOpen:()->Unit,modifier:Modifier=Modifier,preview:Boolean=false) {
    if(!options.showLyrics)return
    val connection=LocalPlayerConnection.current
    val lyrics=rememberStyleLyrics(readings=active&&!preview)
    val info=connection?.uiState?.trackInfo?.collectAsState()?.value
    val lines=lyrics.lines.takeIf{lyrics.mediaId==info?.mediaId}.orEmpty()
    val clock=rememberLivePosition(animated=active&&!preview).withLyricsOffset()
    val index by remember(lines,clock){derivedStateOf{LyricsUtils.findCurrentLineIndex(lines,clock.positionMs())}}
    val line=if(preview)"Your current lyric line appears here" else lines.getOrNull(index)?.text?:"Open lyrics"
    Row(modifier.fillMaxWidth().padding(horizontal=32.dp).heightIn(min=40.dp).clickable(enabled=!preview,onClick=onOpen),verticalAlignment=Alignment.CenterVertically) {
        Text(line,color=flowColor(options.activeColor),fontSize=options.lyricSize.sp,fontWeight=FontWeight.SemiBold,
            maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
        Icon(painterResource(R.drawable.navigate_next),"Open full lyrics",tint=flowColor(options.activeColor).copy(alpha=.65f),modifier=Modifier.padding(start=12.dp).size(22.dp))
    }
}

internal val FlowFooterActions=listOf(PlayerMenuAction.LYRICS,PlayerMenuAction.OUTPUT,PlayerMenuAction.QUEUE)

/** Bottom volume, three independent shortcuts, then the actual selected output's name. */
@Composable
internal fun FlowBottomBar(
    options:FlowOptions,onAction:(PlayerMenuAction)->Unit,modifier:Modifier=Modifier,
    active:Boolean=true,preview:Boolean=false,onPageFrom:(Rect)->Unit={},
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
                    val label=when(i){0->"Open lyrics";1->"Change playback device";else->"Open queue"}
                    Box(Modifier.size(48.dp).onGloballyPositioned{if(!preview)rects[i]=it.boundsInRoot()}
                        .clickable(enabled=active&&!preview){rects[i]?.let(onPageFrom);onAction(action)},contentAlignment=Alignment.Center) {
                        if(i==1)Icon(Icons.Rounded.Cast,label,tint=ink.copy(alpha=.85f),modifier=Modifier.size(28.dp))
                        else Icon(painterResource(if(i==0)R.drawable.lyrics else R.drawable.apple_queue),label,tint=ink.copy(alpha=.85f),modifier=Modifier.size(28.dp))
                    }
                }
            }
        }
        if(options.showDevice) {
            Spacer(Modifier.height(options.deviceGap.dp))
            Text(audio?.fill("{device}").orEmpty(),color=ink.copy(alpha=.65f),fontSize=12.sp,
                maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.align(Alignment.CenterHorizontally).height(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun flowBottomInset(options:FlowOptions)=WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()+options.bottomGap.dp

@Composable
internal fun flowBottomFootprint(options:FlowOptions)=flowBottomInset(options)+options.footerHeight().dp
