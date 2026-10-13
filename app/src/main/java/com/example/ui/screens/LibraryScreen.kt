package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.components.ListMusicCard
import com.example.ui.theme.KairoRed

enum class LibrarySubView {
    MAIN,
    LIKED_SONGS,
    HISTORY,
    LOCAL_MP3S
}

private data class LibraryThemeStyle(
    val background: Color,
    val headerBrandText: Color,
    val pillBg: Brush,
    val pillBorder: Color,
    val pillShadow: Color,
    val pillText: Color,
    val cardBg: Brush,
    val cardBorder: Brush,
    val cardShadow: Color,
    val tileBg: Color,
    val tileBorder: Color,
    val cardTitle: Color,
    val chevronTint: Color,
    val sectionTitle: Color,
    val playlistCardBg: Brush,
    val playlistBorder: Color,
    val playlistShadow: Color,
    val playlistSubtitle: Color,
    val moreIconTint: Color
)

@Composable
fun LibraryScreen(
    allSongs: List<Song>,
    likedSongIds: Set<String>,
    savedLikedSongs: List<Song> = emptyList(),
    historySongIds: List<String>,
    localSongs: List<Song>,
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    currentPlayingSongId: String?,
    isDarkTheme: Boolean = true,
    onSelectPlaylist: (Playlist?) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onSongInPlaylistClick: (Playlist, Song) -> Unit = { pl, _ -> onPlayPlaylist(pl) },
    onDeletePlaylist: (String) -> Unit,
    onOpenContextMenu: (Song) -> Unit,
    onClearHistory: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onImportPlaylistInfoClick: () -> Unit,
    onImportYouTubePlaylistClick: () -> Unit = {},
    onImportLocalFiles: (List<Uri>) -> Unit,
    onRemoveSongFromPlaylist: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportLocalFiles(uris)
        }
    }

    var currentSubView by remember { mutableStateOf(LibrarySubView.MAIN) }

    // Intercept back navigation when in sub-screens or viewing a playlist
    BackHandler(enabled = selectedPlaylist != null || currentSubView != LibrarySubView.MAIN) {
        if (selectedPlaylist != null) {
            onSelectPlaylist(null)
        } else {
            currentSubView = LibrarySubView.MAIN
        }
    }

    // Color definitions matching uploaded screenshots exactly for Dark and Light mode
    val theme = if (isDarkTheme) {
        LibraryThemeStyle(
            background = Color(0xFF070709),
            headerBrandText = Color(0xFF75757E),
            pillBg = Brush.verticalGradient(listOf(Color(0xFF22242D), Color(0xFF121319))),
            pillBorder = Color(0x99E50914),
            pillShadow = Color(0x80E50914),
            pillText = Color.White,
            cardBg = Brush.verticalGradient(listOf(Color(0xFF1B1C25), Color(0xFF0E0F14))),
            cardBorder = Brush.horizontalGradient(listOf(Color(0xFFE50914), Color(0x50E50914), Color(0xFFE50914))),
            cardShadow = Color(0x60E50914),
            tileBg = Color(0xFF14151D),
            tileBorder = Color(0x80E50914),
            cardTitle = Color.White,
            chevronTint = Color(0xFF8E8E98),
            sectionTitle = Color(0xFF8E8E98),
            playlistCardBg = Brush.verticalGradient(listOf(Color(0xFF1B1C25), Color(0xFF0E0F14))),
            playlistBorder = Color(0x90E50914),
            playlistShadow = Color(0x55E50914),
            playlistSubtitle = Color(0xFF8E8E98),
            moreIconTint = Color(0xFF8E8E98)
        )
    } else {
        LibraryThemeStyle(
            background = Color(0xFFFBF8F9),
            headerBrandText = Color(0xFF8C7D82),
            pillBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFAF4F5))),
            pillBorder = Color(0x35E50914),
            pillShadow = Color(0x28E50914),
            pillText = Color(0xFF1D1416),
            cardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFBF5F6))),
            cardBorder = Brush.horizontalGradient(listOf(Color(0x55E50914), Color(0x25E50914), Color(0x55E50914))),
            cardShadow = Color(0x22E50914),
            tileBg = Color(0xFFFFFFFF),
            tileBorder = Color(0x25E50914),
            cardTitle = Color(0xFF1D1416),
            chevronTint = Color(0xFF9E8D93),
            sectionTitle = Color(0xFF8C7D82),
            playlistCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFBF5F6))),
            playlistBorder = Color(0x40E50914),
            playlistShadow = Color(0x22E50914),
            playlistSubtitle = Color(0xFF8C7D82),
            moreIconTint = Color(0xFF8C7D82)
        )
    }

    // If a playlist is selected for detail view:
    if (selectedPlaylist != null) {
        PlaylistDetailView(
            playlist = selectedPlaylist,
            allSongs = allSongs,
            currentPlayingSongId = currentPlayingSongId,
            isDarkTheme = isDarkTheme,
            onBack = { onSelectPlaylist(null) },
            onPlayAll = { onPlayPlaylist(selectedPlaylist) },
            onDelete = { onDeletePlaylist(selectedPlaylist.id) },
            onRemoveSong = { songId -> onRemoveSongFromPlaylist?.invoke(selectedPlaylist.id, songId) },
            onSongClick = { song -> onSongInPlaylistClick(selectedPlaylist, song) },
            onMenuClick = onOpenContextMenu,
            modifier = modifier
        )
        return
    }

    // Route to sub-screens if user clicked on one of the 4 action rows
    when (currentSubView) {
        LibrarySubView.LIKED_SONGS -> {
            val likedSongs = if (savedLikedSongs.isNotEmpty()) {
                savedLikedSongs.distinctBy { it.videoId ?: it.id }
            } else {
                allSongs.filter { likedSongIds.contains(it.id) || (it.videoId != null && likedSongIds.contains(it.videoId)) }
            }
            SubListScreen(
                title = "Liked Songs",
                countText = "${likedSongs.size} Songs",
                songs = likedSongs,
                currentPlayingSongId = currentPlayingSongId,
                isDarkTheme = isDarkTheme,
                onBack = { currentSubView = LibrarySubView.MAIN },
                onSongClick = onSongClick,
                onMenuClick = onOpenContextMenu,
                onPlayAll = { if (likedSongs.isNotEmpty()) onSongClick(likedSongs.first()) },
                emptyTitle = "No liked songs yet",
                emptySubtitle = "Tap the heart icon on any song to save it here",
                modifier = modifier
            )
            return
        }
        LibrarySubView.HISTORY -> {
            val historySongs = historySongIds.mapNotNull { id -> allSongs.find { it.id == id || it.videoId == id } }
            SubListScreen(
                title = "Play History",
                countText = "${historySongs.size} Tracks",
                songs = historySongs,
                currentPlayingSongId = currentPlayingSongId,
                isDarkTheme = isDarkTheme,
                onBack = { currentSubView = LibrarySubView.MAIN },
                onSongClick = onSongClick,
                onMenuClick = onOpenContextMenu,
                onPlayAll = { if (historySongs.isNotEmpty()) onSongClick(historySongs.first()) },
                onClearAll = onClearHistory,
                emptyTitle = "No playback history",
                emptySubtitle = "Songs you play will automatically appear here",
                modifier = modifier
            )
            return
        }
        LibrarySubView.LOCAL_MP3S -> {
            SubListScreen(
                title = "Local MP3 Files",
                countText = "${localSongs.size} Local Tracks",
                songs = localSongs,
                currentPlayingSongId = currentPlayingSongId,
                isDarkTheme = isDarkTheme,
                onBack = { currentSubView = LibrarySubView.MAIN },
                onSongClick = onSongClick,
                onMenuClick = onOpenContextMenu,
                onPlayAll = { if (localSongs.isNotEmpty()) onSongClick(localSongs.first()) },
                actionButtonText = "Import Files",
                onActionButtonClick = { filePickerLauncher.launch(arrayOf("audio/*", "video/*", "video/mp4", "audio/mp4")) },
                emptyTitle = "No local audio or MP4 videos imported",
                emptySubtitle = "Select audio or MP4 video files from device storage to play with zero cloud upload",
                modifier = modifier
            )
            return
        }
        LibrarySubView.MAIN -> {
            // Render the exact Library UI design from screenshots
            val scrollState = rememberScrollState()

            // Strictly user created or user imported playlists only
            val displayPlaylists = playlists

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(theme.background)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .padding(bottom = 110.dp)
            ) {
                // Top Header Brand: "KAIRO MUSIC"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "KAIRO MUSIC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = theme.headerBrandText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Center "Library" Pill Card
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(18.dp),
                                spotColor = theme.pillShadow,
                                ambientColor = theme.pillShadow
                            )
                            .border(1.5.dp, theme.pillBorder, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.pillBg)
                            .padding(horizontal = 48.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Library",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.pillText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 1. Liked Songs Row
                LibraryActionRow(
                    title = "Liked Songs",
                    theme = theme,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Songs",
                            tint = Color(0xFFFF1744),
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    onClick = { currentSubView = LibrarySubView.LIKED_SONGS },
                    testTag = "lib_liked_songs_card"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Play History Row
                LibraryActionRow(
                    title = "Play History",
                    theme = theme,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Play History",
                            tint = Color(0xFFFF2D55),
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    onClick = { currentSubView = LibrarySubView.HISTORY },
                    testTag = "lib_play_history_card"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Local MP3 Files Row
                LibraryActionRow(
                    title = "Local MP3 Files",
                    theme = theme,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Local MP3 Files",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    onClick = { currentSubView = LibrarySubView.LOCAL_MP3S },
                    testTag = "lib_local_mp3_card"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Import YT Playlist Row
                LibraryActionRow(
                    title = "Import YT Playlist",
                    theme = theme,
                    icon = {
                        // YouTube Play Badge
                        Box(
                            modifier = Modifier
                                .size(34.dp, 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF0000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "YouTube",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = onImportYouTubePlaylistClick,
                    testTag = "lib_import_yt_card"
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Section Title: "User Playlists"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Playlists",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.sectionTitle
                    )

                    IconButton(
                        onClick = onCreatePlaylistClick,
                        modifier = Modifier.size(32.dp).testTag("lib_create_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Playlist",
                            tint = KairoRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Playlists Items
                if (displayPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = theme.playlistShadow,
                                ambientColor = theme.playlistShadow
                            )
                            .border(
                                width = 1.5.dp,
                                color = theme.playlistBorder.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(theme.playlistCardBg)
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = theme.sectionTitle,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Playlists Yet",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.cardTitle
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a playlist using + or import from YouTube",
                                fontSize = 12.sp,
                                color = theme.playlistSubtitle
                            )
                        }
                    }
                } else {
                    displayPlaylists.forEach { pl ->
                        UserPlaylistCard(
                            playlist = pl,
                            theme = theme,
                            onClick = { onSelectPlaylist(pl) },
                            onDelete = { onDeletePlaylist(pl.id) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * Standard Action Row Component (Liked Songs, History, Local MP3, Import YT)
 * Perfectly matches glossy border, red neon glow, and tile in screenshot
 */
@Composable
private fun LibraryActionRow(
    title: String,
    theme: LibraryThemeStyle,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = theme.cardShadow,
                ambientColor = theme.cardShadow
            )
            .border(
                width = 1.5.dp,
                brush = theme.cardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(theme.cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Left Icon Tile
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.tileBg)
                        .border(1.dp, theme.tileBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Chevron Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = theme.chevronTint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * User Playlist Card matching screenshot
 */
@Composable
private fun UserPlaylistCard(
    playlist: Playlist,
    theme: LibraryThemeStyle,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = theme.playlistShadow,
                ambientColor = theme.playlistShadow
            )
            .border(
                width = 1.5.dp,
                color = theme.playlistBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(theme.playlistCardBg)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("user_playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Artwork with rounded corners and red border
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, KairoRed, RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E24))
                ) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Playlist Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = playlist.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.cardTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${playlist.songIds.size} songs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.playlistSubtitle
                    )
                }
            }

            // Three-dot Options Menu
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = theme.moreIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Playlist") },
                        onClick = {
                            isMenuOpen = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Sub-screen for Liked Songs, History, and Local MP3s with Back button
 */
@Composable
private fun SubListScreen(
    title: String,
    countText: String,
    songs: List<Song>,
    currentPlayingSongId: String?,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onMenuClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onClearAll: (() -> Unit)? = null,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
    emptyTitle: String,
    emptySubtitle: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkTheme) Color(0xFF070709) else Color(0xFFFBF8F9)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1D1416)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Sub-screen Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = countText,
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color(0xFF8E8E98) else Color(0xFF8C7D82)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (actionButtonText != null && onActionButtonClick != null) {
                    Button(
                        onClick = onActionButtonClick,
                        colors = ButtonDefaults.buttonColors(containerColor = KairoRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(actionButtonText, fontSize = 12.sp)
                    }
                }

                if (songs.isNotEmpty()) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = KairoRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", fontSize = 12.sp)
                    }
                }

                if (onClearAll != null && songs.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = emptyTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = emptySubtitle,
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color(0xFF8E8E98) else Color(0xFF8C7D82)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songs) { song ->
                    ListMusicCard(
                        song = song,
                        isPlayingThis = song.id == currentPlayingSongId,
                        onClick = { onSongClick(song) },
                        onMenuClick = { onMenuClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    allSongs: List<Song>,
    currentPlayingSongId: String?,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onDelete: () -> Unit,
    onRemoveSong: ((String) -> Unit)? = null,
    onSongClick: (Song) -> Unit,
    onMenuClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val songsInPlaylist = playlist.songIds.mapNotNull { id ->
        val cleanId = if (id.startsWith("yt_")) id.removePrefix("yt_") else id
        allSongs.find {
            it.id == id ||
            it.id == cleanId ||
            (it.videoId != null && (it.videoId == id || it.videoId == cleanId)) ||
            (it.id.startsWith("yt_") && it.id.removePrefix("yt_") == cleanId)
        }
    }
    val bgColor = if (isDarkTheme) Color(0xFF070709) else Color(0xFFFBF8F9)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1D1416)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Back Button & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Playlist Hero Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, KairoRed, RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A22))
            ) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${songsInPlaylist.size} tracks • Offline Available",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) Color(0xFF8E8E98) else Color(0xFF8C7D82)
                )

                if (songsInPlaylist.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = KairoRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("playlist_detail_play_all")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (songsInPlaylist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No songs in this playlist yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add songs to this playlist using the 3-dot menu on any song",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color(0xFF8E8E98) else Color(0xFF8C7D82)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songsInPlaylist) { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ListMusicCard(
                                song = song,
                                isPlayingThis = song.id == currentPlayingSongId,
                                onClick = { onSongClick(song) },
                                onMenuClick = { onMenuClick(song) }
                            )
                        }
                        if (onRemoveSong != null) {
                            IconButton(
                                onClick = { onRemoveSong(song.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove from Playlist",
                                    tint = if (isDarkTheme) Color(0xFF8E8E98) else Color(0xFF8C7D82),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
