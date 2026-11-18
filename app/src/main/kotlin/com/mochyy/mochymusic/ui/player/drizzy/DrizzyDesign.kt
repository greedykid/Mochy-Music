package com.mochyy.mochymusic.ui.player.drizzy

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Versioned, portable design. Every orientation has its own complete layout. */
@Serializable
data class DrizzyPart(
    val visible: Boolean = true,
    val width: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val gap: Float = 12f,
    val size: Float = 20f,
    val height: Float = 108f,
    val radius: Float = 0f,
    val opacity: Float = 1f,
    val color: String = "#FFFFFF",
    val secondary: String = "#80FFFFFF",
    val fill: String = "#00000000",
    val align: String = "start",
    val bold: Boolean = false,
    val lines: Int = 2,
    val lineGap: Float = 10f,
    val thumb: Boolean = false,
    val times: Boolean = true,
    val label: Boolean = false,
    val action: String = "",
    val fit: String = "crop",
    val shadow: Boolean = false,
)

@Serializable
data class DrizzyLayout(
    val order: List<String> = DrizzyDesign.sections,
    val transport: List<String> = listOf("back10", "previous", "play", "next", "forward10"),
    val footer: List<String> = listOf("lyricsButton", "deviceButton", "queueButton"),
    val songText: List<String> = listOf("title", "artist"),
    val songButtons: List<String> = listOf("love", "menu"),
    val parts: Map<String, DrizzyPart> = emptyMap(),
    val padding: Float = 22f,
    val background: String = "#484848",
    val dynamic: Boolean = false,
    val blur: Float = 0f,
    val dim: Float = 0.25f,
    val fade: Float = 120f,
) {
    fun part(id: String): DrizzyPart = parts[id] ?: DrizzyDesign.defaultPart(id)
    fun change(id: String, part: DrizzyPart) = copy(parts = parts + (id to part))
}

@Serializable
data class DrizzyDesign(
    val version: Int = 1,
    val portrait: DrizzyLayout = DrizzyLayout(),
    val landscape: DrizzyLayout = DrizzyLayout(parts = mapOf("cover" to DrizzyPart(height = 260f, gap = 0f))),
) {
    fun layout(wide: Boolean) = if (wide) landscape else portrait
    fun change(wide: Boolean, layout: DrizzyLayout) = if (wide) copy(landscape = layout) else copy(portrait = layout)

    companion object {
        val sections = listOf("cover", "metadata", "song", "progress", "transport", "lyrics", "volume", "footer", "device")
        val names = linkedMapOf(
            "cover" to "Artwork", "metadata" to "Audio quality", "song" to "Song information",
            "title" to "Song title", "artist" to "Artist", "love" to "Favorite button", "menu" to "Menu button",
            "progress" to "Duration bar", "transport" to "Playback controls", "back10" to "Back 10 seconds",
            "previous" to "Previous", "play" to "Play / pause", "next" to "Next", "forward10" to "Forward 10 seconds",
            "lyrics" to "Lyrics", "volume" to "Volume bar", "footer" to "Utility buttons",
            "lyricsButton" to "Full lyrics button", "deviceButton" to "Output button", "queueButton" to "Queue button", "device" to "Device name",
        )
        val actions = listOf("back10", "previous", "play", "next", "forward10", "like", "lyrics", "queue", "device", "shuffle", "repeat", "menu", "edit")
        val buttons = setOf("love", "menu", "back10", "previous", "play", "next", "forward10", "lyricsButton", "deviceButton", "queueButton")
        fun defaultPart(id: String): DrizzyPart = when (id) {
            "cover" -> DrizzyPart(height = 0f, gap = 0f)
            "metadata" -> DrizzyPart(size = 10f, align = "center", gap = 8f, color = "#CEFFFFFF")
            "title" -> DrizzyPart(size = 25f, bold = true, lines = 2, gap = 0f)
            "artist" -> DrizzyPart(size = 18f, color = "#C8C8C8", lines = 1, gap = 4f)
            "song" -> DrizzyPart(gap = 12f)
            "progress", "volume" -> DrizzyPart(size = 8f, radius = 8f, gap = 16f)
            "transport" -> DrizzyPart(gap = 12f)
            "play" -> DrizzyPart(size = 64f, color = "#000000", fill = "#FFFFFF", radius = 100f, action = "play")
            "previous", "next" -> DrizzyPart(size = 40f, action = id)
            "back10", "forward10" -> DrizzyPart(size = 28f, action = id)
            "love" -> DrizzyPart(size = 26f, action = "like")
            "menu" -> DrizzyPart(size = 26f, action = "menu")
            "lyricsButton" -> DrizzyPart(size = 26f, action = "lyrics")
            "deviceButton" -> DrizzyPart(size = 28f, action = "device")
            "queueButton" -> DrizzyPart(size = 26f, action = "queue")
            "lyrics" -> DrizzyPart(size = 20f, height = 112f, gap = 16f, bold = true)
            "device" -> DrizzyPart(size = 11f, color = "#BFFFFFFF", align = "center", gap = 8f)
            else -> DrizzyPart()
        }
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
        fun encode(design: DrizzyDesign): String = json.encodeToString(design)
        fun decode(raw: String): DrizzyDesign {
            require(raw.toByteArray().size <= 256 * 1024) { "Design file is too large (256 KB maximum)." }
            val value = json.decodeFromString<DrizzyDesign>(raw)
            require(value.version == 1) { "This design uses an unsupported version." }
            return value.copy(portrait = normalize(value.portrait), landscape = normalize(value.landscape))
        }
        private fun number(value: Float, range: ClosedFloatingPointRange<Float>, fallback: Float): Float =
            if (value.isFinite()) value.coerceIn(range) else fallback
        private fun color(value: String, fallback: String) = value.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) } ?: fallback
        fun normalize(layout: DrizzyLayout): DrizzyLayout {
            fun ordered(input: List<String>, allowed: List<String>) = input.filter { it in allowed }.distinct() + allowed.filter { it !in input }
            return layout.copy(
                order = ordered(layout.order, sections),
                transport = ordered(layout.transport, listOf("back10", "previous", "play", "next", "forward10")),
                footer = ordered(layout.footer, listOf("lyricsButton", "deviceButton", "queueButton")),
                songText = ordered(layout.songText, listOf("title", "artist")),
                songButtons = ordered(layout.songButtons, listOf("love", "menu")),
                padding = number(layout.padding, 0f..48f, 22f), fade = number(layout.fade, 0f..300f, 120f),
                blur = number(layout.blur, 0f..40f, 0f), dim = number(layout.dim, 0f..0.9f, 0.25f), background = color(layout.background, "#484848"),
                parts = layout.parts.filterKeys { it in names }.mapValues { (id, p) ->
                    val d = defaultPart(id)
                    p.copy(width = number(p.width, 0.2f..1f, 1f), x = number(p.x, -120f..120f, 0f), y = number(p.y, -120f..120f, 0f),
                        gap = number(p.gap, 0f..64f, d.gap), size = number(p.size, 2f..96f, d.size), height = number(p.height, 0f..600f, d.height),
                        radius = number(p.radius, 0f..100f, 0f), opacity = number(p.opacity, 0f..1f, 1f), lines = p.lines.coerceIn(1,4),
                        lineGap = number(p.lineGap, 0f..40f, 10f),
                        color = color(p.color,d.color), secondary = color(p.secondary,d.secondary), fill = color(p.fill,d.fill),
                        align = p.align.takeIf { it in listOf("start","center","end") } ?: "start",
                        fit = p.fit.takeIf { it in listOf("crop","fit") } ?: "crop", action = p.action.takeIf { it in actions } ?: d.action)
                },
            )
        }
    }
}

/** Bounded edit history: one entry per completed gesture or setting change. */
class DrizzyHistory(initial: DrizzyDesign) {
    var current: DrizzyDesign = initial
        private set
    private val undo = ArrayDeque<DrizzyDesign>()
    private val redo = ArrayDeque<DrizzyDesign>()
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()
    fun commit(value: DrizzyDesign) {
        if (value == current) return
        undo.addLast(current)
        if (undo.size > 80) undo.removeFirst()
        redo.clear(); current = value
    }
    fun undo(): DrizzyDesign { if (canUndo) { redo.addLast(current); current = undo.removeLast() }; return current }
    fun redo(): DrizzyDesign { if (canRedo) { undo.addLast(current); current = redo.removeLast() }; return current }
}
