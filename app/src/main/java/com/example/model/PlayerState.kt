package com.example.model

enum class PlaybackMode {
    SMART_AUTOPLAY,
    PLAYLIST
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.SMART_AUTOPLAY,
    val manualPlayNextQueue: List<Song> = emptyList(),
    val smartAutoplayQueue: List<Song> = emptyList(),
    val activePlaylist: Playlist? = null,
    val playlistIndex: Int = -1,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val errorMessage: String? = null
)
