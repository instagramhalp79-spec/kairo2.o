package com.example.core.youtube

import android.util.Log
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object YouTubeMediaResolver {
    private const val TAG = "KAIRO_YT_RESOLVER"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Search REAL YouTube WITHOUT any YouTube Data API Key
     * Uses Direct YouTube InnerTube Client API (Zero API key required)
     */
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val trimmed = query.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return@withContext emptyList()

        Log.d(TAG, "[KAIRO][YT_SEARCH] Query: $trimmed")
        val realResults = mutableListOf<Song>()

        try {
            val payload = JSONObject().apply {
                val context = JSONObject().apply {
                    val client = JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                        put("hl", "en")
                        put("gl", "IN")
                    }
                    put("client", client)
                }
                put("context", context)
                put("query", trimmed)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    parseInnerTubeSearchResults(json, realResults)
                    Log.d(TAG, "[KAIRO][YT_SEARCH] InnerTube returned ${realResults.size} real results")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[KAIRO][YT_SEARCH] InnerTube query error: ${e.message}")
        }

        val finalResults = realResults.distinctBy { it.videoId ?: it.id }
        Log.d(TAG, "[KAIRO][YT_SEARCH] Finished with ${finalResults.size} distinct results")
        finalResults
    }

    /**
     * Determine genuine related songs based on the currently playing song (Spotify / YouTube Music style).
     * Considers:
     * - Same artist
     * - Similar genre & musical style
     * - Real YouTube search recommendations for the artist/song
     * Preserves exact videoId for each related song.
     */
    fun getNormalizedBaseTitle(title: String): String {
        var t = title.lowercase()
        // Strip common YouTube tags in brackets/parentheses
        t = t.replace(Regex("\\s*[\\[\\(].*?[\\]\\)]"), " ")
        // Split by separator like '|' or '-' if it contains artist/channel tag
        if (t.contains("|")) {
            t = t.split("|")[0]
        }
        // Remove common clutter words
        t = t.replace(Regex("\\b(official|video|audio|lyric|lyrics|full|song|hd|4k|remix|slowed|reverb|version)\\b"), " ")
        // Keep alphanumeric and devanagari characters
        val alphaNum = t.replace(Regex("[^a-zA-Z0-9\\u0900-\\u097F]"), " ").trim()
        val collapsed = alphaNum.replace(Regex("\\s+"), " ")
        return if (collapsed.length >= 2) collapsed else title.trim().lowercase()
    }

    /**
     * Determine genuine related songs based on the currently playing song (Spotify / YouTube Music style).
     * Considers:
     * - Same artist
     * - Similar genre & musical style
     * - Real YouTube search recommendations for the artist/song
     * Preserves exact videoId for each related song and guarantees no repeating song names.
     */
    suspend fun getRelatedSongs(song: Song, catalog: List<Song> = emptyList()): List<Song> = withContext(Dispatchers.IO) {
        val relatedList = mutableListOf<Song>()
        val currentVid = song.videoId ?: song.id.removePrefix("yt_")
        val currentNormTitle = getNormalizedBaseTitle(song.title)
        val currentRawTitle = song.title.trim().lowercase()

        // 1. Catalog match (instant local recommendations for same artist or genre)
        val catalogArtistMatches = catalog.filter {
            val vid = it.videoId ?: it.id.removePrefix("yt_")
            vid != currentVid &&
            getNormalizedBaseTitle(it.title) != currentNormTitle &&
            it.artist.equals(song.artist, ignoreCase = true)
        }
        val catalogGenreMatches = catalog.filter {
            val vid = it.videoId ?: it.id.removePrefix("yt_")
            vid != currentVid &&
            getNormalizedBaseTitle(it.title) != currentNormTitle &&
            !it.artist.equals(song.artist, ignoreCase = true) &&
            it.genre.equals(song.genre, ignoreCase = true)
        }
        relatedList.addAll(catalogArtistMatches)

        // 2. YouTube real search query for the artist
        val query = if (song.artist.isNotBlank() && !song.artist.equals("YouTube", ignoreCase = true)) {
            "${song.artist} songs"
        } else {
            "${song.title} similar song"
        }

        try {
            val ytResults = search(query)
            // Filter out the currently playing song, duplicate titles, and very long compilations (> 10m)
            val filteredYt = ytResults.filter {
                val vid = it.videoId ?: it.id.removePrefix("yt_")
                vid != currentVid &&
                getNormalizedBaseTitle(it.title) != currentNormTitle &&
                !it.title.trim().equals(currentRawTitle, ignoreCase = true) &&
                it.durationMs in 60000L..600000L
            }
            relatedList.addAll(filteredYt)
        } catch (e: Exception) {
            Log.w(TAG, "[KAIRO][RELATED] Failed fetching related from YouTube: ${e.message}")
        }

        // Add genre matches as backup if list is still small
        if (relatedList.size < 4) {
            relatedList.addAll(catalogGenreMatches)
        }

        // Strictly eliminate any duplicates by ID or song name
        val distinctResults = mutableListOf<Song>()
        val seenTitles = mutableSetOf<String>(currentNormTitle, currentRawTitle)
        val seenIds = mutableSetOf<String>(currentVid, song.id)

        for (item in relatedList) {
            val vid = item.videoId ?: item.id.removePrefix("yt_")
            val iNorm = getNormalizedBaseTitle(item.title)
            val iRaw = item.title.trim().lowercase()
            if (!seenIds.contains(item.id) &&
                !seenIds.contains(vid) &&
                !seenTitles.contains(iNorm) &&
                !seenTitles.contains(iRaw)) {
                distinctResults.add(item)
                seenIds.add(item.id)
                seenIds.add(vid)
                seenTitles.add(iNorm)
                seenTitles.add(iRaw)
            }
        }

        Log.d(TAG, "[KAIRO][RELATED] Found ${distinctResults.size} unique related songs for '${song.title}'")
        distinctResults
    }

    /**
     * Import a YouTube Playlist or Video from URL or ID without any API keys.
     * Supports:
     * - https://www.youtube.com/playlist?list=PL...
     * - https://music.youtube.com/playlist?list=...
     * - https://youtu.be/... (video)
     * - https://www.youtube.com/watch?v=... (video)
     * - Bare Playlist ID: PL... or OLAK5uy_...
     */
    suspend fun importPlaylist(input: String): Pair<String, List<Song>> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return@withContext Pair("Empty Input", emptyList())

        Log.d(TAG, "[KAIRO][YT_IMPORT] Importing YouTube input: $trimmed")

        // 1. Check if it's a playlist
        val playlistId = extractPlaylistId(trimmed)
        if (!playlistId.isNullOrBlank()) {
            return@withContext fetchPlaylistTracks(playlistId)
        }

        // 2. Check if it's a single video URL
        val videoId = extractVideoId(trimmed)
        if (!videoId.isNullOrBlank()) {
            val singleSong = fetchSingleVideoMetadata(videoId)
            return@withContext if (singleSong != null) {
                Pair(singleSong.title, listOf(singleSong))
            } else {
                Pair("YouTube Video", listOf(
                    Song(
                        id = "yt_$videoId",
                        videoId = videoId,
                        title = "YouTube Track",
                        artist = "YouTube",
                        artworkUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                        audioUrl = "https://www.youtube.com/watch?v=$videoId",
                        durationMs = 210000L,
                        source = "YouTube"
                    )
                ))
            }
        }

        // 3. Fallback: treat as search query
        val searchItems = search(trimmed)
        return@withContext Pair("Search: $trimmed", searchItems)
    }

    private fun extractPlaylistId(input: String): String? {
        val matcher = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)").matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        if (input.startsWith("PL") || input.startsWith("OLAK5uy_") || input.startsWith("RD") || input.startsWith("UU")) {
            return input
        }
        return null
    }

    fun extractVideoId(input: String): String? {
        val matcher = Pattern.compile("(?:v=|youtu\\.be/|embed/|shorts/)([a-zA-Z0-9_-]{11})").matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        if (input.length == 11 && !input.contains("/") && !input.contains("?") && !input.contains("&")) {
            return input
        }
        return null
    }

    private suspend fun fetchPlaylistTracks(playlistId: String): Pair<String, List<Song>> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        var playlistTitle = "YouTube Playlist"

        try {
            val payload = JSONObject().apply {
                val context = JSONObject().apply {
                    val client = JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                        put("hl", "en")
                        put("gl", "IN")
                    }
                    put("client", client)
                }
                put("context", context)
                put("browseId", "VL$playlistId")
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/browse")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)

                    // Extract Playlist Title
                    val header = json.optJSONObject("header")
                    val plHeader = header?.optJSONObject("playlistHeaderRenderer")
                        ?: header?.optJSONObject("pageHeaderRenderer")?.optJSONObject("pageTitle")
                    if (plHeader != null) {
                        val t = plHeader.optJSONObject("title")?.optString("simpleText")
                            ?: plHeader.optString("content")
                        if (!t.isNullOrBlank()) {
                            playlistTitle = t
                        }
                    }

                    // Recursively search for lockupViewModel and playlistVideoRenderer
                    parsePlaylistItemsRecursive(json, songs)
                    Log.d(TAG, "[KAIRO][YT_IMPORT] Fetched ${songs.size} items for playlist '$playlistTitle'")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[KAIRO][YT_IMPORT] Failed to fetch playlist tracks: ${e.message}")
        }

        val distinctSongs = songs.distinctBy { it.videoId ?: it.id }
        Pair(playlistTitle, distinctSongs)
    }

    private fun parsePlaylistItemsRecursive(jsonObj: Any, outList: MutableList<Song>) {
        when (jsonObj) {
            is JSONObject -> {
                // Check modern lockupViewModel
                if (jsonObj.has("lockupViewModel")) {
                    val lum = jsonObj.optJSONObject("lockupViewModel")
                    if (lum != null) {
                        val vid = lum.optString("contentId")
                        val meta = lum.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
                        val title = meta?.optJSONObject("title")?.optString("content", "") ?: ""
                        val rows = meta?.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")?.optJSONArray("metadataRows")
                        var artist = "YouTube Artist"
                        if (rows != null && rows.length() > 0) {
                            val parts = rows.optJSONObject(0)?.optJSONArray("metadataParts")
                            if (parts != null && parts.length() > 0) {
                                val a = parts.optJSONObject(0)?.optJSONObject("text")?.optString("content")
                                if (!a.isNullOrBlank()) artist = a
                            }
                        }

                        if (!vid.isNullOrBlank() && title.isNotBlank()) {
                            outList.add(
                                Song(
                                    id = "yt_$vid",
                                    videoId = vid,
                                    title = title,
                                    artist = artist,
                                    album = "YouTube Playlist",
                                    artworkUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                                    audioUrl = "https://www.youtube.com/watch?v=$vid",
                                    durationMs = 210000L,
                                    source = "YouTube",
                                    genre = "YouTube Music"
                                )
                            )
                        }
                    }
                }

                // Check classic playlistVideoRenderer
                if (jsonObj.has("playlistVideoRenderer")) {
                    val pvr = jsonObj.optJSONObject("playlistVideoRenderer")
                    if (pvr != null) {
                        val vid = pvr.optString("videoId")
                        val titleRuns = pvr.optJSONObject("title")?.optJSONArray("runs")
                        val title = buildRunsText(titleRuns)
                        val authorRuns = pvr.optJSONObject("shortBylineText")?.optJSONArray("runs")
                        val author = buildRunsText(authorRuns).ifBlank { "YouTube Artist" }
                        val lengthText = pvr.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
                        val durationMs = if (lengthText.isNotBlank()) parseDuration(lengthText) else 210000L

                        if (!vid.isNullOrBlank() && title.isNotBlank()) {
                            outList.add(
                                Song(
                                    id = "yt_$vid",
                                    videoId = vid,
                                    title = title,
                                    artist = author,
                                    album = "YouTube Playlist",
                                    artworkUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                                    audioUrl = "https://www.youtube.com/watch?v=$vid",
                                    durationMs = durationMs,
                                    source = "YouTube",
                                    genre = "YouTube Music"
                                )
                            )
                        }
                    }
                }

                // Traverse keys
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = jsonObj.opt(key)
                    if (child != null) {
                        parsePlaylistItemsRecursive(child, outList)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until jsonObj.length()) {
                    val item = jsonObj.opt(i)
                    if (item != null) {
                        parsePlaylistItemsRecursive(item, outList)
                    }
                }
            }
        }
    }

    private suspend fun fetchSingleVideoMetadata(videoId: String): Song? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val title = json.optString("title", "YouTube Track")
                    val author = json.optString("author_name", "YouTube Artist")
                    val thumbnail = json.optString("thumbnail_url", "https://i.ytimg.com/vi/$videoId/hqdefault.jpg")

                    return@withContext Song(
                        id = "yt_$videoId",
                        videoId = videoId,
                        title = title,
                        artist = author,
                        album = "YouTube",
                        artworkUrl = thumbnail,
                        audioUrl = "https://www.youtube.com/watch?v=$videoId",
                        durationMs = 210000L,
                        source = "YouTube",
                        genre = "YouTube Music"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[KAIRO][YT_IMPORT] oEmbed fetch error: ${e.message}")
        }
        null
    }

    private fun parseInnerTubeSearchResults(json: JSONObject, outList: MutableList<Song>) {
        val contents = json.optJSONObject("contents")
            ?.optJSONObject("twoColumnSearchResultsRenderer")
            ?.optJSONObject("primaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return

        for (i in 0 until contents.length()) {
            val sec = contents.optJSONObject(i) ?: continue
            val itemSection = sec.optJSONObject("itemSectionRenderer") ?: continue
            val items = itemSection.optJSONArray("contents") ?: continue

            for (j in 0 until items.length()) {
                val item = items.optJSONObject(j) ?: continue
                val vr = item.optJSONObject("videoRenderer") ?: continue

                val videoId = vr.optString("videoId")
                if (videoId.isNullOrBlank()) continue

                // Title
                val titleRuns = vr.optJSONObject("title")?.optJSONArray("runs")
                val title = buildRunsText(titleRuns)
                if (title.isBlank()) continue

                // Author / Channel
                val ownerRuns = vr.optJSONObject("ownerText")?.optJSONArray("runs")
                    ?: vr.optJSONObject("shortBylineText")?.optJSONArray("runs")
                val channel = buildRunsText(ownerRuns).ifBlank { "YouTube" }

                // Duration
                val lengthText = vr.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
                val durationMs = if (lengthText.isNotBlank()) parseDuration(lengthText) else 210000L

                // Thumbnail
                val thumbnails = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                val thumbUrl = if (thumbnails != null && thumbnails.length() > 0) {
                    thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                } else {
                    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                }

                outList.add(
                    Song(
                        id = "yt_$videoId",
                        videoId = videoId,
                        title = title,
                        artist = channel,
                        album = "YouTube",
                        artworkUrl = thumbUrl,
                        audioUrl = "https://www.youtube.com/watch?v=$videoId",
                        durationMs = durationMs,
                        source = "YouTube",
                        genre = "YouTube Music"
                    )
                )
            }
        }
    }

    private fun buildRunsText(runs: JSONArray?): String {
        if (runs == null) return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text", ""))
        }
        return sb.toString().trim()
    }

    private fun parseDuration(simpleText: String): Long {
        val parts = simpleText.split(":")
        return when (parts.size) {
            2 -> {
                val m = parts[0].toLongOrNull() ?: 0L
                val s = parts[1].toLongOrNull() ?: 0L
                (m * 60 + s) * 1000L
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val s = parts[2].toLongOrNull() ?: 0L
                (h * 3600 + m * 60 + s) * 1000L
            }
            else -> 180000L
        }
    }
}
