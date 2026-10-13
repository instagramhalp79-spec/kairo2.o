package com.example.core.music

import com.example.model.Song

/**
 * Direct Real Audio Stream Provider.
 * 
 * Verifies and delivers playable high-fidelity streams.
 */
object Mp3AudioStreamProvider {

    /**
     * Returns song.audioUrl if already valid, otherwise empty string
     * to trigger dynamic resolution by LevyraMusicResolver.
     */
    fun getMp3StreamForSong(song: Song): String {
        if (song.audioUrl.isNotBlank() &&
            (song.audioUrl.startsWith("http://") || song.audioUrl.startsWith("https://") ||
             song.audioUrl.startsWith("content://") || song.audioUrl.startsWith("file://")) &&
            !song.audioUrl.contains("soundhelix") &&
            !song.audioUrl.contains("youtube.com") &&
            !song.audioUrl.contains("youtu.be")
        ) {
            return song.audioUrl
        }
        return ""
    }
}
