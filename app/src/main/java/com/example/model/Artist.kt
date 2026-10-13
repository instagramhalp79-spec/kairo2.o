package com.example.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val genre: String,
    val followerCount: String,
    val isFollowed: Boolean = false
)
