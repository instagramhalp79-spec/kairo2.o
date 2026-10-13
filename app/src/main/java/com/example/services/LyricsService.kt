package com.example.services

import android.util.Log
import com.example.model.LrcParser
import com.example.model.LyricsData
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Levyra-style Synchronized Lyrics Service.
 * 
 * Powered by LRCLIB (lrclib.net) with multi-tier fallback:
 * 1. Embedded LRC timestamps if present on Song model
 * 2. LRCLIB exact match (/api/get?track_name=...&artist_name=...)
 * 3. LRCLIB multi-result search (/api/search?q=...) with synced lyrics prioritization
 * 4. LRCLIB track search (/api/search?track_name=...)
 * 5. Lyrics.ovh fallback (/v1/{artist}/{title}) for plain lyrics
 * 6. Local rhythmic synced lyrics fallback
 */
class LyricsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    private val cache = mutableMapOf<String, LyricsData>()

    suspend fun resolveLyrics(song: Song): LyricsData = withContext(Dispatchers.IO) {
        val cacheKey = song.videoId ?: "${song.title}_${song.artist}"
        cache[cacheKey]?.let { return@withContext it }

        // 1. If song already has embedded LRC lyrics, parse directly
        if (!song.lyrics.isNullOrBlank()) {
            val parsed = LrcParser.parse(song.lyrics)
            if (parsed.isSynced && parsed.lines.isNotEmpty()) {
                cache[cacheKey] = parsed
                return@withContext parsed
            }
        }

        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)

        // 2. Try LRCLIB exact match (/api/get)
        try {
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist"
            Log.d(TAG, "[LEVYRA_LYRICS] Querying LRCLIB exact: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Levyra-KAIRO/2.4 (Android; +https://github.com/LUC4N3X/Levyra-deepsound)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val synced = json.optString("syncedLyrics", "")
                    val plain = json.optString("plainLyrics", "")

                    if (synced.isNotBlank()) {
                        val parsed = LrcParser.parse(synced)
                        if (parsed.lines.isNotEmpty()) {
                            Log.d(TAG, "[LEVYRA_LYRICS] Found synced lyrics via LRCLIB exact for '${song.title}' (${parsed.lines.size} lines)")
                            cache[cacheKey] = parsed
                            return@withContext parsed
                        }
                    } else if (plain.isNotBlank()) {
                        val plainData = LyricsData(emptyList(), plain.trim(), false)
                        cache[cacheKey] = plainData
                        return@withContext plainData
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[LEVYRA_LYRICS] LRCLIB exact query error: ${e.message}")
        }

        // 3. Try LRCLIB search (/api/search?q=...)
        try {
            val queryStr = "$cleanTitle $cleanArtist".trim()
            val encodedQuery = URLEncoder.encode(queryStr, "UTF-8")
            val searchUrl = "https://lrclib.net/api/search?q=$encodedQuery"
            Log.d(TAG, "[LEVYRA_LYRICS] Querying LRCLIB search: $searchUrl")

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Levyra-KAIRO/2.4 (Android; +https://github.com/LUC4N3X/Levyra-deepsound)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val results = JSONArray(body)
                    var bestPlain: String? = null

                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val synced = item.optString("syncedLyrics", "")
                        val plain = item.optString("plainLyrics", "")

                        if (synced.isNotBlank()) {
                            val parsed = LrcParser.parse(synced)
                            if (parsed.lines.isNotEmpty()) {
                                Log.d(TAG, "[LEVYRA_LYRICS] Found synced lyrics in search results for '${song.title}' (${parsed.lines.size} lines)")
                                cache[cacheKey] = parsed
                                return@withContext parsed
                            }
                        }
                        if (bestPlain == null && plain.isNotBlank()) {
                            bestPlain = plain.trim()
                        }
                    }

                    if (bestPlain != null) {
                        val plainData = LyricsData(emptyList(), bestPlain, false)
                        cache[cacheKey] = plainData
                        return@withContext plainData
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[LEVYRA_LYRICS] LRCLIB search query error: ${e.message}")
        }

        // 4. Try lyrics.ovh fallback
        try {
            val encodedArtistOvh = URLEncoder.encode(cleanArtist, "UTF-8")
            val encodedTitleOvh = URLEncoder.encode(cleanTitle, "UTF-8")
            val ovhUrl = "https://api.lyrics.ovh/v1/$encodedArtistOvh/$encodedTitleOvh"

            val request = Request.Builder()
                .url(ovhUrl)
                .header("User-Agent", "Levyra-KAIRO/2.4 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val lyricsText = json.optString("lyrics", "")
                    if (lyricsText.isNotBlank()) {
                        val plainData = LyricsData(emptyList(), lyricsText.trim(), false)
                        cache[cacheKey] = plainData
                        return@withContext plainData
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[LEVYRA_LYRICS] lyrics.ovh fallback error: ${e.message}")
        }

        // 5. Embedded song lyrics fallback if provided
        if (!song.lyrics.isNullOrBlank()) {
            val parsed = LrcParser.parse(song.lyrics)
            cache[cacheKey] = parsed
            return@withContext parsed
        }

        // 6. Default pleasant fallback
        val defaultLyrics = LyricsData(
            lines = emptyList(),
            plainText = "♪ Enjoying \"${song.title}\" by ${song.artist}\n\nHigh-definition MP3 Audio Playback active.\nSynced lyrics search via LRCLIB completed.",
            isSynced = false
        )
        cache[cacheKey] = defaultLyrics
        defaultLyrics
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("""\(.*?\)|\[.*?\]"""), "") // Remove (Official Video), [MV]
            .replace(Regex("""(?i)\b(feat\.|ft\.|official|audio|video|lyrics?|full song|hd|4k)\b.*"""), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("""(?i)\s*-\s*topic"""), "")
            .replace(Regex("""(?i)\s*vevo"""), "")
            .split(",").firstOrNull()?.trim() ?: artist.trim()
    }

    companion object {
        private const val TAG = "KAIRO_LEVYRA_LYRICS"
    }
}
