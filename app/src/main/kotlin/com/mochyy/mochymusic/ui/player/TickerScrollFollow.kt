package com.mochyy.mochymusic.ui.player

import kotlin.math.abs
import kotlin.math.exp

/** Pixel geometry measured once from the displayed text, with real provider timestamps. */
internal data class TickerWordBounds(
    val startMs: Long, val endMs: Long, val left: Float, val right: Float, val rtl: Boolean = false,
)

internal data class TickerScrollPlan(
    val target: Float, val minimum: Float, val maximum: Float, val followSeconds: Float,
)

/** Look ahead without sacrificing visibility of the word that is actually being sung. */
internal fun tickerScrollPlan(
    words: List<TickerWordBounds>, positionMs: Long, viewport: Float, textWidth: Float,
): TickerScrollPlan {
    val overflow = (textWidth - viewport).coerceAtLeast(0f)
    if (viewport <= 0f || overflow == 0f || words.isEmpty()) return TickerScrollPlan(0f, 0f, overflow, .13f)
    val index = words.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
    val current = words[index]
    val duration = (current.endMs - current.startMs).coerceAtLeast(1L)
    val leadMs = (duration * .65f).toLong().coerceIn(70L, 160L)
    val future = words.indexOfLast { it.startMs <= positionMs + leadMs }.coerceAtLeast(index)
    val next = words[future]
    val pad = minOf(16f, viewport * .06f, ((viewport-(current.right-current.left))/2f).coerceAtLeast(0f))
    val fraction = ((positionMs - current.startMs).toFloat() / duration).coerceIn(0f, 1f)
    val head = if (current.rtl) current.right - (current.right-current.left)*fraction
        else current.left + (current.right-current.left)*fraction
    val focus = if (current.rtl) viewport*.58f else viewport*.42f
    var target = head - focus
    target = if (current.rtl) minOf(target, next.left - viewport*.14f)
        else maxOf(target, next.right - viewport*.86f)
    if (future == words.lastIndex && next.right-next.left<=viewport) target = if (next.rtl) 0f else overflow
    // Very long words cannot fit whole: follow the sung part instead of oscillating between edges.
    val fits = current.right-current.left <= viewport
    val minimum = (if(fits) current.right+pad-viewport else head-viewport*.85f).coerceIn(0f,overflow)
    val maximum = (if(fits) current.left-pad else head-viewport*.15f).coerceIn(minimum,overflow)
    val response = when { duration < 200 -> .035f; duration < 400 -> .055f; else -> .13f }
    return TickerScrollPlan(target.coerceIn(minimum,maximum),minimum,maximum,response)
}

/** Frame-rate independent follow, bounded so a fast word never waits behind the viewport edge. */
internal class TickerScrollFollower {
    var value = 0f; private set
    private var velocity = 0f
    private var initialized = false
    fun advance(plan: TickerScrollPlan, dtSeconds: Float, viewport: Float, seek: Boolean = false): Float {
        if (!initialized || seek) {
            initialized = true; value = plan.target; velocity = 0f
        } else {
            val response = if(abs(value-plan.target)>viewport*.2f) minOf(plan.followSeconds,.04f) else plan.followSeconds
            val omega = 1f/response
            val dt = dtSeconds.coerceIn(0f,.064f)
            val y = value-plan.target
            val e = exp(-omega*dt)
            val tmp = (velocity+omega*y)*dt
            value = plan.target+(y+tmp)*e
            velocity = (velocity-omega*tmp)*e
        }
        val bounded = value.coerceIn(plan.minimum,plan.maximum)
        if(bounded!=value) velocity=0f
        value=bounded
        if(abs(value-plan.target)<.05f && abs(velocity)<1f) { value=plan.target; velocity=0f }
        return value
    }
}

/** A ticker must finish quick end words; the full page's 300 ms animation lead-in is unsuitable. */
internal fun synchronizedTickerIndex(lines: List<com.mochyy.mochymusic.lyrics.LyricsEntry>, positionMs: Long): Int {
    if(lines.isEmpty()) return -1
    if(positionMs >= com.mochyy.mochymusic.lyrics.LyricsUtils.lyricsEndMs(lines)) return lines.size
    return lines.indexOfLast { it.time <= positionMs }
}
