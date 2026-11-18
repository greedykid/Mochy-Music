package com.mochyy.mochymusic.ui.player.flow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R
import kotlin.math.cos

/** Only mounted/animated while visible. Nothing changes the lyric strip's measured height. */
@Composable
internal fun FlowLyricPlaceholder(status: FlowLyricStatus, ink: Color, fontSize: TextUnit, animate: Boolean) {
    val label=stringResource(when(status) {
        FlowLyricStatus.LOADING -> R.string.flow_lyrics_loading
        FlowLyricStatus.WAITING -> R.string.flow_lyrics_waiting
        else -> R.string.flow_lyrics_open
    })
    val dots=status==FlowLyricStatus.LOADING || status==FlowLyricStatus.WAITING
    val phase=if(dots && animate) rememberInfiniteTransition(label="Flow lyric placeholder")
        .animateFloat(0f,1f,infiniteRepeatable(tween(1200,easing=LinearEasing)),label="Flow lyric dots") else null
    Row(verticalAlignment=Alignment.CenterVertically,
        modifier=Modifier.clearAndSetSemantics { contentDescription=label }) {
        if(status!=FlowLyricStatus.WAITING) Text(label,color=ink.copy(alpha=ink.alpha*.7f),
            fontSize=fontSize,fontWeight=FontWeight.SemiBold,maxLines=1)
        if(dots) {
            if(status==FlowLyricStatus.LOADING) Spacer(Modifier.width(8.dp))
            Canvas(Modifier.width(30.dp).height(16.dp)) {
                repeat(3) { i ->
                    val beat=if(phase==null) .55f else (.5+.5*cos(2*Math.PI*(phase.value-i*.18))).toFloat()
                    drawCircle(ink.copy(alpha=ink.alpha*(.25f+.65f*beat)),radius=2.5.dp.toPx(),
                        center=androidx.compose.ui.geometry.Offset((4+i*10).dp.toPx(),size.height/2))
                }
            }
        }
    }
}
