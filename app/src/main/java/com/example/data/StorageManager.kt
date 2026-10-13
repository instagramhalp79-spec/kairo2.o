package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Playlist
import com.example.model.Song
import org.json.JSONArray
import org.json.JSONObject

class StorageManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kairo_music_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LIKED_SONGS = "kairo_liked_songs"
        private const val KEY_LIKED_SONGS_FULL = "kairo_liked_songs_full"
        private const val KEY_HISTORY = "kairo_history"
        private const val KEY_HISTORY_FULL = "kairo_history_full"
        private const val KEY_FOLLOWED_ARTISTS = "kairo_followed_artists"
        private const val KEY_PLAYLISTS = "kairo_custom_playlists"
        private const val KEY_LOCAL_SONGS = "kairo_local_songs"
        private const val KEY_YT_SONGS = "kairo_yt_songs"
        private const val KEY_THEME = "kairo_theme"
        private const val KEY_NOTIFICATIONS = "kairo_notifications"
        private const val KEY_AUTOPLAY = "kairo_autoplay"
        private const val KEY_LAST_PLAYED_SONG = "kairo_last_played_song"
        private const val KEY_SPOTLIGHT_LAST_UPDATE_TIME = "kairo_spotlight_last_update_time"
        private const val KEY_SPOTLIGHT_SONGS = "kairo_spotlight_songs"
    }

    // Liked songs
    fun getLikedSongIds(): Set<String> {
        val ids = prefs.getStringSet(KEY_LIKED_SONGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        // Also combine with any IDs from full liked songs
        getLikedSongs().forEach { ids.add(it.id) }
        return ids
    }

    fun getLikedSongs(): List<Song> {
        val raw = prefs.getString(KEY_LIKED_SONGS_FULL, "[]") ?: "[]"
        val list = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSongFromJson(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveLikedSongs(songs: List<Song>) {
        val jsonArray = JSONArray()
        songs.forEach { s ->
            jsonArray.put(songToJson(s))
        }
        val ids = songs.map { it.id }.toSet()
        prefs.edit()
            .putString(KEY_LIKED_SONGS_FULL, jsonArray.toString())
            .putStringSet(KEY_LIKED_SONGS, ids)
            .apply()
    }

    fun toggleLikeSong(song: Song): Boolean {
        val currentList = getLikedSongs().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == song.id || (it.videoId != null && it.videoId == song.videoId) }
        val isNowLiked = if (existingIndex >= 0) {
            currentList.removeAt(existingIndex)
            false
        } else {
            currentList.add(0, song)
            true
        }
        saveLikedSongs(currentList)
        return isNowLiked
    }

    fun toggleLikeSongById(songId: String, fallbackSong: Song? = null): Boolean {
        if (fallbackSong != null && (fallbackSong.id == songId || fallbackSong.videoId == songId)) {
            return toggleLikeSong(fallbackSong)
        }
        val currentIds = getLikedSongIds().toMutableSet()
        val isNowLiked = if (currentIds.contains(songId)) {
            currentIds.remove(songId)
            val updated = getLikedSongs().filter { it.id != songId && it.videoId != songId }
            saveLikedSongs(updated)
            false
        } else {
            currentIds.add(songId)
            prefs.edit().putStringSet(KEY_LIKED_SONGS, currentIds).apply()
            true
        }
        return isNowLiked
    }

    // Play History
    fun getHistorySongIds(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun getHistorySongs(): List<Song> {
        val raw = prefs.getString(KEY_HISTORY_FULL, "[]") ?: "[]"
        val list = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSongFromJson(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    fun addToHistory(song: Song) {
        // Save full song object
        val fullList = getHistorySongs().toMutableList()
        fullList.removeAll { it.id == song.id || (it.videoId != null && it.videoId == song.videoId) }
        fullList.add(0, song)
        if (fullList.size > 80) {
            fullList.removeAt(fullList.lastIndex)
        }
        val fullArray = JSONArray()
        fullList.forEach { fullArray.put(songToJson(it)) }

        // Also save ID list for backwards compatibility
        val idList = getHistorySongIds().toMutableList()
        idList.remove(song.id)
        idList.add(0, song.id)
        if (idList.size > 80) {
            idList.removeAt(idList.lastIndex)
        }
        val idArray = JSONArray(idList)

        prefs.edit()
            .putString(KEY_HISTORY_FULL, fullArray.toString())
            .putString(KEY_HISTORY, idArray.toString())
            .apply()
    }

    fun addToHistoryById(songId: String) {
        val current = getHistorySongIds().toMutableList()
        current.remove(songId)
        current.add(0, songId)
        if (current.size > 80) {
            current.removeAt(current.lastIndex)
        }
        val jsonArray = JSONArray(current)
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit()
            .putString(KEY_HISTORY, "[]")
            .putString(KEY_HISTORY_FULL, "[]")
            .apply()
    }

    // Followed Artists
    fun getFollowedArtistIds(): Set<String> {
        val defaultFollowed = setOf("art_1", "art_3")
        return prefs.getStringSet(KEY_FOLLOWED_ARTISTS, defaultFollowed) ?: defaultFollowed
    }

    fun toggleFollowArtist(artistId: String): Boolean {
        val current = getFollowedArtistIds().toMutableSet()
        val isFollowed = if (current.contains(artistId)) {
            current.remove(artistId)
            false
        } else {
            current.add(artistId)
            true
        }
        prefs.edit().putStringSet(KEY_FOLLOWED_ARTISTS, current).apply()
        return isFollowed
    }

    // Custom Playlists (Strictly user-created / user-added only)
    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null)
        if (raw == null) {
            savePlaylists(emptyList())
            return emptyList()
        }
        val list = mutableListOf<Playlist>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                // Exclude any default/sample playlists - ONLY user created playlists are allowed
                if (id == "pl_1" || id == "pl_2" || id == "pl_3" || id.startsWith("sample_") || id == "starter_gym_pl") {
                    continue
                }
                val songIdsArray = obj.getJSONArray("songIds")
                val songIds = mutableListOf<String>()
                for (j in 0 until songIdsArray.length()) {
                    songIds.add(songIdsArray.getString(j))
                }
                list.add(
                    Playlist(
                        id = id,
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        coverUrl = obj.optString("coverUrl", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"),
                        songIds = songIds,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val jsonArray = JSONArray()
        playlists.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("description", p.description)
            obj.put("coverUrl", p.coverUrl)
            obj.put("songIds", JSONArray(p.songIds))
            obj.put("createdAt", p.createdAt)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply()
    }

    // Local Imported Songs
    fun getLocalSongs(): List<Song> {
        val raw = prefs.getString(KEY_LOCAL_SONGS, "[]") ?: "[]"
        val list = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSongFromJson(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveLocalSongs(songs: List<Song>) {
        val jsonArray = JSONArray()
        songs.forEach { s ->
            jsonArray.put(songToJson(s))
        }
        prefs.edit().putString(KEY_LOCAL_SONGS, jsonArray.toString()).apply()
    }

    // Saved YouTube Songs (imported playlists / tracks)
    fun getYoutubeSongs(): List<Song> {
        val raw = prefs.getString(KEY_YT_SONGS, "[]") ?: "[]"
        val list = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSongFromJson(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveYoutubeSongs(songs: List<Song>) {
        val jsonArray = JSONArray()
        songs.forEach { s ->
            jsonArray.put(songToJson(s))
        }
        prefs.edit().putString(KEY_YT_SONGS, jsonArray.toString()).apply()
    }

    private fun songToJson(s: Song): JSONObject {
        val obj = JSONObject()
        obj.put("id", s.id)
        obj.put("videoId", s.videoId)
        obj.put("title", s.title)
        obj.put("artist", s.artist)
        obj.put("album", s.album)
        obj.put("artworkUrl", s.artworkUrl)
        obj.put("audioUrl", s.audioUrl)
        obj.put("durationMs", s.durationMs)
        obj.put("source", s.source)
        obj.put("genre", s.genre)
        obj.put("isNewRelease", s.isNewRelease)
        obj.put("isVideo", s.isVideo || s.hasVideo)
        return obj
    }

    private fun parseSongFromJson(obj: JSONObject): Song {
        val vId = if (obj.has("videoId") && !obj.isNull("videoId")) obj.getString("videoId") else null
        val isVid = obj.optBoolean("isVideo", false)
        return Song(
            id = obj.getString("id"),
            videoId = vId,
            title = obj.getString("title"),
            artist = obj.optString("artist", "Unknown Artist"),
            album = obj.optString("album", "KAIRO Music"),
            artworkUrl = obj.optString("artworkUrl", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"),
            audioUrl = obj.optString("audioUrl", ""),
            durationMs = obj.optLong("durationMs", 180000L),
            source = obj.optString("source", "Online"),
            genre = obj.optString("genre", "Music"),
            isNewRelease = obj.optBoolean("isNewRelease", false),
            isVideo = isVid
        )
    }

    // Last Played Song (Shown on Playing button when clicked, empty on fresh app open)
    fun getLastPlayedSong(): Song? {
        val raw = prefs.getString(KEY_LAST_PLAYED_SONG, null) ?: return null
        return try {
            parseSongFromJson(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    fun saveLastPlayedSong(song: Song) {
        prefs.edit().putString(KEY_LAST_PLAYED_SONG, songToJson(song).toString()).apply()
    }

    fun clearLastPlayedSong() {
        prefs.edit().remove(KEY_LAST_PLAYED_SONG).apply()
    }

    // 24-Hour Trending Spotlight Management
    fun getSpotlightLastUpdateTime(): Long {
        return prefs.getLong(KEY_SPOTLIGHT_LAST_UPDATE_TIME, 0L)
    }

    fun setSpotlightLastUpdateTime(timeMs: Long) {
        prefs.edit().putLong(KEY_SPOTLIGHT_LAST_UPDATE_TIME, timeMs).apply()
    }

    fun getSavedSpotlightSongs(): List<Song> {
        val raw = prefs.getString(KEY_SPOTLIGHT_SONGS, null) ?: return emptyList()
        val list = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                list.add(parseSongFromJson(jsonArray.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveSpotlightSongs(songs: List<Song>) {
        val jsonArray = JSONArray()
        songs.forEach { jsonArray.put(songToJson(it)) }
        prefs.edit().putString(KEY_SPOTLIGHT_SONGS, jsonArray.toString()).apply()
    }

    // Theme (default dark)
    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_THEME, true)
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, isDark).apply()
    }

    // Notifications
    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    // Related-song Autoplay
    fun isAutoplayEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTOPLAY, true)
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
    }
}
