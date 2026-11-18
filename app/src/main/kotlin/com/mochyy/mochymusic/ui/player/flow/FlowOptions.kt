package com.mochyy.mochymusic.ui.player.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FlowOptions(
    val showLyrics: Boolean = true,
    val showProgress: Boolean = true,
    val showTransport: Boolean = true,
    val showVolume: Boolean = true,
    val showButtons: Boolean = true,
    val showDevice: Boolean = true,
    val showTimes: Boolean = true,
    val remainingTime: Boolean = true,
    val lyricSize: Float = 16f,
    val trackThickness: Float = 7f,
    val volumeThickness: Float = 6f,
    val iconScale: Float = 1f,
    val transportGap: Float = 32f,
    val volumeGap: Float = 32f,
    val footerGap: Float = 20f,
    val deviceGap: Float = 16f,
    val bottomGap: Float = 12f,
    val activeColor: String = "#FFFFFF",
    val inactiveColor: String = "#55FFFFFF",
) {
    fun footerHeight(fontScale:Float=1f):Float = (if(showVolume)40f else 0f) + (if(showButtons)footerGap+48f else 0f) +
        (if(showDevice)deviceGap+18f*(if(fontScale.isFinite())fontScale.coerceAtLeast(1f)else 1f) else 0f)
    fun forHeight(height:Float):FlowOptions = if(height>=500f)this else copy(
        transportGap=transportGap.coerceAtMost(12f),volumeGap=volumeGap.coerceAtMost(12f),
        footerGap=footerGap.coerceAtMost(8f),deviceGap=deviceGap.coerceAtMost(8f),bottomGap=bottomGap.coerceAtMost(8f),iconScale=iconScale.coerceAtMost(1f))
    fun normalized(): FlowOptions {
        fun safe(v:Float,min:Float,max:Float,fallback:Float)=if(v.isFinite())v.coerceIn(min,max)else fallback
        fun color(v:String,fallback:String)=v.takeIf{it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))}?:fallback
        return copy(lyricSize=safe(lyricSize,10f,30f,16f),trackThickness=safe(trackThickness,2f,20f,7f),
            volumeThickness=safe(volumeThickness,2f,20f,6f),iconScale=safe(iconScale,.65f,1.3f,1f),
            transportGap=safe(transportGap,0f,80f,32f),volumeGap=safe(volumeGap,0f,80f,32f),
            footerGap=safe(footerGap,0f,60f,20f),deviceGap=safe(deviceGap,0f,50f,16f),bottomGap=safe(bottomGap,0f,50f,12f),
            activeColor=color(activeColor,"#FFFFFF"),inactiveColor=color(inactiveColor,"#55FFFFFF"))
    }
    companion object {
        private val json=Json{ignoreUnknownKeys=true;encodeDefaults=true}
        fun encode(value:FlowOptions)=json.encodeToString(value.normalized())
        fun decode(raw:String):FlowOptions {
            require(raw.length<32*1024)
            return json.decodeFromString<FlowOptions>(raw).normalized()
        }
    }
}
internal val FlowOptionsKey=stringPreferencesKey("flagship_flow_options_v1")
@Composable
internal fun rememberFlowOptions():FlowOptions {
    val raw by rememberPreference(FlowOptionsKey,"")
    return remember(raw){runCatching{FlowOptions.decode(raw)}.getOrDefault(FlowOptions())}
}
