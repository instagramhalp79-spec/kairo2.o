package com.example.core.music

import android.content.Context
import android.util.Log
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class OfflineMusicManager(private val context: Context) {
    companion object {
        private const val TAG = "KAIRO_OFFLINE"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val offlineDir: File by lazy {
        val dir = File(context.filesDir, "kairo_offline_music")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val _downloadedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedSongIds: StateFlow<Set<String>> = _downloadedSongIds.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadStatusMessage = MutableStateFlow<String?>(null)
    val downloadStatusMessage: StateFlow<String?> = _downloadStatusMessage.asStateFlow()

    init {
        refreshDownloadedCache()
    }

    fun refreshDownloadedCache() {
        val files = offlineDir.listFiles() ?: emptyArray()
        val ids = files.filter { it.isFile && it.length() > 50_000L }
            .map { it.nameWithoutExtension }
            .toSet()
        _downloadedSongIds.value = ids
        Log.d(TAG, "[KAIRO][OFFLINE] Cache initialized: ${ids.size} offline tracks available on disk")
    }

    private fun getSanitizedFileName(songId: String): String {
        return songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    fun getLocalFileForSong(songId: String): File {
        val cleanName = getSanitizedFileName(songId)
        return File(offlineDir, "$cleanName.mp3")
    }

    fun isSongDownloaded(song: Song): Boolean {
        if (song.audioUrl.startsWith("file://")) {
            val path = song.audioUrl.removePrefix("file://")
            val f = File(path)
            if (f.exists() && f.length() > 50_000L) return true
        }
        val target = getLocalFileForSong(song.id)
        return target.exists() && target.length() > 50_000L
    }

    fun getLocalAudioUrlIfDownloaded(song: Song): String? {
        if (song.audioUrl.startsWith("file://")) {
            val path = song.audioUrl.removePrefix("file://")
            val f = File(path)
            if (f.exists() && f.length() > 50_000L) return song.audioUrl
        }
        val target = getLocalFileForSong(song.id)
        if (target.exists() && target.length() > 50_000L) {
            return "file://${target.absolutePath}"
        }
        return null
    }

    fun getDownloadedTrackCount(playlist: Playlist): Int {
        val downloaded = _downloadedSongIds.value
        return playlist.songIds.count { id ->
            downloaded.contains(getSanitizedFileName(id)) || isSongDownloaded(Song(id = id, title = "", artist = "", artworkUrl = "", audioUrl = "", durationMs = 0L))
        }
    }

    suspend fun downloadSong(song: Song): Song? = withContext(Dispatchers.IO) {
        val existing = getLocalAudioUrlIfDownloaded(song)
        if (existing != null) {
            Log.d(TAG, "[KAIRO][OFFLINE] Song '${song.title}' already downloaded at: $existing")
            return@withContext song.copy(audioUrl = existing, source = "Offline")
        }

        // Determine downloadable stream URL
        var streamUrl = song.audioUrl
        if (streamUrl.isBlank() || streamUrl.contains("soundhelix") || streamUrl.contains("youtube.com") || streamUrl.contains("youtu.be")) {
            streamUrl = LevyraMusicResolver.resolveSongStream(song)
        }

        if (streamUrl.isBlank() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
            Log.w(TAG, "[KAIRO][OFFLINE] No direct downloadable stream URL for '${song.title}'")
            return@withContext null
        }

        val targetFile = getLocalFileForSong(song.id)
        val tempFile = File(offlineDir, "${targetFile.name}.tmp")

        try {
            Log.d(TAG, "[KAIRO][OFFLINE] Downloading '${song.title}' from $streamUrl")
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "[KAIRO][OFFLINE] Download failed with code ${response.code} for '${song.title}'")
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.length() > 50_000L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                _downloadedSongIds.value = _downloadedSongIds.value + getSanitizedFileName(song.id)
                Log.d(TAG, "[KAIRO][OFFLINE] Successfully saved '${song.title}' to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                return@withContext song.copy(
                    audioUrl = "file://${targetFile.absolutePath}",
                    source = "Offline"
                )
            } else {
                tempFile.delete()
                Log.w(TAG, "[KAIRO][OFFLINE] Downloaded file too small (${tempFile.length()} bytes) for '${song.title}'")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "[KAIRO][OFFLINE] Exception downloading '${song.title}': ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            return@withContext null
        }
    }

    suspend fun downloadPlaylist(
        playlistName: String,
        songs: List<Song>,
        onSongDownloaded: (Song) -> Unit
    ): List<Song> = withContext(Dispatchers.IO) {
        _isDownloading.value = true
        _downloadStatusMessage.value = "Downloading '$playlistName' for offline playback (0/${songs.size})..."
        Log.d(TAG, "[KAIRO][OFFLINE] Starting background download of ${songs.size} tracks for '$playlistName'")

        val updatedSongs = mutableListOf<Song>()
        var completedCount = 0

        for ((index, song) in songs.withIndex()) {
            _downloadStatusMessage.value = "Downloading ($index/${songs.size}): ${song.title}"
            val downloadedSong = downloadSong(song)
            if (downloadedSong != null) {
                updatedSongs.add(downloadedSong)
                onSongDownloaded(downloadedSong)
                completedCount++
            } else {
                updatedSongs.add(song)
            }
        }

        refreshDownloadedCache()
        _isDownloading.value = false
        _downloadStatusMessage.value = "Downloaded $completedCount/${songs.size} tracks for '$playlistName' • Offline Ready"
        Log.d(TAG, "[KAIRO][OFFLINE] Completed playlist download: $completedCount/${songs.size} offline ready")
        updatedSongs
    }
}
