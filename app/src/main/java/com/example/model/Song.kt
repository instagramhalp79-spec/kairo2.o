package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "KAIRO Music",
    val artworkUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val source: String = "Online", // "YouTube", "Online", or "Local"
    val lyrics: String? = null,
    val genre: String = "Pop",
    val videoId: String? = null,
    val isNewRelease: Boolean = false,
    val isVideo: Boolean = false
) {
    val hasVideo: Boolean
        get() = isVideo ||
                videoId != null ||
                source == "YouTube" ||
                audioUrl.contains(".mp4", ignoreCase = true) ||
                title.contains(".mp4", ignoreCase = true) ||
                genre.contains("MP4", ignoreCase = true) ||
                genre.contains("Video", ignoreCase = true)
}
