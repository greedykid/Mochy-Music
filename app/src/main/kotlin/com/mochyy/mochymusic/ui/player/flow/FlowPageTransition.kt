package com.mochyy.mochymusic.ui.player.flow

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.ui.player.letTouchesThroughWhenHidden

/** Flow has a lyric strip, not a card: no clipping into a temporary pill on close. */
internal fun Modifier.flowPageTransition(progress: () -> Float): Modifier = graphicsLayer {
    val p = progress().coerceIn(0f, 1f)
    alpha = p
    translationY = (1f - p) * 32.dp.toPx()
    letTouchesThroughWhenHidden()
}
