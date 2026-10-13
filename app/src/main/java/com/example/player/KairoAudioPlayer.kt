package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.model.PlaybackMode
import com.example.model.PlayerState
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

class KairoAudioPlayer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSongStarted: (Song) -> Unit,
    private val onAutoplayNext: (() -> Unit)? = null,
    private val onRequestNextTrack: (() -> Song?)? = null,
    private val onRequestPreviousTrack: (() -> Song?)? = null
) {
    companion object {
        private const val TAG = "KAIRO_PLAYER"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var videoSurface: android.view.Surface? = null
    private var progressJob: Job? = null
    private val random = Random()
    private var isYouTubeActive = false
    private var currentVolume: Float = 1.0f
    private val isAdvancingTrack = AtomicBoolean(false)

    fun attachVideoSurface(surface: android.view.Surface) {
        videoSurface = surface
        try {
            if (surface.isValid) {
                mediaPlayer?.setSurface(surface)
                Log.d(TAG, "[KAIRO][PLAYER] Video surface attached to MediaPlayer")
            }
        } catch (e: Exception) {
            Log.w(TAG, "[KAIRO][PLAYER] Failed to attach video surface: ${e.message}")
        }
    }

    fun detachVideoSurface() {
        videoSurface = null
        try {
            mediaPlayer?.setSurface(null)
        } catch (_: Exception) {}
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Android MediaSession & Notification Manager
    val notificationManager: KairoMediaNotificationManager = KairoMediaNotificationManager(
        context = context,
        onPlayPause = { togglePlayPause() },
        onSkipNext = { playNext() },
        onSkipPrevious = { playPrevious() },
        onStopPlayback = { stopPlayback() },
        onSeekTo = { seekTo(it) }
    )

    // Direct YouTube Audio Playback Bridge (Zero API Keys Required)
    val ytBridge = YouTubePlayerBridge(
        context = context,
        onStateChanged = { isPlaying, isBuffering ->
            if (isYouTubeActive) {
                _playerState.value = _playerState.value.copy(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering
                )
                _playerState.value.currentSong?.let { song ->
                    notificationManager.updateMetadata(
                        song = song,
                        durationMs = _playerState.value.durationMs,
                        isPlaying = isPlaying,
                        positionMs = _playerState.value.currentPositionMs
                    )
                }
            }
        },
        onTimeUpdate = { curMs, durMs ->
            if (isYouTubeActive) {
                val finalDur = if (durMs > 0) durMs else _playerState.value.currentSong?.durationMs ?: 0L
                _playerState.value = _playerState.value.copy(
                    currentPositionMs = curMs,
                    durationMs = finalDur
                )
            }
        },
        onSongEnded = {
            if (isYouTubeActive) {
                handleTrackEnded()
            }
        },
        onError = { errMsg ->
            if (isYouTubeActive) {
                Log.w(TAG, "[KAIRO][PLAYER] YouTube bridge error: $errMsg. Resolving seamless fallback stream...")
                val song = _playerState.value.currentSong
                if (song != null) {
                    // Fix playback error 152: Attempt automatic fallback to direct stream
                    scope.launch(Dispatchers.Main) {
                        _playerState.value = _playerState.value.copy(
                            isBuffering = true,
                            errorMessage = null
                        )
                        val fallback = withContext(Dispatchers.IO) {
                            com.example.core.music.LevyraMusicResolver.resolveSongTrack(song)
                        }
                        if (fallback != null && fallback.audioUrl.isNotBlank() &&
                            !fallback.audioUrl.contains("youtube.com") && !fallback.audioUrl.contains("youtu.be")) {
                            Log.i(TAG, "[KAIRO][PLAYER] Resolved direct stream for '${song.title}' after YouTube error: ${fallback.audioUrl}")
                            val updatedSong = song.copy(
                                audioUrl = fallback.audioUrl,
                                artworkUrl = fallback.artworkUrl ?: song.artworkUrl
                            )
                            loadAndPlayLocal(updatedSong, fallback.audioUrl)
                        } else {
                            val clean = com.example.core.music.LevyraMusicResolver.cleanSongQuery(song.title, song.artist)
                            val retryFallback = withContext(Dispatchers.IO) {
                                com.example.core.music.LevyraMusicResolver.resolveExactSong(clean.coreTitle, clean.primaryArtist)
                            }
                            if (retryFallback != null && retryFallback.audioUrl.isNotBlank()) {
                                Log.i(TAG, "[KAIRO][PLAYER] Resolved cleaned direct stream for '${song.title}': ${retryFallback.audioUrl}")
                                val updatedSong = song.copy(
                                    audioUrl = retryFallback.audioUrl,
                                    artworkUrl = retryFallback.artworkUrl ?: song.artworkUrl
                                )
                                loadAndPlayLocal(updatedSong, retryFallback.audioUrl)
                            } else {
                                Log.w(TAG, "[KAIRO][PLAYER] Fallback stream not found for '${song.title}'")
                                _playerState.value = _playerState.value.copy(
                                    isBuffering = false,
                                    isPlaying = false,
                                    errorMessage = "Could not play: ${song.title}"
                                )
                            }
                        }
                    }
                } else {
                    _playerState.value = _playerState.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = errMsg
                    )
                }
            }
        }
    )

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (!isYouTubeActive) {
                    mediaPlayer?.let { player ->
                        val isPlaying = try { player.isPlaying } catch (_: Exception) { false }
                        if (isPlaying) {
                            val pos = try { player.currentPosition.toLong() } catch (_: Exception) { 0L }
                            val dur = try { player.duration.toLong().coerceAtLeast(1L) } catch (_: Exception) { 0L }
                            _playerState.value = _playerState.value.copy(
                                currentPositionMs = pos,
                                durationMs = if (dur > 0) dur else _playerState.value.currentSong?.durationMs ?: 0L,
                                isPlaying = true
                            )
                        }
                    }
                }
                delay(250)
            }
        }
    }

    fun playSingleTrack(song: Song) {
        playSong(
            song = song,
            newQueue = listOf(song),
            targetIndex = 0,
            playbackMode = PlaybackMode.SMART_AUTOPLAY,
            activePlaylist = null
        )
    }

    fun playSong(
        song: Song,
        newQueue: List<Song>? = null,
        targetIndex: Int = -1,
        playbackMode: PlaybackMode = PlaybackMode.SMART_AUTOPLAY,
        activePlaylist: Playlist? = null
    ) {
        val currentQueue = newQueue ?: if (_playerState.value.queue.contains(song)) {
            _playerState.value.queue
        } else {
            listOf(song)
        }
        val actualIndex = if (targetIndex >= 0) targetIndex else currentQueue.indexOf(song).let { if (it >= 0) it else 0 }

        isAdvancingTrack.set(false)

        _playerState.value = _playerState.value.copy(
            currentSong = song,
            queue = currentQueue,
            currentIndex = actualIndex,
            playbackMode = playbackMode,
            activePlaylist = activePlaylist,
            isBuffering = true,
            errorMessage = null,
            currentPositionMs = 0L,
            durationMs = song.durationMs
        )

        loadAndPlayTrack(song)
        onSongStarted(song)
        notificationManager.updateMetadata(song, song.durationMs, false, 0L)
    }

    /**
     * Restores the user's last played song into the player state in paused ready state.
     * Only applied when no active song is currently loaded in the player.
     */
    fun restoreLastPlayedSong(song: Song) {
        if (_playerState.value.currentSong != null) return
        _playerState.value = _playerState.value.copy(
            currentSong = song,
            queue = listOf(song),
            currentIndex = 0,
            isPlaying = false,
            isBuffering = false,
            errorMessage = null,
            currentPositionMs = 0L,
            durationMs = song.durationMs
        )
        notificationManager.updateMetadata(song, song.durationMs, false, 0L)
    }

    private fun extractVideoId(song: Song): String? {
        val vid = song.videoId?.trim()
        if (!vid.isNullOrBlank() && vid.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return vid
        }
        val url = song.audioUrl.trim()
        if (url.isBlank()) return null

        val patterns = listOf(
            Regex("(?:v=|/v/|youtu\\.be/|/embed/|shorts/)([a-zA-Z0-9_-]{11})"),
            Regex("^[a-zA-Z0-9_-]{11}$")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues.last()
            }
        }
        return null
    }

    private fun safelyRecycleMediaPlayer() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            try { player.setSurface(null) } catch (_: Exception) {}
            if (player.isPlaying) {
                player.stop()
            }
            player.reset()
        } catch (_: Exception) {}
        try {
            player.release()
        } catch (_: Exception) {}
    }

    private fun loadAndPlayTrack(song: Song) {
        scope.launch(Dispatchers.Main) {
            _playerState.value = _playerState.value.copy(
                isBuffering = true,
                currentSong = song,
                errorMessage = null
            )

            // 1. Direct local file (downloaded offline file or imported MP3/MP4/video)
            val isLocalMedia = song.source == "Local" ||
                    song.audioUrl.startsWith("content://") ||
                    song.audioUrl.startsWith("file://") ||
                    song.audioUrl.startsWith("/") ||
                    java.io.File(song.audioUrl).exists()

            if (isLocalMedia) {
                Log.d(TAG, "[KAIRO][PLAYER] Playing local media track: '${song.title}' (${song.audioUrl})")
                loadAndPlayLocal(song, song.audioUrl)
                return@launch
            }

            // 2. MP4 Video / YouTube track: Play via MP4 video engine if videoId is available
            var videoId = extractVideoId(song)
            if (!videoId.isNullOrBlank()) {
                Log.d(TAG, "[KAIRO][PLAYER] Playing via MP4 video engine for: $videoId ('${song.title}')")
                playViaYouTube(song, videoId)
                return@launch
            }

            // 2b. If song has no 11-char videoId and is not local, dynamically resolve exact match from YouTube
            if (!isLocalMedia) {
                Log.d(TAG, "[KAIRO][PLAYER] Auto-resolving exact YouTube match for '${song.title}' by '${song.artist}'")
                val searchResults = withContext(Dispatchers.IO) {
                    val q = "${song.title} ${song.artist}".trim()
                    com.example.core.youtube.YouTubeMediaResolver.search(q)
                }
                val resolvedVid = searchResults.firstOrNull()?.let { extractVideoId(it) }
                if (!resolvedVid.isNullOrBlank()) {
                    Log.d(TAG, "[KAIRO][PLAYER] Resolved exact YouTube videoId=$resolvedVid for '${song.title}'")
                    val updatedSong = song.copy(
                        videoId = resolvedVid,
                        audioUrl = "https://www.youtube.com/watch?v=$resolvedVid"
                    )
                    _playerState.value = _playerState.value.copy(currentSong = updatedSong)
                    playViaYouTube(updatedSong, resolvedVid)
                    return@launch
                }
            }

            // 3. Direct online audio stream (MP3/AAC/MP4/M4A) that is NOT a YouTube web link and NOT soundhelix
            val isDirectAudioUrl = (song.audioUrl.startsWith("http://") || song.audioUrl.startsWith("https://")) &&
                    (song.audioUrl.contains(".mp3") || song.audioUrl.contains(".mp4") ||
                     song.audioUrl.contains(".m4a") || song.audioUrl.contains(".aac") ||
                     song.audioUrl.contains("saavncdn.com")) &&
                    !song.audioUrl.contains("youtube.com") && !song.audioUrl.contains("youtu.be") &&
                    !song.audioUrl.contains("soundhelix")

            if (isDirectAudioUrl) {
                loadAndPlayLocal(song, song.audioUrl)
                return@launch
            }

            // 4. Online stream resolution for exact title & artist via Levyra resolver
            val resolvedTrack = withContext(Dispatchers.IO) {
                com.example.core.music.LevyraMusicResolver.resolveSongTrack(song)
            }

            if (resolvedTrack != null && resolvedTrack.audioUrl.isNotBlank() &&
                !resolvedTrack.audioUrl.contains("youtube.com") && !resolvedTrack.audioUrl.contains("youtu.be")
            ) {
                val resolvedSong = song.copy(
                    audioUrl = resolvedTrack.audioUrl,
                    title = if (resolvedTrack.title.isNotBlank()) resolvedTrack.title else song.title,
                    artist = if (resolvedTrack.artist.isNotBlank() && resolvedTrack.artist != "YouTube") resolvedTrack.artist else song.artist,
                    artworkUrl = resolvedTrack.artworkUrl ?: song.artworkUrl
                )
                _playerState.value = _playerState.value.copy(currentSong = resolvedSong)
                loadAndPlayLocal(resolvedSong, resolvedTrack.audioUrl)
                return@launch
            }

            // 5. If song has any non-YouTube audioUrl, attempt MediaPlayer playback
            if (song.audioUrl.isNotBlank() && !song.audioUrl.contains("youtube.com") && !song.audioUrl.contains("youtu.be") && !song.audioUrl.contains("soundhelix")) {
                loadAndPlayLocal(song, song.audioUrl)
                return@launch
            }

            // 6. Broad fallback stream resolution before giving up
            val broadFallback = withContext(Dispatchers.IO) {
                val clean = com.example.core.music.LevyraMusicResolver.cleanSongQuery(song.title, song.artist)
                com.example.core.music.LevyraMusicResolver.resolveExactSong(clean.coreTitle, "")
            }
            if (broadFallback != null && broadFallback.audioUrl.isNotBlank()) {
                val resolvedSong = song.copy(
                    audioUrl = broadFallback.audioUrl,
                    artworkUrl = broadFallback.artworkUrl ?: song.artworkUrl
                )
                _playerState.value = _playerState.value.copy(currentSong = resolvedSong)
                loadAndPlayLocal(resolvedSong, broadFallback.audioUrl)
                return@launch
            }

            Log.w(TAG, "[KAIRO][PLAYER] Stream search exhausted for '${song.title}'")
            _playerState.value = _playerState.value.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "Could not play: ${song.title}"
            )
        }
    }

    private fun playViaYouTube(song: Song, videoId: String) {
        scope.launch(Dispatchers.Main) {
            isYouTubeActive = true
            safelyRecycleMediaPlayer()

            _playerState.value = _playerState.value.copy(
                isBuffering = true,
                isPlaying = false,
                currentSong = song,
                errorMessage = null,
                durationMs = song.durationMs
            )
            Log.d(TAG, "[KAIRO][PLAYER] Playing via YouTube bridge: videoId=$videoId ('${song.title}')")
            ytBridge.playVideo(videoId)
            onSongStarted(song)
            notificationManager.updateMetadata(song, song.durationMs, false, 0L)
        }
    }

    private fun loadAndPlayLocal(song: Song, effectiveUrl: String) {
        scope.launch(Dispatchers.Main) {
            isYouTubeActive = false
            try {
                ytBridge.stop()
            } catch (_: Exception) {}

            safelyRecycleMediaPlayer()

            Log.d(TAG, "[KAIRO][PLAYER] Playing media via MediaPlayer for: '${song.title}'")

            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    // Attach video surface for MP4 video playback
                    if (videoSurface != null && videoSurface?.isValid == true) {
                        try {
                            setSurface(videoSurface)
                            Log.d(TAG, "[KAIRO][PLAYER] Surface attached to new MediaPlayer for '${song.title}'")
                        } catch (e: Exception) {
                            Log.w(TAG, "[KAIRO][PLAYER] Could not attach initial surface: ${e.message}")
                        }
                    }

                    // Robust content URI and file path resolution
                    var dataSourceSet = false
                    val cleanPath = when {
                        effectiveUrl.startsWith("file://") -> effectiveUrl.removePrefix("file://")
                        else -> effectiveUrl
                    }
                    val localFile = java.io.File(cleanPath)

                    if (localFile.exists() && localFile.length() > 0) {
                        try {
                            setDataSource(localFile.absolutePath)
                            dataSourceSet = true
                            Log.d(TAG, "[KAIRO][PLAYER] Set data source directly from local file: ${localFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.w(TAG, "[KAIRO][PLAYER] setDataSource(localFile) failed: ${e.message}")
                        }
                    }

                    if (!dataSourceSet && effectiveUrl.startsWith("content://")) {
                        val uri = Uri.parse(effectiveUrl)
                        try {
                            setDataSource(context, uri)
                            dataSourceSet = true
                            Log.d(TAG, "[KAIRO][PLAYER] Set data source from content URI successfully")
                        } catch (e: Exception) {
                            Log.w(TAG, "[KAIRO][PLAYER] setDataSource(context, uri) failed: ${e.message}, attempting file cache copy")
                            try {
                                val ext = if (song.isVideo || song.hasVideo) "mp4" else "mp3"
                                val cacheFile = java.io.File(context.cacheDir, "play_${song.id}.$ext")
                                context.contentResolver.openInputStream(uri)?.use { inStream ->
                                    cacheFile.outputStream().use { outStream ->
                                        inStream.copyTo(outStream)
                                    }
                                }
                                if (cacheFile.exists() && cacheFile.length() > 0) {
                                    setDataSource(cacheFile.absolutePath)
                                    dataSourceSet = true
                                    Log.d(TAG, "[KAIRO][PLAYER] Set data source from cached copy: ${cacheFile.absolutePath}")
                                }
                            } catch (e2: Exception) {
                                Log.w(TAG, "[KAIRO][PLAYER] Cached copy fallback failed: ${e2.message}")
                            }
                        }
                    }

                    if (!dataSourceSet) {
                        try {
                            setDataSource(effectiveUrl)
                        } catch (_: Exception) {
                            setDataSource(context, Uri.parse(effectiveUrl))
                        }
                    }

                    setOnPreparedListener { mp ->
                        Log.d(TAG, "[KAIRO][PLAYER] Exact Track ready and playing: '${song.title}'")
                        if (videoSurface != null && videoSurface?.isValid == true) {
                            try {
                                mp.setSurface(videoSurface)
                            } catch (_: Exception) {}
                        }
                        val realDuration = try { mp.duration.toLong() } catch (_: Exception) { 0L }.coerceAtLeast(song.durationMs)
                        _playerState.value = _playerState.value.copy(
                            isBuffering = false,
                            isPlaying = true,
                            durationMs = realDuration,
                            currentSong = song
                        )
                        try {
                            mp.start()
                        } catch (e: Exception) {
                            Log.w(TAG, "[KAIRO][PLAYER] mp.start() failed: ${e.message}")
                        }
                        notificationManager.updateMetadata(song, realDuration, true, 0L)
                    }

                    setOnCompletionListener {
                        handleTrackEnded()
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.w(TAG, "[KAIRO][PLAYER] MediaPlayer error ($what, $extra) for '${song.title}'")
                        if (mediaPlayer === mp) {
                            safelyRecycleMediaPlayer()
                        } else {
                            try { mp.reset() } catch (_: Exception) {}
                            try { mp.release() } catch (_: Exception) {}
                        }

                        // Local tracks must never show stream unavailable or trigger online query
                        if (song.source == "Local") {
                            scope.launch(Dispatchers.Main) {
                                if (effectiveUrl.startsWith("content://")) {
                                    val copied = withContext(Dispatchers.IO) {
                                        try {
                                            val uri = Uri.parse(effectiveUrl)
                                            val ext = if (song.isVideo || song.hasVideo) "mp4" else "mp3"
                                            val fallbackFile = java.io.File(context.cacheDir, "recov_${song.id}.$ext")
                                            context.contentResolver.openInputStream(uri)?.use { inStream ->
                                                fallbackFile.outputStream().use { outStream ->
                                                    inStream.copyTo(outStream)
                                                }
                                            }
                                            if (fallbackFile.exists() && fallbackFile.length() > 0) fallbackFile.absolutePath else null
                                        } catch (_: Exception) { null }
                                    }
                                    if (copied != null) {
                                        Log.i(TAG, "[KAIRO][PLAYER] Retrying local playback via recovered file: $copied")
                                        loadAndPlayLocal(song.copy(audioUrl = copied), copied)
                                        return@launch
                                    }
                                }
                                _playerState.value = _playerState.value.copy(
                                    isBuffering = false,
                                    isPlaying = false,
                                    errorMessage = null
                                )
                            }
                            return@setOnErrorListener true
                        }

                        scope.launch(Dispatchers.Main) {
                            val fallback = withContext(Dispatchers.IO) {
                                com.example.core.music.LevyraMusicResolver.resolveExactSong(song.title, song.artist)
                            }
                            if (fallback != null && fallback.audioUrl.isNotBlank() && fallback.audioUrl != effectiveUrl) {
                                Log.i(TAG, "[KAIRO][PLAYER] Resolved fresh replacement stream for '${song.title}': ${fallback.audioUrl}")
                                val resolvedSong = song.copy(
                                    audioUrl = fallback.audioUrl,
                                    artworkUrl = fallback.artworkUrl ?: song.artworkUrl
                                )
                                loadAndPlayLocal(resolvedSong, fallback.audioUrl)
                                return@launch
                            }

                            val vid = extractVideoId(song)
                            if (!vid.isNullOrBlank()) {
                                Log.i(TAG, "[KAIRO][PLAYER] Stream error, falling back to YouTube for: $vid")
                                playViaYouTube(song, vid)
                                return@launch
                            }

                            _playerState.value = _playerState.value.copy(
                                isBuffering = false,
                                isPlaying = false,
                                errorMessage = null
                            )
                            delay(600)
                            if (_playerState.value.currentSong?.id == song.id && !_playerState.value.isPlaying) {
                                playNext()
                            }
                        }
                        true
                    }
                }

                mediaPlayer = player
                player.prepareAsync()
            } catch (e: Exception) {
                Log.e(TAG, "[KAIRO][PLAYER] Failed to prepare MediaPlayer for '${song.title}': ${e.message}")
                safelyRecycleMediaPlayer()
                val vid = extractVideoId(song)
                if (!vid.isNullOrBlank()) {
                    playViaYouTube(song, vid)
                } else {
                    _playerState.value = _playerState.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Failed to load '${song.title}'."
                    )
                }
            }
        }
    }

    private fun handleTrackEnded() {
        val state = _playerState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                if (isYouTubeActive) {
                    ytBridge.play()
                } else {
                    val player = mediaPlayer
                    if (player != null) {
                        try {
                            player.start()
                        } catch (e: Exception) {
                            state.currentSong?.let { loadAndPlayTrack(it) }
                        }
                    } else {
                        state.currentSong?.let { loadAndPlayTrack(it) }
                    }
                }
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                if (!isAdvancingTrack.compareAndSet(false, true)) {
                    Log.d(TAG, "[KAIRO][PLAYER] Track completion already handled, ignoring duplicate callback")
                    return
                }
                advancePlayback()
            }
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0.0f, 1.0f)
        if (isYouTubeActive) {
            ytBridge.setVolume((currentVolume * 100).toInt())
        } else {
            mediaPlayer?.let { player ->
                try {
                    player.setVolume(currentVolume, currentVolume)
                } catch (_: Exception) {}
            }
        }
    }

    fun getVolume(): Float = currentVolume

    fun togglePlayPause() {
        val current = _playerState.value
        val song = current.currentSong ?: return

        if (isYouTubeActive) {
            if (current.isPlaying) {
                ytBridge.pause()
                _playerState.value = current.copy(isPlaying = false)
                notificationManager.updateMetadata(song, current.durationMs, false, current.currentPositionMs)
            } else {
                ytBridge.play()
                _playerState.value = current.copy(isPlaying = true)
                notificationManager.updateMetadata(song, current.durationMs, true, current.currentPositionMs)
            }
        } else {
            val player = mediaPlayer
            if (player != null) {
                val isPlaying = try { player.isPlaying } catch (_: Exception) { false }
                if (isPlaying) {
                    try { player.pause() } catch (_: Exception) {}
                    _playerState.value = current.copy(isPlaying = false)
                    notificationManager.updateMetadata(song, current.durationMs, false, current.currentPositionMs)
                } else {
                    try {
                        player.start()
                        _playerState.value = current.copy(isPlaying = true)
                        notificationManager.updateMetadata(song, current.durationMs, true, current.currentPositionMs)
                    } catch (e: Exception) {
                        Log.w(TAG, "[KAIRO][PLAYER] start() failed on MediaPlayer ($e), reloading track")
                        loadAndPlayTrack(song)
                    }
                }
            } else {
                loadAndPlayTrack(song)
            }
        }
    }

    fun pause() {
        val current = _playerState.value
        val song = current.currentSong
        if (isYouTubeActive) {
            ytBridge.pause()
        } else {
            try { mediaPlayer?.pause() } catch (_: Exception) {}
        }
        _playerState.value = current.copy(isPlaying = false)
        if (song != null) {
            notificationManager.updateMetadata(song, current.durationMs, false, current.currentPositionMs)
        }
    }

    /**
     * Completely stops playback and removes notification.
     * Keeps app and notification 100% synchronized when user closes from notification bar.
     */
    fun stopPlayback() {
        val current = _playerState.value
        if (isYouTubeActive) {
            try { ytBridge.pause() } catch (_: Exception) {}
        } else {
            try { mediaPlayer?.pause() } catch (_: Exception) {}
        }
        _playerState.value = current.copy(isPlaying = false)
        notificationManager.clearNotification()
        Log.i(TAG, "[KAIRO][PLAYER] Playback stopped and notification cleared.")
    }

    fun playNext() {
        isAdvancingTrack.set(true)
        advancePlayback()
    }

    private fun advancePlayback() {
        val state = _playerState.value

        // Check if next track is supplied by authoritative queue manager
        if (onRequestNextTrack != null) {
            val next = onRequestNextTrack.invoke()
            if (next != null) {
                Log.d(TAG, "[KAIRO][PLAYER] Advancing to next track: '${next.title}' (${next.artist})")
                playSong(
                    song = next,
                    playbackMode = state.playbackMode,
                    activePlaylist = state.activePlaylist
                )
                return
            }
        }

        if (state.queue.isNotEmpty() && state.currentIndex < state.queue.lastIndex) {
            val nextIndex = state.currentIndex + 1
            playSong(
                song = state.queue[nextIndex],
                newQueue = state.queue,
                targetIndex = nextIndex,
                playbackMode = state.playbackMode,
                activePlaylist = state.activePlaylist
            )
            return
        }

        // End of playback
        _playerState.value = state.copy(isPlaying = false, currentPositionMs = 0)
        state.currentSong?.let {
            notificationManager.updateMetadata(it, state.durationMs, false, 0)
        }
    }

    fun playPrevious() {
        val state = _playerState.value
        if (state.currentPositionMs > 3000L) {
            seekTo(0)
            return
        }

        if (onRequestPreviousTrack != null) {
            val prev = onRequestPreviousTrack.invoke()
            if (prev != null) {
                playSong(
                    song = prev,
                    playbackMode = state.playbackMode,
                    activePlaylist = state.activePlaylist
                )
                return
            }
        }

        if (state.queue.isEmpty()) return

        val prevIndex = if (state.currentIndex > 0) {
            state.currentIndex - 1
        } else {
            0
        }

        val prevSong = state.queue[prevIndex]
        playSong(
            song = prevSong,
            newQueue = state.queue,
            targetIndex = prevIndex,
            playbackMode = state.playbackMode,
            activePlaylist = state.activePlaylist
        )
    }

    fun seekTo(positionMs: Long) {
        if (isYouTubeActive) {
            ytBridge.seekTo(positionMs)
            _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
        } else {
            mediaPlayer?.let { player ->
                try {
                    player.seekTo(positionMs.toInt())
                    _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
                } catch (_: Exception) {}
            }
        }
    }

    fun toggleShuffle() {
        val next = !_playerState.value.shuffleMode
        _playerState.value = _playerState.value.copy(shuffleMode = next)
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.value = _playerState.value.copy(repeatMode = nextMode)
    }

    fun addToQueue(song: Song) {
        playNextInQueue(song)
    }

    fun updateQueue(newQueue: List<Song>) {
        val currentSong = _playerState.value.currentSong
        val newIndex = if (currentSong != null) {
            newQueue.indexOfFirst { (it.videoId ?: it.id) == (currentSong.videoId ?: currentSong.id) }
                .let { if (it >= 0) it else _playerState.value.currentIndex }
        } else {
            _playerState.value.currentIndex
        }
        _playerState.value = _playerState.value.copy(
            queue = newQueue,
            currentIndex = newIndex
        )
    }

    fun playNextInQueue(song: Song) {
        val state = _playerState.value
        val already = state.manualPlayNextQueue.any { it.id == song.id || (it.videoId != null && it.videoId == song.videoId) }
        if (!already) {
            val updatedManual = state.manualPlayNextQueue + song
            _playerState.value = state.copy(manualPlayNextQueue = updatedManual)
        }
    }

    fun updateDisplayQueue(
        displayQueue: List<Song>,
        currentIndex: Int,
        playbackMode: PlaybackMode,
        manualQueue: List<Song>,
        autoplayQueue: List<Song>,
        activePlaylist: Playlist? = null,
        playlistIndex: Int = -1
    ) {
        _playerState.value = _playerState.value.copy(
            queue = displayQueue,
            currentIndex = currentIndex,
            playbackMode = playbackMode,
            manualPlayNextQueue = manualQueue,
            smartAutoplayQueue = autoplayQueue,
            activePlaylist = activePlaylist,
            playlistIndex = playlistIndex
        )
    }

    fun removeFromQueue(song: Song) {
        val state = _playerState.value
        val updatedQueue = state.queue.filter { it.id != song.id && (it.videoId == null || it.videoId != song.videoId) }
        val updatedManual = state.manualPlayNextQueue.filter { it.id != song.id && (it.videoId == null || it.videoId != song.videoId) }
        _playerState.value = state.copy(
            queue = updatedQueue,
            manualPlayNextQueue = updatedManual
        )
    }

    fun release() {
        progressJob?.cancel()
        try {
            ytBridge.stop()
        } catch (_: Exception) {}
        safelyRecycleMediaPlayer()
        notificationManager.release()
    }
}
