package com.example.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
