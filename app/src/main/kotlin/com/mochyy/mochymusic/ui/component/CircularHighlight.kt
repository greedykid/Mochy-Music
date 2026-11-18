package com.mochyy.mochymusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Opt-in for icon controls in native Flagship Flow, including its selector preview. */
val LocalCircularButtonFeedback = compositionLocalOf { false }

/** A centered circular wash; glyphs and hold badges are never clipped to the feedback shape. */
object CircularHighlight : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = CircularHighlightNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = javaClass.hashCode()
}

private class CircularHighlightNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val strength = Animatable(0f)
    override fun onAttach() {
        coroutineScope.launch {
            strength.snapTo(0f)
            val presses = mutableSetOf<PressInteraction.Press>()
            var focused = false
            var animation: Job? = null
            source.interactions.collect { interaction ->
                when(interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                    else -> return@collect
                }
                animation?.cancel()
                val on = presses.isNotEmpty() || focused
                val quickTap = interaction is PressInteraction.Release
                animation = launch {
                    if(on) strength.animateTo(1f,tween(80))
                    else {
                        if(quickTap && strength.value < .65f) strength.animateTo(.65f,tween(50))
                        strength.animateTo(0f,tween(240))
                    }
                }
            }
        }
    }
    override fun ContentDrawScope.draw() {
        if(strength.value > 0f) drawCircle(Color.White.copy(alpha=.16f*strength.value),radius=size.minDimension/2f)
        drawContent()
    }
}
