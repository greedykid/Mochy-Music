package com.mochyy.mochymusic.ui.screens.artist

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal data class ArtistWordmark(val url: String, val aspect: Float, val source: String)

internal object ArtistWordmarks {
    private const val UserAgent = "MochyWordmark/1.0 (+https://github.com/greedykid/Mochy-Music)"
    private const val Day = 86_400_000L
    private val musicBrainzLock = Mutex()
    private var lastMusicBrainzRequest = 0L

    private fun cache(context: Context) = context.getSharedPreferences("artist_wordmarks", Context.MODE_PRIVATE)

    private fun read(context: Context, id: String): JSONObject? =
        cache(context).getString(id, null)?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.takeIf { it.optLong("expires") > System.currentTimeMillis() }
        }

    private fun JSONObject.wordmark(): ArtistWordmark? {
        val url = optString("url")
        val aspect = optDouble("aspect").toFloat()
        val source = optString("source")
        if (Uri.parse(url).let { it.scheme != "https" || it.host !in setOf("upload.wikimedia.org", "thumb.wikimedia.org") } || !aspect.isFinite() || aspect <= 0 ||
            !source.startsWith("https://commons.wikimedia.org/")) return null
        return ArtistWordmark(url, aspect, source)
    }

    fun cached(context: Context, id: String): ArtistWordmark? = read(context, id)?.wordmark()

    suspend fun resolve(context: Context, id: String, name: String, albums: List<String>): ArtistWordmark? =
        withContext(Dispatchers.IO) {
            read(context, id)?.let { return@withContext it.wordmark() }
            if (name.isBlank() || albums.isEmpty()) return@withContext null
            val result = try {
                lookup(name, albums)
            } catch (_: java.io.IOException) {
                return@withContext null
            } catch (_: org.json.JSONException) {
                return@withContext null
            }
            val record = JSONObject().put("expires", System.currentTimeMillis() + if (result == null) Day else 30 * Day)
            result?.let { record.put("url", it.url).put("aspect", it.aspect.toDouble()).put("source", it.source) }
            val prefs = cache(context)
            val edit = prefs.edit()
            val excess = (prefs.all.size - 255).coerceAtLeast(0)
            prefs.all.entries.sortedBy { (_, value) ->
                runCatching { JSONObject(value as String).optLong("expires") }.getOrDefault(0L)
            }.take(excess).forEach { edit.remove(it.key) }
            edit.putString(id, record.toString()).apply()
            result
        }

    private suspend fun lookup(name: String, albums: List<String>): ArtistWordmark? {
        val query = albums.take(3).joinToString(" OR ") { "releasegroup:${quoted(it)}" }
        val matches = musicBrainz("release-group", "query" to "artist:${quoted(name)} AND ($query)", "limit" to "25")
        val expected = albums.map(::normalize).toSet()
        val candidates = mutableSetOf<String>()
        val groups = matches.getJSONArray("release-groups")
        for (i in 0 until groups.length()) {
            val group = groups.getJSONObject(i)
            if (normalize(group.optString("title")) !in expected) continue
            val credits = group.optJSONArray("artist-credit") ?: continue
            // A compilation or joint credit is weaker evidence than a solo release credit.
            if (credits.length() != 1) continue
            val artist = credits.optJSONObject(0)?.optJSONObject("artist") ?: continue
            if (normalize(artist.optString("name")) == normalize(name)) {
                artist.optString("id").takeIf { runCatching { UUID.fromString(it) }.isSuccess }?.let(candidates::add)
            }
        }
        val mbid = candidates.singleOrNull() ?: return null
        val relations = musicBrainz("artist/$mbid", "inc" to "url-rels").getJSONArray("relations")
        val entities = (0 until relations.length()).mapNotNull { index ->
            val relation = relations.getJSONObject(index)
            if (relation.optString("type") != "wikidata") return@mapNotNull null
            val url = Uri.parse(relation.optJSONObject("url")?.optString("resource").orEmpty())
            if (url.host != "www.wikidata.org" && url.host != "wikidata.org") return@mapNotNull null
            url.lastPathSegment?.takeIf { it.matches(Regex("Q[1-9][0-9]*")) }
        }
        val entity = entities.distinct().singleOrNull() ?: return null
        val claims = get("https://www.wikidata.org/w/api.php",
            "action" to "wbgetclaims", "entity" to entity, "property" to "P154", "format" to "json")
            .getJSONObject("claims").optJSONArray("P154") ?: return null
        val filenames = (0 until claims.length()).mapNotNull { index ->
            val claim = claims.getJSONObject(index)
            if (claim.optString("rank") == "deprecated") return@mapNotNull null
            val filename = claim.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optString("value").orEmpty()
            filename.takeIf { it.endsWith(".svg", true) || it.endsWith(".png", true) || it.endsWith(".webp", true) }
                ?.let { claim.optString("rank") to it }
        }
        val file = filenames.firstOrNull { it.first == "preferred" }?.second ?: filenames.firstOrNull()?.second ?: return null
        val info = get("https://commons.wikimedia.org/w/api.php", "action" to "query",
            "titles" to "File:$file", "prop" to "imageinfo", "iiprop" to "url|size|extmetadata",
            "iiurlwidth" to "800", "formatversion" to "2", "format" to "json")
            .getJSONObject("query").getJSONArray("pages").optJSONObject(0)
            ?.optJSONArray("imageinfo")?.optJSONObject(0) ?: return null
        return JSONObject()
            .put("url", info.optString("thumburl").ifBlank { info.optString("url") })
            .put("aspect", info.getDouble("width") / info.getDouble("height"))
            .put("source", info.optString("descriptionurl"))
            .wordmark()
    }

    private fun normalize(text: String) = text.trim().lowercase(java.util.Locale.ROOT)
    private fun quoted(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private suspend fun musicBrainz(path: String, vararg params: Pair<String, String>): JSONObject =
        musicBrainzLock.withLock {
            delay((1100L - (SystemClock.elapsedRealtime() - lastMusicBrainzRequest)).coerceAtLeast(0))
            try {
                get("https://musicbrainz.org/ws/2/$path", *params, "fmt" to "json")
            } finally {
                lastMusicBrainzRequest = SystemClock.elapsedRealtime()
            }
        }

    private fun get(endpoint: String, vararg params: Pair<String, String>): JSONObject {
        val uri = Uri.parse(endpoint).buildUpon().apply {
            params.forEach { (key, value) -> appendQueryParameter(key, value) }
        }.build()
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("User-Agent", UserAgent)
            if (connection.responseCode != 200) throw java.io.IOException("Wordmark lookup HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }
}
