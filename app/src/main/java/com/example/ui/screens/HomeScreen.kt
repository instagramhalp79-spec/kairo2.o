package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Artist
import com.example.model.Song
import com.example.ui.components.ArtistCard
import com.example.ui.components.HeroBanner
import com.example.ui.components.HorizontalMusicCard
import com.example.ui.components.ListMusicCard
import com.example.ui.components.QuickPickItem
import com.example.ui.theme.KairoDarkElevated
import com.example.ui.theme.KairoRed

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    playedSongs: List<Song> = emptyList(),
    spotlightSongs: List<Song> = emptyList(),
    currentPlayingSongId: String? = null,
    artists: List<Artist>,
    followedArtistIds: Set<String>,
    onSongClick: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    onOpenContextMenu: (Song) -> Unit,
    onFollowArtistToggle: (String) -> Unit,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedArtistFilter by remember { mutableStateOf<Artist?>(null) }

    val categories = listOf("All", "Bollywood", "Punjabi", "Haryanvi", "English", "Tamil", "Bhojpuri", "Followed Artists")

    val heroSong = allSongs.firstOrNull() ?: return
    val quickPicks = remember(playedSongs) {
        playedSongs.distinctBy { it.videoId ?: it.id }.take(16)
    }
    val haryanviSongs = allSongs.filter { it.genre.contains("Haryanvi", ignoreCase = true) }
    val bollywoodSongs = allSongs.filter { it.genre.contains("Bollywood", ignoreCase = true) }
    val punjabiSongs = allSongs.filter { it.genre.contains("Punjabi", ignoreCase = true) }
    val englishSongs = allSongs.filter { it.genre.contains("English", ignoreCase = true) }
    val tamilSongs = allSongs.filter { it.genre.contains("Tamil", ignoreCase = true) || it.genre.contains("South", ignoreCase = true) }
    val bhojpuriSongs = allSongs.filter { it.genre.contains("Bhojpuri", ignoreCase = true) }

    // Filtered songs based on category or selected artist
    val displayedSongs = when {
        selectedArtistFilter != null -> {
            allSongs.filter { it.artist.contains(selectedArtistFilter!!.name, ignoreCase = true) }
        }
        selectedCategory == "Bollywood" -> bollywoodSongs
        selectedCategory == "Punjabi" -> punjabiSongs
        selectedCategory == "Haryanvi" -> haryanviSongs
        selectedCategory == "English" -> englishSongs
        selectedCategory == "Tamil" -> tamilSongs
        selectedCategory == "Bhojpuri" -> bhojpuriSongs
        selectedCategory == "Followed Artists" -> {
            val followedNames = artists.filter { followedArtistIds.contains(it.id) }.map { it.name.lowercase() }
            allSongs.filter { song -> followedNames.any { song.artist.lowercase().contains(it) } }
        }
        else -> allSongs
    }

    // 24-Hour Trending Spotlight & New Releases collection
    val effectiveSpotlightSongs = remember(spotlightSongs, allSongs) {
        if (spotlightSongs.isNotEmpty()) {
            spotlightSongs
        } else {
            val list = mutableListOf<Song>()
            bollywoodSongs.firstOrNull()?.let { list.add(it) }
            punjabiSongs.firstOrNull()?.let { list.add(it) }
            haryanviSongs.firstOrNull()?.let { list.add(it) }
            englishSongs.firstOrNull()?.let { list.add(it) }
            tamilSongs.firstOrNull()?.let { list.add(it) }
            bhojpuriSongs.firstOrNull()?.let { list.add(it) }
            bollywoodSongs.getOrNull(1)?.let { list.add(it) }
            punjabiSongs.getOrNull(1)?.let { list.add(it) }
            if (list.size < 4) allSongs.take(8) else list
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Spotlight Banner (Auto-changes every 5 seconds, swipe left/right, glowing red aura)
        item {
            HeroBanner(
                songs = effectiveSpotlightSongs,
                onPlayClick = { onSongClick(it) },
                onQueueClick = { onQueueSong(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Category Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category && selectedArtistFilter == null
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = category
                            selectedArtistFilter = null
                        },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KairoRed,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("filter_chip_$category")
                    )
                }
            }
        }

        // Selected Artist Spotlight Section (when an artist is tapped on HomeScreen)
        item {
            AnimatedVisibility(
                visible = selectedArtistFilter != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedArtistFilter?.let { artist ->
                    val artistTracks = allSongs.filter { it.artist.contains(artist.name, ignoreCase = true) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(KairoDarkElevated)
                            .border(1.dp, KairoRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = artist.imageUrl,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, KairoRed, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = artist.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${artistTracks.size} Real Songs • ${artist.genre}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { selectedArtistFilter = null },
                                modifier = Modifier.size(28.dp).testTag("close_artist_spotlight")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (artistTracks.isNotEmpty()) {
                            Button(
                                onClick = {
                                    onSongClick(artistTracks.first())
                                    artistTracks.drop(1).forEach { onQueueSong(it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = KairoRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("play_all_artist_tracks")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All Songs (${artist.name}) in MP3", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Picks Section (Strictly shows played & auto-played songs, formatted as YouTube Music 4-row carousel)
        if (selectedCategory == "All" && selectedArtistFilter == null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeader(
                        title = "Quick Picks",
                        subtitle = if (quickPicks.isNotEmpty()) "Played & auto-played tracks • 1-Click Play" else "Play any song to start your Quick Picks",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (quickPicks.isNotEmpty()) {
                        val columns = remember(quickPicks) { quickPicks.chunked(4) }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(columns) { columnSongs ->
                                Column(
                                    modifier = Modifier.width(310.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    columnSongs.forEach { song ->
                                        QuickPickItem(
                                            song = song,
                                            isPlayingThis = song.id == currentPlayingSongId,
                                            onClick = { onSongClick(song) },
                                            onMenuClick = { onOpenContextMenu(song) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State if user hasn't played any tracks yet
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KairoDarkElevated)
                                .border(1.dp, KairoRed.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = KairoRed,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Your Quick Picks will appear here",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Only songs you play or auto-play are saved in Quick Picks",
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Indian Artists Section (All 28 artists)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Top Indian Artists",
                    subtitle = "All 28 artists • Tap artist to see and play their MP3 songs",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(artists) { artist ->
                        ArtistCard(
                            artist = artist,
                            isFollowed = followedArtistIds.contains(artist.id),
                            onArtistClick = {
                                selectedArtistFilter = artist
                            },
                            onFollowToggle = { onFollowArtistToggle(artist.id) }
                        )
                    }
                }
            }
        }

        // Trending Haryanvi Beats (Renuka Panwar, Masoom Sharma, Gulzaar, Sumit Goswami, Sapna Choudhary, etc.)
        if (selectedCategory == "All" || selectedCategory == "Haryanvi") {
            item {
                SongHorizontalSection(
                    title = "Trending Haryanvi Beats",
                    subtitle = "Real superhits by Renuka, Masoom, Gulzaar, Sapna & Sumit",
                    songs = haryanviSongs,
                    onSongClick = onSongClick,
                    onMenuClick = onOpenContextMenu
                )
            }
        }

        // Bollywood Melodies (Arijit Singh, Shreya Ghoshal, Jubin Nautiyal, Neha Kakkar)
        if (selectedCategory == "All" || selectedCategory == "Bollywood") {
            item {
                SongHorizontalSection(
                    title = "Bollywood Melodies",
                    subtitle = "Romantic soul tracks by Arijit Singh, Shreya Ghoshal & Jubin Nautiyal",
                    songs = bollywoodSongs,
                    onSongClick = onSongClick,
                    onMenuClick = onOpenContextMenu
                )
            }
        }

        // Punjabi Powerhouse (Sidhu Moose Wala, Diljit Dosanjh, Karan Aujla)
        if (selectedCategory == "All" || selectedCategory == "Punjabi") {
            item {
                SongHorizontalSection(
                    title = "Punjabi Powerhouse",
                    subtitle = "High energy hits by Sidhu Moose Wala, Diljit Dosanjh & Karan Aujla",
                    songs = punjabiSongs,
                    onSongClick = onSongClick,
                    onMenuClick = onOpenContextMenu
                )
            }
        }

        // All Real Songs & Artists Discography (Every single song available directly in MP3)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SectionHeader(
                    title = if (selectedArtistFilter != null) "Songs by ${selectedArtistFilter!!.name}" else "All Artists' Real Songs (MP3)",
                    subtitle = "${displayedSongs.size} Real songs ready to play in high-definition MP3"
                )
                Spacer(modifier = Modifier.height(10.dp))
                displayedSongs.forEach { song ->
                    ListMusicCard(
                        song = song,
                        onClick = { onSongClick(song) },
                        onMenuClick = { onOpenContextMenu(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongHorizontalSection(
    title: String,
    subtitle: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onMenuClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                HorizontalMusicCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onMenuClick = { onMenuClick(song) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
