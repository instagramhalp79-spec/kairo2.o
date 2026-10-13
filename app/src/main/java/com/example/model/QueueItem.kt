package com.example.model

enum class QueueSource {
    USER,       // Explicitly added by user ("Add to Queue")
    RELATED,    // Automatically recommended based on current song
    PLAYLIST,   // From explicit playlist in Library
    SEARCH      // Search result selection (isolated)
}

data class QueueItem(
    val song: Song,
    val source: QueueSource
)
