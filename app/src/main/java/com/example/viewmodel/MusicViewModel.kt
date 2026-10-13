package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.youtube.YouTubeMediaResolver
import com.example.data.SampleData
import com.example.data.StorageManager
import com.example.model.Artist
import com.example.model.LyricsData
import com.example.model.PlaybackMode
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.player.KairoAudioPlayer
import com.example.services.LyricsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
    PLAYING
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "KAIRO_VM"
    }

    private val storage = StorageManager(application)
    private val lyricsService = LyricsService()
    val offlineMusicManager = com.example.core.music.OfflineMusicManager(application)

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _savedYoutubeSongs = MutableStateFlow<List<Song>>(emptyList())
    val savedYoutubeSongs: StateFlow<List<Song>> = _savedYoutubeSongs.asStateFlow()

    private val _savedLikedSongs = MutableStateFlow<List<Song>>(emptyList())
    val savedLikedSongs: StateFlow<List<Song>> = _savedLikedSongs.asStateFlow()

    private val _savedHistorySongs = MutableStateFlow<List<Song>>(emptyList())
    val savedHistorySongs: StateFlow<List<Song>> = _savedHistorySongs.asStateFlow()

    private val _likedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSongIds: StateFlow<Set<String>> = _likedSongIds.asStateFlow()

    private val _historySongIds = MutableStateFlow<List<String>>(emptyList())
    val historySongIds: StateFlow<List<String>> = _historySongIds.asStateFlow()

    private val _followedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    val followedArtistIds: StateFlow<Set<String>> = _followedArtistIds.asStateFlow()

    // Library: ONLY explicitly user-created or user-saved playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    // Navigation and UI states
    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _isFullScreenPlayerOpen = MutableStateFlow(false)
    val isFullScreenPlayerOpen: StateFlow<Boolean> = _isFullScreenPlayerOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // YouTube search results (STRICTLY ISOLATED TO SEARCH TAB — NEVER ENTERS QUEUE OR LIBRARY AUTOMATICALLY)
    private val _youtubeSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val youtubeSearchResults: StateFlow<List<Song>> = _youtubeSearchResults.asStateFlow()
    private val _isSearchingYouTube = MutableStateFlow(false)
    val isSearchingYouTube: StateFlow<Boolean> = _isSearchingYouTube.asStateFlow()
    private val _searchErrorMessage = MutableStateFlow<String?>(null)
    val searchErrorMessage: StateFlow<String?> = _searchErrorMessage.asStateFlow()
    private val searchRequestCounter = AtomicLong(0)

    // Real-time Lyrics
    private val _currentLyrics = MutableStateFlow(LyricsData(emptyList(), "Loading lyrics...", false))
    val currentLyrics: StateFlow<LyricsData> = _currentLyrics.asStateFlow()
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    // QUEUE ARCHITECTURE: Strict separation between Manual Play Next Queue and Smart Autoplay Queue
    private val _manualPlayNextQueue = MutableStateFlow<List<Song>>(emptyList())
    val manualPlayNextQueue: StateFlow<List<Song>> = _manualPlayNextQueue.asStateFlow()
    val userQueue: StateFlow<List<Song>> = _manualPlayNextQueue // Backward-compatibility alias

    private val _smartAutoplayQueue = MutableStateFlow<List<Song>>(emptyList())
    val smartAutoplayQueue: StateFlow<List<Song>> = _smartAutoplayQueue.asStateFlow()
    val relatedQueue: StateFlow<List<Song>> = _smartAutoplayQueue // Backward-compatibility alias
    val relatedSongs: StateFlow<List<Song>> = _smartAutoplayQueue

    // Session-level playback history & played set (never repeat within current session)
    private val _sessionHistorySongs = MutableStateFlow<List<Song>>(emptyList())
    val sessionHistorySongs: StateFlow<List<Song>> = _sessionHistorySongs.asStateFlow()

    private val _sessionPlayedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val sessionPlayedSongIds: StateFlow<Set<String>> = _sessionPlayedSongIds.asStateFlow()

    private val _sessionPlayedSongTitles = MutableStateFlow<Set<String>>(emptySet())
    val sessionPlayedSongTitles: StateFlow<Set<String>> = _sessionPlayedSongTitles.asStateFlow()

    // Artist repetition tracker to avoid indefinite same-singer looping while prioritizing same artist
    private val _consecutiveArtistPlays = MutableStateFlow(0)
    private val _lastPlayedArtist = MutableStateFlow("")

    private val _isLoadingRelated = MutableStateFlow(false)
    val isLoadingRelated: StateFlow<Boolean> = _isLoadingRelated.asStateFlow()
    private var relatedJob: Job? = null

    // Explicit playlist context if playing a user playlist from Library
    private val _activePlaylistContext = MutableStateFlow<Playlist?>(null)

    // Dialog / Sheet states
    private val _contextMenuSong = MutableStateFlow<Song?>(null)
    val contextMenuSong: StateFlow<Song?> = _contextMenuSong.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen: StateFlow<Boolean> = _isCreatePlaylistDialogOpen.asStateFlow()

    private val _isShareDialogOpen = MutableStateFlow(false)
    val isShareDialogOpen: StateFlow<Boolean> = _isShareDialogOpen.asStateFlow()
    private val _shareSong = MutableStateFlow<Song?>(null)
    val shareSong: StateFlow<Song?> = _shareSong.asStateFlow()

    private val _isImportPlaylistDialogOpen = MutableStateFlow(false)
    val isImportPlaylistDialogOpen: StateFlow<Boolean> = _isImportPlaylistDialogOpen.asStateFlow()

    // Real YouTube Playlist Import states
    private val _isImportYouTubeDialogOpen = MutableStateFlow(false)
    val isImportYouTubeDialogOpen: StateFlow<Boolean> = _isImportYouTubeDialogOpen.asStateFlow()

    private val _isImportingYouTube = MutableStateFlow(false)
    val isImportingYouTube: StateFlow<Boolean> = _isImportingYouTube.asStateFlow()

    private val _importYouTubeErrorMessage = MutableStateFlow<String?>(null)
    val importYouTubeErrorMessage: StateFlow<String?> = _importYouTubeErrorMessage.asStateFlow()

    private val _isNotificationDialogOpen = MutableStateFlow(false)
    val isNotificationDialogOpen: StateFlow<Boolean> = _isNotificationDialogOpen.asStateFlow()

    private val _isFollowInstagramDialogOpen = MutableStateFlow(false)
    val isFollowInstagramDialogOpen: StateFlow<Boolean> = _isFollowInstagramDialogOpen.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private var searchJob: Job? = null

    // Authoritative Player Engine with strict priority callback
    val player: KairoAudioPlayer = KairoAudioPlayer(
        context = application,
        scope = viewModelScope,
        onSongStarted = { song ->
            storage.addToHistory(song)
            storage.saveLastPlayedSong(song)
            _historySongIds.value = storage.getHistorySongIds()
            _savedHistorySongs.value = storage.getHistorySongs()

            // Session history, played IDs, and played song titles tracking
            val songKey = song.videoId ?: song.id
            val normTitle = YouTubeMediaResolver.getNormalizedBaseTitle(song.title)
            val rawTitle = song.title.trim().lowercase()

            _sessionPlayedSongIds.value = _sessionPlayedSongIds.value + songKey + song.id
            _sessionPlayedSongTitles.value = _sessionPlayedSongTitles.value + normTitle + rawTitle

            // Purge played song and any song with the SAME NAME from manual queue
            _manualPlayNextQueue.value = _manualPlayNextQueue.value.filter {
                val k = it.videoId ?: it.id
                k != songKey && it.id != song.id &&
                YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != normTitle &&
                !it.title.trim().equals(song.title.trim(), ignoreCase = true)
            }.distinctBy { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) }

            // Purge played song and any song with the SAME NAME from smart autoplay / related queue
            _smartAutoplayQueue.value = _smartAutoplayQueue.value.filter {
                val k = it.videoId ?: it.id
                k != songKey && it.id != song.id &&
                YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != normTitle &&
                !it.title.trim().equals(song.title.trim(), ignoreCase = true)
            }.distinctBy { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) }

            val curHist = _sessionHistorySongs.value.filter {
                val k = it.videoId ?: it.id
                k != songKey && YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != normTitle
            }
            _sessionHistorySongs.value = curHist + song

            // Artist repetition tracker
            val artistName = song.artist.trim()
            if (artistName.equals(_lastPlayedArtist.value, ignoreCase = true)) {
                _consecutiveArtistPlays.value = _consecutiveArtistPlays.value + 1
            } else {
                _consecutiveArtistPlays.value = 1
                _lastPlayedArtist.value = artistName
            }

            loadLyricsForSong(song)
            // Generate recommendations strictly from CURRENT SONG, excluding all played songs & duplicate names
            prepareRelatedSongs(song)
            syncPlayerQueueState()
        },
        onRequestNextTrack = {
            resolveNextTrack()
        },
        onRequestPreviousTrack = {
            resolvePreviousTrack()
        }
    )

    val playerState = player.playerState

    // Catalog for Home & Library (Preserves all sample, local, imported, liked, and history songs)
    val allSongs: StateFlow<List<Song>> = combine(
        MutableStateFlow(SampleData.songs),
        _localSongs,
        _savedYoutubeSongs,
        _savedLikedSongs,
        _savedHistorySongs
    ) { sample, local, savedYt, liked, hist ->
        (sample + local + savedYt + liked + hist).distinctBy { it.videoId ?: it.id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.songs)

    // 24-Hour Trending Spotlight & Newly Released Tracks
    private val _spotlightSongs = MutableStateFlow<List<Song>>(emptyList())
    val spotlightSongs: StateFlow<List<Song>> = _spotlightSongs.asStateFlow()

    init {
        loadSavedData()
        viewModelScope.launch {
            allSongs.collect {
                updateSpotlightSongs()
            }
        }
    }

    private fun loadSavedData() {
        _likedSongIds.value = storage.getLikedSongIds()
        _savedLikedSongs.value = storage.getLikedSongs()
        _historySongIds.value = storage.getHistorySongIds()
        val histSongs = storage.getHistorySongs()
        _savedHistorySongs.value = histSongs
        _sessionPlayedSongTitles.value = histSongs.flatMap {
            listOf(YouTubeMediaResolver.getNormalizedBaseTitle(it.title), it.title.trim().lowercase())
        }.toSet()
        _followedArtistIds.value = storage.getFollowedArtistIds()
        _playlists.value = storage.getPlaylists()
        _localSongs.value = storage.getLocalSongs()
        _savedYoutubeSongs.value = storage.getYoutubeSongs()
        _isDarkTheme.value = storage.isDarkTheme()
        _notificationsEnabled.value = storage.areNotificationsEnabled()
        _isAutoplayEnabled.value = storage.isAutoplayEnabled()

        // Restore user's last played song if one exists from a previous session (empty on first app launch)
        val savedLast = storage.getLastPlayedSong()
        if (savedLast != null) {
            player.restoreLastPlayedSong(savedLast)
        }

        updateSpotlightSongs()
    }

    fun getLastPlayedSong(): Song? = storage.getLastPlayedSong()

    fun restoreLastPlayedSong(song: Song) {
        player.restoreLastPlayedSong(song)
    }

    /**
     * Refreshes Trending Spotlight songs with new releases and 24-hour daily rotation.
     * New songs appear deterministically every 24 hours.
     */
    fun updateSpotlightSongs() {
        viewModelScope.launch(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            val lastUpdate = storage.getSpotlightLastUpdateTime()
            // 24 hours in milliseconds = 86,400,000 ms
            val dayCycle = (now / 86400000L).toInt()

            if (now - lastUpdate >= 86400000L) {
                storage.setSpotlightLastUpdateTime(now)
            }

            val catalog = allSongs.value.ifEmpty { SampleData.songs }
            val newReleases = catalog.filter { it.isNewRelease }
            val otherTrending = catalog.filter { !it.isNewRelease }

            // 24-hour deterministic rotation: shift by dayCycle so new songs appear every 24 hours
            val rotatedNewReleases = if (newReleases.isNotEmpty()) {
                val shift = (dayCycle % newReleases.size).coerceAtLeast(0)
                newReleases.drop(shift) + newReleases.take(shift)
            } else emptyList()

            val rotatedTrending = if (otherTrending.isNotEmpty()) {
                val shift = ((dayCycle * 2) % otherTrending.size).coerceAtLeast(0)
                otherTrending.drop(shift) + otherTrending.take(shift)
            } else emptyList()

            val combined = mutableListOf<Song>()
            // Feature newly released tracks at the top
            combined.addAll(rotatedNewReleases.take(4))
            // Mix with 24h rotating trending tracks across languages
            for (song in rotatedTrending) {
                if (combined.none { (it.videoId ?: it.id) == (song.videoId ?: song.id) }) {
                    combined.add(song)
                }
                if (combined.size >= 8) break
            }

            if (combined.size < 6) {
                catalog.forEach { s ->
                    if (combined.none { (it.videoId ?: it.id) == (s.videoId ?: s.id) }) {
                        combined.add(s)
                    }
                }
            }

            _spotlightSongs.value = combined
            storage.saveSpotlightSongs(combined)
        }
    }

    private fun loadLyricsForSong(song: Song) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _currentLyrics.value = LyricsData(emptyList(), "Loading lyrics...", false)
            Log.d(TAG, "[KAIRO][LYRICS] Loading lyrics for ${song.title}")

            val lyrics = lyricsService.resolveLyrics(song)
            _currentLyrics.value = lyrics
            _isLoadingLyrics.value = false
            Log.d(TAG, "[KAIRO][LYRICS] Lyrics loaded: synced=${lyrics.isSynced}, lines=${lyrics.lines.size}")
        }
    }

    /**
     * Generates recommendations strictly from CURRENT SONG, not search queries.
     * Guarantees that no previously played song and NO song with the SAME NAME repeats in Related or Queue.
     */
    private fun prepareRelatedSongs(song: Song) {
        relatedJob?.cancel()
        relatedJob = viewModelScope.launch {
            _isLoadingRelated.value = true
            Log.d(TAG, "[KAIRO][RELATED] Generating dynamic recommendations based on CURRENT SONG: '${song.title}' (${song.artist})")

            val related = withContext(Dispatchers.IO) {
                YouTubeMediaResolver.getRelatedSongs(song, SampleData.songs)
            }
            val curVid = song.videoId ?: song.id
            val curNorm = YouTubeMediaResolver.getNormalizedBaseTitle(song.title)
            val curRaw = song.title.trim().lowercase()
            val sessionPlayed = _sessionPlayedSongIds.value
            val sessionTitles = _sessionPlayedSongTitles.value
            val history = _historySongIds.value.toSet()
            val manualQueue = _manualPlayNextQueue.value

            val seenIds = mutableSetOf<String>()
            seenIds.add(song.id)
            seenIds.add(curVid)
            seenIds.addAll(sessionPlayed)
            seenIds.addAll(history)
            manualQueue.forEach { m ->
                seenIds.add(m.id)
                m.videoId?.let { seenIds.add(it) }
            }

            val seenTitles = mutableSetOf<String>()
            seenTitles.add(curNorm)
            seenTitles.add(curRaw)
            seenTitles.addAll(sessionTitles)
            manualQueue.forEach { m ->
                seenTitles.add(YouTubeMediaResolver.getNormalizedBaseTitle(m.title))
                seenTitles.add(m.title.trim().lowercase())
            }

            fun isEligible(cand: Song): Boolean {
                val vid = cand.videoId ?: cand.id
                if (seenIds.contains(cand.id) || seenIds.contains(vid)) return false
                val cNorm = YouTubeMediaResolver.getNormalizedBaseTitle(cand.title)
                val cRaw = cand.title.trim().lowercase()
                if (seenTitles.contains(cNorm) || seenTitles.contains(cRaw)) return false
                return true
            }

            // Filter YouTube related recommendations
            val cleanRelated = related.filter { isEligible(it) }

            // Also find candidates in catalog matching same artist or same genre
            val curArtist = song.artist.trim()
            val curGenre = song.genre.trim()
            val catalogMatching = allSongs.value.filter { isEligible(it) }

            // Priority 1 candidates: same artist
            val sameArtistCandidates = catalogMatching.filter {
                it.artist.equals(curArtist, ignoreCase = true) ||
                        it.artist.contains(curArtist, ignoreCase = true) ||
                        curArtist.contains(it.artist, ignoreCase = true)
            }

            // Priority 3 candidates: same genre / language
            val sameGenreCandidates = catalogMatching.filter {
                it.genre.equals(curGenre, ignoreCase = true)
            }

            val combinedList = mutableListOf<Song>()
            for (cand in (sameArtistCandidates + cleanRelated + sameGenreCandidates + catalogMatching)) {
                if (isEligible(cand)) {
                    combinedList.add(cand)
                    seenIds.add(cand.id)
                    cand.videoId?.let { seenIds.add(it) }
                    seenTitles.add(YouTubeMediaResolver.getNormalizedBaseTitle(cand.title))
                    seenTitles.add(cand.title.trim().lowercase())
                }
            }

            _smartAutoplayQueue.value = combinedList
            _isLoadingRelated.value = false
            syncPlayerQueueState()
            Log.d(TAG, "[KAIRO][RELATED] Prepared ${_smartAutoplayQueue.value.size} recommendations for '${song.title}'")
        }
    }

    fun syncPlayerQueueState() {
        val curSong = player.playerState.value.currentSong
        val mode = player.playerState.value.playbackMode
        val manual = _manualPlayNextQueue.value

        val fullDisplayQueue: List<Song>
        val curIndex: Int

        val activePlaylist = _activePlaylistContext.value ?: player.playerState.value.activePlaylist
        if (mode == PlaybackMode.PLAYLIST && activePlaylist != null) {
            val plSongs = activePlaylist.songIds.mapNotNull { id ->
                allSongs.value.find { it.id == id || (it.videoId != null && it.videoId == id) }
            }
            val curPlIndex = if (curSong != null) {
                plSongs.indexOfFirst { it.id == curSong.id || (it.videoId != null && it.videoId == curSong.videoId) }
            } else -1

            fullDisplayQueue = if (manual.isNotEmpty() && curPlIndex >= 0) {
                val before = plSongs.take(curPlIndex + 1)
                val after = plSongs.drop(curPlIndex + 1)
                before + manual + after
            } else {
                plSongs
            }
            curIndex = if (curSong != null) {
                fullDisplayQueue.indexOfFirst { it.id == curSong.id || (it.videoId != null && it.videoId == curSong.videoId) }
                    .let { if (it >= 0) it else 0 }
            } else 0
        } else {
            // SMART_AUTOPLAY: Strictly purge played tracks and same-name tracks from upcoming queue
            val playedIds = _sessionPlayedSongIds.value
            val playedTitles = _sessionPlayedSongTitles.value
            val curNormTitle = curSong?.let { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) } ?: ""
            val curRawTitle = curSong?.title?.trim()?.lowercase() ?: ""

            val cleanUpcoming = mutableListOf<Song>()
            val seenQueueTitles = mutableSetOf<String>()
            val seenQueueIds = mutableSetOf<String>()

            if (curSong != null) {
                seenQueueTitles.add(curNormTitle)
                seenQueueTitles.add(curRawTitle)
                seenQueueIds.add(curSong.id)
                curSong.videoId?.let { seenQueueIds.add(it) }
            }
            seenQueueTitles.addAll(playedTitles)
            seenQueueIds.addAll(playedIds)

            val upcomingCandidates = manual + _smartAutoplayQueue.value
            for (item in upcomingCandidates) {
                val itemKey = item.videoId ?: item.id
                val itemNorm = YouTubeMediaResolver.getNormalizedBaseTitle(item.title)
                val itemRaw = item.title.trim().lowercase()

                if (!seenQueueIds.contains(item.id) &&
                    !seenQueueIds.contains(itemKey) &&
                    !seenQueueTitles.contains(itemNorm) &&
                    !seenQueueTitles.contains(itemRaw)) {
                    cleanUpcoming.add(item)
                    seenQueueIds.add(item.id)
                    seenQueueIds.add(itemKey)
                    seenQueueTitles.add(itemNorm)
                    seenQueueTitles.add(itemRaw)
                }
            }

            fullDisplayQueue = listOfNotNull(curSong) + cleanUpcoming
            curIndex = if (curSong != null) 0 else -1
        }

        player.updateDisplayQueue(
            displayQueue = fullDisplayQueue,
            currentIndex = curIndex,
            playbackMode = mode,
            manualQueue = manual,
            autoplayQueue = _smartAutoplayQueue.value,
            activePlaylist = activePlaylist,
            playlistIndex = if (mode == PlaybackMode.PLAYLIST) curIndex else -1
        )
    }

    /**
     * Resolves next track strictly adhering to user requirements:
     * 1. Priority 1: High-Priority Manual Queue ("Play Next" button) - preserves FIFO order
     * 2. Priority 2: Playlist strict sequence (ONLY applies to playlists: 1st song -> 2nd song in exact order)
     * 3. Priority 3: Smart Autoplay (ONLY for non-playlists: same artist first, then related, then same language/genre, no duplicate session tracks)
     */
    private fun resolveNextTrack(): Song? {
        val curSong = player.playerState.value.currentSong
        val mode = player.playerState.value.playbackMode
        val playedIds = _sessionPlayedSongIds.value
        val playedTitles = _sessionPlayedSongTitles.value
        val curKey = curSong?.let { it.videoId ?: it.id } ?: ""
        val curNorm = curSong?.let { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) } ?: ""
        val curRaw = curSong?.title?.trim()?.lowercase() ?: ""

        // -------------------------------------------------------------
        // PRIORITY 1: Manual "Play Next" Queue
        // High-priority user queue: immediately played after current track ends.
        // Multiple items preserve FIFO addition order.
        // Filter out any songs that have already been played or have the same name.
        // -------------------------------------------------------------
        val cleanManual = _manualPlayNextQueue.value.filter { m ->
            val mKey = m.videoId ?: m.id
            val mNorm = YouTubeMediaResolver.getNormalizedBaseTitle(m.title)
            val mRaw = m.title.trim().lowercase()
            mKey != curKey &&
            m.id != (curSong?.id ?: "") &&
            !playedIds.contains(m.id) &&
            !playedIds.contains(mKey) &&
            mNorm != curNorm &&
            mRaw != curRaw &&
            !playedTitles.contains(mNorm) &&
            !playedTitles.contains(mRaw)
        }.distinctBy { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) }
        _manualPlayNextQueue.value = cleanManual

        if (cleanManual.isNotEmpty()) {
            val nextManualSong = cleanManual.first()
            _manualPlayNextQueue.value = cleanManual.drop(1)
            Log.d(TAG, "[KAIRO][QUEUE] Priority 1: Playing manual Play Next track: '${nextManualSong.title}' (${nextManualSong.artist})")
            syncPlayerQueueState()
            return nextManualSong
        }

        // -------------------------------------------------------------
        // PRIORITY 2: Special Playlist Mode (Strict Playlist Order)
        // ONLY applies when user explicitly started playback from a playlist!
        // Track 1 -> Track 2 -> Track 3 -> ... -> End of playlist
        // NEVER replaces Track 2 with recommendation.
        // NEVER uses search results.
        // -------------------------------------------------------------
        val activePlaylist = _activePlaylistContext.value ?: player.playerState.value.activePlaylist
        if (mode == PlaybackMode.PLAYLIST && activePlaylist != null) {
            val playlistSongs = activePlaylist.songIds.mapNotNull { id ->
                val cleanId = if (id.startsWith("yt_")) id.removePrefix("yt_") else id
                allSongs.value.find {
                    it.id == id ||
                    it.id == cleanId ||
                    (it.videoId != null && (it.videoId == id || it.videoId == cleanId)) ||
                    (it.id.startsWith("yt_") && it.id.removePrefix("yt_") == cleanId)
                }
            }
            if (playlistSongs.isNotEmpty()) {
                val curIndex = if (curSong != null) {
                    playlistSongs.indexOfFirst {
                        it.id == curSong.id || (it.videoId != null && it.videoId == curSong.videoId)
                    }
                } else -1

                val nextIndex = curIndex + 1
                if (nextIndex in 0 until playlistSongs.size) {
                    val nextPlaylistSong = playlistSongs[nextIndex]
                    Log.d(TAG, "[KAIRO][PLAYLIST] Strict playlist sequence (Index $curIndex -> $nextIndex of ${playlistSongs.size}): '${nextPlaylistSong.title}'")
                    return nextPlaylistSong
                } else {
                    // Reached end of playlist
                    when (player.playerState.value.repeatMode) {
                        RepeatMode.ALL -> {
                            Log.d(TAG, "[KAIRO][PLAYLIST] Repeat ALL: Looping playlist to first track '${playlistSongs[0].title}'")
                            return playlistSongs[0]
                        }
                        RepeatMode.ONE -> {
                            return curSong ?: playlistSongs.last()
                        }
                        RepeatMode.OFF -> {
                            Log.d(TAG, "[KAIRO][PLAYLIST] Playlist finished with repeat OFF.")
                            return null
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PRIORITY 3: Smart Autoplay (For Search / Catalog / Non-Playlist Playback)
        // Activated when user plays a song from search results or catalog.
        // 1. Same artist (if unplayed, max 2 consecutive to keep variety).
        // 2. Songs related to currently playing song.
        // 3. Songs matching language, genre, or style (Haryanvi -> Haryanvi, Hindi -> Hindi).
        // 4. Similar mood / tempo preferences.
        // 5. Unplayed recommendations.
        // Never repeat previously played songs in current listening session.
        // Never repeat songs with the same name.
        // -------------------------------------------------------------
        if (_isAutoplayEnabled.value && curSong != null) {
            val curArtist = curSong.artist.trim()
            val curGenre = curSong.genre.trim()
            val consecutiveCount = _consecutiveArtistPlays.value

            val pool = (_smartAutoplayQueue.value + allSongs.value)
            val unplayedPool = pool.filter { cand ->
                val candKey = cand.videoId ?: cand.id
                val cNorm = YouTubeMediaResolver.getNormalizedBaseTitle(cand.title)
                val cRaw = cand.title.trim().lowercase()

                candKey != curKey &&
                cand.id != curSong.id &&
                !playedIds.contains(candKey) &&
                !playedIds.contains(cand.id) &&
                cNorm != curNorm &&
                cRaw != curRaw &&
                !playedTitles.contains(cNorm) &&
                !playedTitles.contains(cRaw)
            }.distinctBy { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) }

            // 1. Same singer / artist (if consecutive < 2 to maintain variety)
            if (consecutiveCount < 2) {
                val sameArtist = unplayedPool.firstOrNull { cand ->
                    cand.artist.equals(curArtist, ignoreCase = true) ||
                            cand.artist.contains(curArtist, ignoreCase = true) ||
                            curArtist.contains(cand.artist, ignoreCase = true)
                }
                if (sameArtist != null) {
                    val nextNorm = YouTubeMediaResolver.getNormalizedBaseTitle(sameArtist.title)
                    _smartAutoplayQueue.value = _smartAutoplayQueue.value.filter {
                        val k = it.videoId ?: it.id
                        k != (sameArtist.videoId ?: sameArtist.id) &&
                        YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != nextNorm
                    }
                    Log.d(TAG, "[KAIRO][AUTOPLAY] Selected Priority 1 (Same artist '${sameArtist.artist}'): '${sameArtist.title}'")
                    syncPlayerQueueState()
                    return sameArtist
                }
            }

            // 2. Related song
            val relatedCand = _smartAutoplayQueue.value.firstOrNull { cand ->
                val candKey = cand.videoId ?: cand.id
                val cNorm = YouTubeMediaResolver.getNormalizedBaseTitle(cand.title)
                val cRaw = cand.title.trim().lowercase()
                candKey != curKey &&
                cand.id != curSong.id &&
                !playedIds.contains(candKey) &&
                !playedIds.contains(cand.id) &&
                cNorm != curNorm &&
                cRaw != curRaw &&
                !playedTitles.contains(cNorm) &&
                !playedTitles.contains(cRaw)
            }
            if (relatedCand != null) {
                val nextNorm = YouTubeMediaResolver.getNormalizedBaseTitle(relatedCand.title)
                _smartAutoplayQueue.value = _smartAutoplayQueue.value.filter {
                    val k = it.videoId ?: it.id
                    k != (relatedCand.videoId ?: relatedCand.id) &&
                    YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != nextNorm
                }
                Log.d(TAG, "[KAIRO][AUTOPLAY] Selected Priority 2 (Related track): '${relatedCand.title}' (${relatedCand.artist})")
                syncPlayerQueueState()
                return relatedCand
            }

            // 3. Same genre / language / style (Haryanvi -> Haryanvi, Hindi -> Hindi)
            val sameLanguage = unplayedPool.firstOrNull { cand ->
                cand.genre.equals(curGenre, ignoreCase = true)
            }
            if (sameLanguage != null) {
                val nextNorm = YouTubeMediaResolver.getNormalizedBaseTitle(sameLanguage.title)
                _smartAutoplayQueue.value = _smartAutoplayQueue.value.filter {
                    val k = it.videoId ?: it.id
                    k != (sameLanguage.videoId ?: sameLanguage.id) &&
                    YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != nextNorm
                }
                Log.d(TAG, "[KAIRO][AUTOPLAY] Selected Priority 3 (Same language/genre '$curGenre'): '${sameLanguage.title}' (${sameLanguage.artist})")
                syncPlayerQueueState()
                return sameLanguage
            }

            // 4. Any unplayed track
            val unplayedFallback = unplayedPool.firstOrNull()
            if (unplayedFallback != null) {
                val nextNorm = YouTubeMediaResolver.getNormalizedBaseTitle(unplayedFallback.title)
                _smartAutoplayQueue.value = _smartAutoplayQueue.value.filter {
                    val k = it.videoId ?: it.id
                    k != (unplayedFallback.videoId ?: unplayedFallback.id) &&
                    YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != nextNorm
                }
                Log.d(TAG, "[KAIRO][AUTOPLAY] Selected Priority 4 (Unplayed recommendation): '${unplayedFallback.title}'")
                syncPlayerQueueState()
                return unplayedFallback
            }

            // 5. Fallback loop if all tracks played: pick something with a different title
            val fallbackAny = pool.firstOrNull {
                val k = it.videoId ?: it.id
                k != curKey &&
                it.id != curSong.id &&
                YouTubeMediaResolver.getNormalizedBaseTitle(it.title) != curNorm
            }
            if (fallbackAny != null) {
                Log.d(TAG, "[KAIRO][AUTOPLAY] Looping fallback: '${fallbackAny.title}'")
                return fallbackAny
            }
        }

        return null
    }

    private fun resolvePreviousTrack(): Song? {
        val mode = player.playerState.value.playbackMode
        val activePlaylist = _activePlaylistContext.value ?: player.playerState.value.activePlaylist

        // In Playlist Mode: strict previous playlist song
        if (mode == PlaybackMode.PLAYLIST && activePlaylist != null) {
            val playlistSongs = activePlaylist.songIds.mapNotNull { id ->
                val cleanId = if (id.startsWith("yt_")) id.removePrefix("yt_") else id
                allSongs.value.find {
                    it.id == id ||
                    it.id == cleanId ||
                    (it.videoId != null && (it.videoId == id || it.videoId == cleanId)) ||
                    (it.id.startsWith("yt_") && it.id.removePrefix("yt_") == cleanId)
                }
            }
            if (playlistSongs.isNotEmpty()) {
                val curSong = player.playerState.value.currentSong
                val curIndex = if (curSong != null) {
                    playlistSongs.indexOfFirst {
                        it.id == curSong.id || (it.videoId != null && it.videoId == curSong.videoId)
                    }
                } else 0

                val prevIndex = if (curIndex > 0) curIndex - 1 else 0
                val prevSong = playlistSongs[prevIndex]
                Log.d(TAG, "[KAIRO][PLAYLIST] Playlist Previous ($curIndex -> $prevIndex): '${prevSong.title}'")
                return prevSong
            }
        }

        // For non-playlist: use actual session playback history
        val history = _sessionHistorySongs.value
        val prevSong = history.dropLast(1).lastOrNull()
        if (prevSong != null) {
            Log.d(TAG, "[KAIRO][PLAYER] Playing previous track from session history: '${prevSong.title}'")
            return prevSong
        }

        val historyIds = _historySongIds.value
        val prevId = historyIds.getOrNull(1)
        if (prevId != null) {
            return allSongs.value.find { it.id == prevId } ?: SampleData.songs.find { it.id == prevId }
        }

        return null
    }

    /**
     * SEARCH RESULT PLAYBACK — EXACT SONG ONLY.
     * When user taps Song A in search, only Song A plays.
     * Search results B, C, D NEVER become the queue!
     */
    fun playFromSearch(song: Song) {
        Log.d(TAG, "[KAIRO][SEARCH] User selected exact song from search: '${song.title}' (videoId: ${song.videoId})")
        _activePlaylistContext.value = null
        player.playSong(
            song = song,
            newQueue = listOf(song),
            targetIndex = 0,
            playbackMode = PlaybackMode.SMART_AUTOPLAY,
            activePlaylist = null
        )
        _isFullScreenPlayerOpen.value = true
        syncPlayerQueueState()
    }

    fun playSong(song: Song, queue: List<Song>? = null) {
        _activePlaylistContext.value = null
        storage.saveLastPlayedSong(song)
        if (queue != null) {
            val idx = queue.indexOfFirst { it.id == song.id || (it.videoId != null && it.videoId == song.videoId) }.let { if (it >= 0) it else 0 }
            player.playSong(
                song = song,
                newQueue = queue,
                targetIndex = idx,
                playbackMode = PlaybackMode.SMART_AUTOPLAY,
                activePlaylist = null
            )
        } else {
            player.playSong(
                song = song,
                newQueue = listOf(song),
                targetIndex = 0,
                playbackMode = PlaybackMode.SMART_AUTOPLAY,
                activePlaylist = null
            )
        }
        syncPlayerQueueState()
    }

    fun playPlaylist(playlist: Playlist, startSong: Song? = null) {
        _activePlaylistContext.value = playlist
        // Clear manual queue when explicitly starting a new playlist context
        _manualPlayNextQueue.value = emptyList()

        val playlistSongs = playlist.songIds.mapNotNull { id ->
            val cleanId = if (id.startsWith("yt_")) id.removePrefix("yt_") else id
            allSongs.value.find {
                it.id == id ||
                it.id == cleanId ||
                (it.videoId != null && (it.videoId == id || it.videoId == cleanId)) ||
                (it.id.startsWith("yt_") && it.id.removePrefix("yt_") == cleanId)
            }
        }

        if (playlistSongs.isNotEmpty()) {
            val targetSong = if (startSong != null) {
                playlistSongs.find { it.id == startSong.id || (it.videoId != null && it.videoId == startSong.videoId) } ?: playlistSongs.first()
            } else {
                playlistSongs.first()
            }
            val targetIndex = playlistSongs.indexOfFirst { it.id == targetSong.id || (it.videoId != null && it.videoId == targetSong.videoId) }.coerceAtLeast(0)

            player.playSong(
                song = targetSong,
                newQueue = playlistSongs,
                targetIndex = targetIndex,
                playbackMode = PlaybackMode.PLAYLIST,
                activePlaylist = playlist
            )
            syncPlayerQueueState()
            Log.d(TAG, "[KAIRO][PLAYLIST] Activated Playlist Mode for '${playlist.name}' starting with '${targetSong.title}' (Index $targetIndex). Next track will strictly be the next playlist track.")
        }
    }

    // USER QUEUE & PLAY NEXT OPERATIONS
    fun addToUserQueue(song: Song) {
        playNextSong(song)
    }

    fun playNextSong(song: Song) {
        val curSong = player.playerState.value.currentSong
        val songNorm = YouTubeMediaResolver.getNormalizedBaseTitle(song.title)
        val songRaw = song.title.trim().lowercase()
        val curNorm = curSong?.let { YouTubeMediaResolver.getNormalizedBaseTitle(it.title) } ?: ""

        if (curSong != null && (curSong.id == song.id || (curSong.videoId != null && curSong.videoId == song.videoId) || curNorm == songNorm)) {
            Log.d(TAG, "[KAIRO][PLAY_NEXT] Song '${song.title}' or same name is already currently playing.")
            return
        }

        val alreadyQueued = _manualPlayNextQueue.value.any {
            it.id == song.id || (it.videoId != null && it.videoId == song.videoId) ||
            YouTubeMediaResolver.getNormalizedBaseTitle(it.title) == songNorm ||
            it.title.trim().equals(song.title.trim(), ignoreCase = true)
        }
        if (alreadyQueued) {
            Log.d(TAG, "[KAIRO][PLAY_NEXT] Song '${song.title}' or same name is already in Play Next queue.")
            return
        }

        val wasPlayed = _sessionPlayedSongIds.value.contains(song.id) ||
                (song.videoId != null && _sessionPlayedSongIds.value.contains(song.videoId)) ||
                _sessionPlayedSongTitles.value.contains(songNorm) ||
                _sessionPlayedSongTitles.value.contains(songRaw)
        if (wasPlayed) {
            Log.d(TAG, "[KAIRO][PLAY_NEXT] Song '${song.title}' was already played in this session. Skipping repetition.")
            return
        }

        // FIFO preservation: appended so earliest click plays first
        _manualPlayNextQueue.value = _manualPlayNextQueue.value + song
        player.playNextInQueue(song)
        syncPlayerQueueState()
        Log.d(TAG, "[KAIRO][PLAY_NEXT] Added '${song.title}' to manual Play Next queue (Position #${_manualPlayNextQueue.value.size}). Will play immediately after current track ends.")
    }

    fun playNextInUserQueue(song: Song) {
        playNextSong(song)
    }

    fun removeFromUserQueue(index: Int) {
        if (index in _manualPlayNextQueue.value.indices) {
            val updated = _manualPlayNextQueue.value.toMutableList()
            val removed = updated.removeAt(index)
            _manualPlayNextQueue.value = updated
            player.removeFromQueue(removed)
            syncPlayerQueueState()
        }
    }

    fun removeTrackFromQueue(song: Song) {
        _manualPlayNextQueue.value = _manualPlayNextQueue.value.filter { it.id != song.id && (it.videoId == null || it.videoId != song.videoId) }
        _smartAutoplayQueue.value = _smartAutoplayQueue.value.filter { it.id != song.id && (it.videoId == null || it.videoId != song.videoId) }
        player.removeFromQueue(song)
        syncPlayerQueueState()
    }

    fun clearUserQueue() {
        _manualPlayNextQueue.value = emptyList()
        syncPlayerQueueState()
    }

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openFullScreenPlayer() {
        _isFullScreenPlayerOpen.value = true
    }

    fun closeFullScreenPlayer() {
        _isFullScreenPlayerOpen.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _youtubeSearchResults.value = emptyList()
            _isSearchingYouTube.value = false
            _searchErrorMessage.value = null
            return
        }

        val currentRequestId = searchRequestCounter.incrementAndGet()

        searchJob = viewModelScope.launch {
            delay(350) // 350ms debounce
            performYouTubeSearch(trimmed, currentRequestId)
        }
    }

    fun executeImmediateSearch(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val currentRequestId = searchRequestCounter.incrementAndGet()
        viewModelScope.launch {
            performYouTubeSearch(trimmed, currentRequestId)
        }
    }

    private suspend fun performYouTubeSearch(query: String, requestId: Long) {
        _isSearchingYouTube.value = true
        _searchErrorMessage.value = null
        try {
            // Levyra-style multi-source resolution: Fetch high-fidelity direct CDN streams and YouTube results concurrently
            val levyraDeferred = viewModelScope.launch {
                // background fetch
            }
            
            val directStreams = try {
                com.example.core.music.LevyraMusicResolver.searchSongs(query)
            } catch (e: Exception) {
                Log.w(TAG, "[KAIRO][LEVYRA_SEARCH] Direct stream search error: ${e.message}")
                emptyList()
            }

            val ytResults = try {
                YouTubeMediaResolver.search(query)
            } catch (e: Exception) {
                Log.w(TAG, "[KAIRO][YT_SEARCH] YouTube search error: ${e.message}")
                emptyList()
            }

            // Combine results prioritizing high quality direct lossless/320kbps streams (Levyra) followed by YouTube video tracks
            val combinedResults = (directStreams + ytResults).distinctBy { 
                if (it.source == "YouTube") (it.videoId ?: it.id) else "${it.title.lowercase()}_${it.artist.lowercase()}"
            }

            if (requestId == searchRequestCounter.get()) {
                _youtubeSearchResults.value = combinedResults
                _isSearchingYouTube.value = false
                if (combinedResults.isEmpty()) {
                    Log.d(TAG, "[KAIRO][SEARCH] No results found for '$query'")
                } else {
                    Log.d(TAG, "[KAIRO][SEARCH] Loaded ${combinedResults.size} tracks (${directStreams.size} direct streams, ${ytResults.size} YouTube)")
                }
            }
        } catch (e: Exception) {
            if (requestId == searchRequestCounter.get()) {
                _searchErrorMessage.value = "Unable to search songs. Check your internet connection."
                _isSearchingYouTube.value = false
                Log.w(TAG, "[KAIRO][SEARCH] Search error: ${e.message}")
            }
        }
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        storage.setDarkTheme(next)
    }

    fun toggleNotifications() {
        val next = !_notificationsEnabled.value
        _notificationsEnabled.value = next
        storage.setNotificationsEnabled(next)
    }

    fun toggleAutoplay() {
        val next = !_isAutoplayEnabled.value
        _isAutoplayEnabled.value = next
        storage.setAutoplayEnabled(next)
        Log.d(TAG, "[KAIRO][AUTOPLAY] Autoplay toggled: $next")
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume)
    }

    fun toggleLike(song: Song) {
        storage.toggleLikeSong(song)
        _savedLikedSongs.value = storage.getLikedSongs()
        _likedSongIds.value = storage.getLikedSongIds()
    }

    fun toggleLike(songId: String) {
        val song = allSongs.value.find { it.id == songId || it.videoId == songId }
            ?: _youtubeSearchResults.value.find { it.id == songId || it.videoId == songId }
            ?: _manualPlayNextQueue.value.find { it.id == songId || it.videoId == songId }
            ?: _smartAutoplayQueue.value.find { it.id == songId || it.videoId == songId }
            ?: player.playerState.value.currentSong
        if (song != null) {
            toggleLike(song)
        } else {
            storage.toggleLikeSongById(songId)
            _savedLikedSongs.value = storage.getLikedSongs()
            _likedSongIds.value = storage.getLikedSongIds()
        }
    }

    fun isSongLiked(songId: String): Boolean {
        return _likedSongIds.value.contains(songId)
    }

    fun toggleFollowArtist(artistId: String) {
        storage.toggleFollowArtist(artistId)
        _followedArtistIds.value = storage.getFollowedArtistIds()
    }

    fun isArtistFollowed(artistId: String): Boolean {
        return _followedArtistIds.value.contains(artistId)
    }

    // USER PLAYLIST OPERATIONS (Strictly User-created/User-saved)
    fun createPlaylist(name: String, description: String = "", initialSong: Song? = null) {
        if (name.isBlank()) return
        val targetSong = initialSong ?: _songToAddToPlaylist.value
        val cover = if (targetSong != null && targetSong.artworkUrl.isNotBlank()) {
            targetSong.artworkUrl
        } else {
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        }
        val initialSongIds = if (targetSong != null) listOf(targetSong.id) else emptyList()

        val newPlaylist = Playlist(
            id = "pl_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            description = description.trim(),
            coverUrl = cover,
            songIds = initialSongIds
        )
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        storage.savePlaylists(updated)
        _isCreatePlaylistDialogOpen.value = false

        // Cache the song if it's from online/YouTube
        if (targetSong != null) {
            val exists = _savedYoutubeSongs.value.any { it.id == targetSong.id || (it.videoId != null && it.videoId == targetSong.videoId) }
            if (!exists) {
                val newSaved = _savedYoutubeSongs.value + targetSong
                _savedYoutubeSongs.value = newSaved
                storage.saveYoutubeSongs(newSaved)
            }
            _songToAddToPlaylist.value = null
        }
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        storage.savePlaylists(updated)
        if (_selectedPlaylist.value?.id == playlistId) {
            _selectedPlaylist.value = null
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        val songToAdd = _songToAddToPlaylist.value ?: allSongs.value.find {
            it.id == songId || it.videoId == songId || (songId.startsWith("yt_") && it.videoId == songId.removePrefix("yt_"))
        }
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                val newSongIds = if (!pl.songIds.contains(songId)) pl.songIds + songId else pl.songIds
                // If playlist has default cover or empty, and this song has cover art, update coverUrl
                val newCover = if (pl.coverUrl.contains("unsplash") && songToAdd != null && songToAdd.artworkUrl.isNotBlank()) {
                    songToAdd.artworkUrl
                } else pl.coverUrl
                pl.copy(songIds = newSongIds, coverUrl = newCover)
            } else pl
        }
        _playlists.value = updated
        storage.savePlaylists(updated)
        if (_selectedPlaylist.value?.id == playlistId) {
            _selectedPlaylist.value = updated.find { it.id == playlistId }
        }

        // Ensure the added song is permanently cached in _savedYoutubeSongs if it's external
        if (songToAdd != null) {
            val exists = _savedYoutubeSongs.value.any { it.id == songToAdd.id || (it.videoId != null && it.videoId == songToAdd.videoId) }
            if (!exists) {
                val newSaved = _savedYoutubeSongs.value + songToAdd
                _savedYoutubeSongs.value = newSaved
                storage.saveYoutubeSongs(newSaved)
            }
        }

        _songToAddToPlaylist.value = null
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val cleanSongId = if (songId.startsWith("yt_")) songId.removePrefix("yt_") else songId
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songIds = pl.songIds.filter { id ->
                    val cleanId = if (id.startsWith("yt_")) id.removePrefix("yt_") else id
                    id != songId && cleanId != cleanSongId
                })
            } else pl
        }
        _playlists.value = updated
        storage.savePlaylists(updated)
        if (_selectedPlaylist.value?.id == playlistId) {
            _selectedPlaylist.value = updated.find { it.id == playlistId }
        }
    }

    fun openContextMenu(song: Song) {
        _contextMenuSong.value = song
    }

    fun closeContextMenu() {
        _contextMenuSong.value = null
    }

    fun openAddToPlaylistSheet(song: Song) {
        _songToAddToPlaylist.value = song
        _contextMenuSong.value = null
    }

    fun closeAddToPlaylistSheet() {
        _songToAddToPlaylist.value = null
    }

    fun setCreatePlaylistDialogOpen(open: Boolean) {
        _isCreatePlaylistDialogOpen.value = open
    }

    fun openShareDialog(song: Song) {
        _shareSong.value = song
        _isShareDialogOpen.value = true
        _contextMenuSong.value = null
    }

    fun closeShareDialog() {
        _isShareDialogOpen.value = false
        _shareSong.value = null
    }

    fun setImportPlaylistDialogOpen(open: Boolean) {
        _isImportPlaylistDialogOpen.value = open
    }

    fun setImportYouTubeDialogOpen(open: Boolean) {
        _isImportYouTubeDialogOpen.value = open
        if (!open) {
            _importYouTubeErrorMessage.value = null
        }
    }

    /**
     * YOUTUBE PLAYLIST IMPORT (Requirement 10, 11, 12, 13)
     * Automatically saves to Library with exact name & thumbnail, and downloads for offline playback.
     */
    fun importYouTubePlaylist(urlOrInput: String, playImmediately: Boolean = true, saveToLibrary: Boolean = false) {
        val trimmed = urlOrInput.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _isImportingYouTube.value = true
            _importYouTubeErrorMessage.value = null
            Log.d(TAG, "[KAIRO][IMPORT] Starting YouTube import for: $trimmed (saveToLibrary=$saveToLibrary)")

            try {
                val (title, songs) = YouTubeMediaResolver.importPlaylist(trimmed)
                if (songs.isEmpty()) {
                    _importYouTubeErrorMessage.value = "No playable tracks found for this YouTube link. Please check URL."
                    _isImportingYouTube.value = false
                    return@launch
                }

                // Automatically save imported YouTube playlist to Library with same name & same thumbnail
                val coverThumbnail = songs.firstOrNull()?.artworkUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
                val resolvedSongs = songs.map { s ->
                    _savedYoutubeSongs.value.find { it.videoId != null && it.videoId == s.videoId } ?: s
                }
                val newPlaylist = Playlist(
                    id = "pl_yt_" + UUID.randomUUID().toString().take(8),
                    name = title,
                    description = "${resolvedSongs.size} tracks imported from YouTube • Offline Ready",
                    coverUrl = coverThumbnail,
                    songIds = resolvedSongs.map { it.id }
                )

                val updatedPlaylists = _playlists.value + newPlaylist
                _playlists.value = updatedPlaylists
                storage.savePlaylists(updatedPlaylists)

                val existingVideoIds = _savedYoutubeSongs.value.mapNotNull { it.videoId }.toSet()
                val existingIds = _savedYoutubeSongs.value.map { it.id }.toSet()
                val newSongsToAdd = songs.filter { (it.videoId == null || !existingVideoIds.contains(it.videoId)) && !existingIds.contains(it.id) }
                val updatedYtSongs = _savedYoutubeSongs.value + newSongsToAdd
                _savedYoutubeSongs.value = updatedYtSongs
                storage.saveYoutubeSongs(updatedYtSongs)
                Log.d(TAG, "[KAIRO][IMPORT] Successfully saved playlist '${title}' (${resolvedSongs.size} songs) automatically to Library")

                // Automatically download playlist tracks in background for completely offline playback!
                viewModelScope.launch {
                    try {
                        val downloadedList = offlineMusicManager.downloadPlaylist(title, songs) { downloadedSong ->
                            val currentYt = _savedYoutubeSongs.value.map {
                                if (it.id == downloadedSong.id) downloadedSong else it
                            }
                            _savedYoutubeSongs.value = currentYt
                            storage.saveYoutubeSongs(currentYt)
                        }
                        Log.i(TAG, "[KAIRO][OFFLINE] Auto-download finished for playlist '$title' (${downloadedList.size} tracks)")
                    } catch (e: Exception) {
                        Log.w(TAG, "[KAIRO][OFFLINE] Offline background download error: ${e.message}")
                    }
                }

                _isImportingYouTube.value = false
                _isImportYouTubeDialogOpen.value = false

                if (playImmediately && songs.isNotEmpty()) {
                    player.playSong(songs.first(), songs)
                    _isFullScreenPlayerOpen.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "[KAIRO][IMPORT] Failed to import YouTube playlist: ${e.message}")
                _importYouTubeErrorMessage.value = "Unable to load playlist. Please check internet connection."
                _isImportingYouTube.value = false
            }
        }
    }

    fun onAppClosed() {
        Log.i(TAG, "[KAIRO] onAppClosed: stopping audio and dismissing notification")
        player.stopPlayback()
    }

    fun setNotificationDialogOpen(open: Boolean) {
        _isNotificationDialogOpen.value = open
    }

    fun setFollowInstagramDialogOpen(open: Boolean) {
        _isFollowInstagramDialogOpen.value = open
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun selectArtist(artist: Artist?) {
        _selectedArtist.value = artist
    }

    fun clearHistory() {
        storage.clearHistory()
        _historySongIds.value = emptyList()
    }

    fun importAudioFiles(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val retriever = MediaMetadataRetriever()
            val importedList = mutableListOf<Song>()
            val mediaDir = java.io.File(context.filesDir, "imported_media").apply { mkdirs() }
            val thumbDir = java.io.File(context.filesDir, "imported_thumbs").apply { mkdirs() }

            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val songId = "loc_" + UUID.randomUUID().toString().take(8)

                // Quick pre-check for MP4/Video vs Audio
                val mimeType = try { context.contentResolver.getType(uri) ?: "" } catch (_: Exception) { "" }
                val uriStr = uri.toString()
                val lastSeg = uri.lastPathSegment ?: ""
                val isMp4Pre = uriStr.contains(".mp4", ignoreCase = true) ||
                        lastSeg.contains(".mp4", ignoreCase = true) ||
                        mimeType.contains("video", ignoreCase = true) ||
                        mimeType.contains("mp4", ignoreCase = true)

                val ext = if (isMp4Pre) "mp4" else "mp3"
                val destFile = java.io.File(mediaDir, "${songId}.$ext")

                var finalPath = uri.toString()
                try {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        destFile.outputStream().use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    if (destFile.exists() && destFile.length() > 0) {
                        finalPath = destFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[KAIRO][IMPORT] File copy attempt notice: ${e.message}")
                }

                try {
                    if (destFile.exists() && destFile.length() > 0) {
                        retriever.setDataSource(destFile.absolutePath)
                    } else {
                        retriever.setDataSource(context, uri)
                    }

                    val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                        ?: "Local Track"
                    val title = rawTitle.replace(".mp4", "", ignoreCase = true).trim()
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: "Local Artist"
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?: "Device Media"
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 210000L

                    val hasVideoMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                    val isMp4 = isMp4Pre ||
                            rawTitle.contains(".mp4", ignoreCase = true) ||
                            hasVideoMeta?.equals("yes", ignoreCase = true) == true

                    // Extract actual video frame thumbnail or embedded album picture
                    var artworkUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                    try {
                        val picBytes = retriever.embeddedPicture
                        if (picBytes != null) {
                            val artFile = java.io.File(thumbDir, "art_${songId}.jpg")
                            artFile.writeBytes(picBytes)
                            artworkUri = Uri.fromFile(artFile).toString()
                        } else if (isMp4) {
                            val frame = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.frameAtTime
                            if (frame != null) {
                                val artFile = java.io.File(thumbDir, "thumb_${songId}.jpg")
                                java.io.FileOutputStream(artFile).use { out ->
                                    frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                                }
                                artworkUri = Uri.fromFile(artFile).toString()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[KAIRO][IMPORT] Thumbnail extraction notice: ${e.message}")
                    }

                    val song = Song(
                        id = songId,
                        title = title,
                        artist = artist,
                        album = album,
                        artworkUrl = artworkUri,
                        audioUrl = finalPath,
                        durationMs = durationMs,
                        source = "Local",
                        genre = if (isMp4) "MP4 Video" else "Local Audio",
                        isVideo = isMp4
                    )
                    importedList.add(song)
                } catch (_: Exception) {
                    val fallbackRaw = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported Song"
                    val isMp4Fallback = isMp4Pre || fallbackRaw.contains(".mp4", ignoreCase = true)
                    importedList.add(
                        Song(
                            id = songId,
                            title = fallbackRaw.replace(".mp4", "", ignoreCase = true).trim(),
                            artist = "Local Artist",
                            album = "My Files",
                            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                            audioUrl = finalPath,
                            durationMs = 180000L,
                            source = "Local",
                            genre = if (isMp4Fallback) "MP4 Video" else "Local Audio",
                            isVideo = isMp4Fallback
                        )
                    )
                }
            }

            try { retriever.release() } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                val updated = _localSongs.value + importedList
                _localSongs.value = updated
                storage.saveLocalSongs(updated)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
