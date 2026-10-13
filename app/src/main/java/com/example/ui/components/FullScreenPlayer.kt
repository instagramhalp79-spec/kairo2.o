package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import com.example.model.LyricsData
import com.example.model.PlaybackMode
import com.example.model.PlayerState
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.player.YouTubePlayerBridge
import com.example.ui.theme.KairoDarkBg
import com.example.ui.theme.KairoDarkElevated
import com.example.ui.theme.KairoRed
import com.example.ui.theme.KairoRedGlow
import java.util.Locale

@Composable
fun FullScreenPlayer(
    playerState: PlayerState,
    isLiked: Boolean,
    lyricsData: LyricsData,
    isLoadingLyrics: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenContextMenu: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onSelectQueueItem: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    ytBridge: YouTubePlayerBridge? = null,
    onAttachVideoSurface: (android.view.Surface) -> Unit = {},
    onDetachVideoSurface: () -> Unit = {},
    relatedSongs: List<Song> = emptyList(),
    isLoadingRelated: Boolean = false,
    isAutoplayEnabled: Boolean = true,
    onToggleAutoplay: () -> Unit = {},
    onPlayRelatedSong: (Song) -> Unit = {},
    onQueueSong: (Song) -> Unit = {},
    volume: Float = 1.0f,
    onVolumeChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onClose()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            onDetachVideoSurface()
        }
    }

    val song = playerState.currentSong ?: return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderPosition by remember { mutableFloatStateOf(0f) }
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: Player, 1: Queue, 2: Related, 3: Lyrics

    // Default to Video mode when playing an MP4 video or YouTube track
    val isMp4OrVideoTrack = song.hasVideo
    var isVideoMode by remember(song.id) { mutableStateOf(isMp4OrVideoTrack) }

    val currentMs = if (isDraggingSlider) dragSliderPosition.toLong() else playerState.currentPositionMs
    val durationMs = playerState.durationMs.coerceAtLeast(1L)

    // Lyrics active index calculation in real-time based on audio position
    val activeLyricIndex = lyricsData.getActiveLineIndex(currentMs)
    val lyricsListState = rememberLazyListState()

    // Smoothly scroll to active lyric line
    LaunchedEffect(activeLyricIndex) {
        if (activeLyricIndex >= 0 && selectedBottomTab == 3) {
            val targetScroll = (activeLyricIndex - 2).coerceAtLeast(0)
            lyricsListState.animateScrollToItem(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KairoDarkBg)
    ) {
        // Ambient blurred backdrop
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp)
        )

        // Dark gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC07080B),
                            Color(0xF007080B),
                            Color(0xFF07080B)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("player_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isMp4OrVideoTrack) "PLAYING MP4 VIDEO" else "PLAYING FROM PLAYLIST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = song.album.ifBlank { "KAIRO Stream" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { onOpenContextMenu(song) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("player_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab bar: Player / Queue / Related / Lyrics
            TabRow(
                selectedTabIndex = selectedBottomTab,
                containerColor = Color.Transparent,
                contentColor = KairoRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedBottomTab]),
                        color = KairoRed,
                        height = 2.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    text = {
                        Text(
                            "PLAYER",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomTab == 0) Color.White else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    text = {
                        Text(
                            "QUEUE (${playerState.queue.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomTab == 1) Color.White else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    text = {
                        Text(
                            "RELATED",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomTab == 2) Color.White else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedBottomTab == 3,
                    onClick = { selectedBottomTab = 3 },
                    text = {
                        Text(
                            "LYRICS",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBottomTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBottomTab == 3) Color.White else Color.Gray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedBottomTab) {
                0 -> {
                    // MAIN PLAYER VIEW
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Song / Video (MP4) Mode Switch (YouTube Music style)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x28FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!isVideoMode) KairoRed else Color.Transparent)
                                    .clickable { isVideoMode = false }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Song",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isVideoMode) KairoRed else Color.Transparent)
                                    .clickable { isVideoMode = true }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Video (MP4)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isVideoMode) {
                            // MP4 Video Container with widescreen 16:9 ratio and red glow
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .aspectRatio(16f / 9f)
                                    .shadow(28.dp, shape = RoundedCornerShape(20.dp), spotColor = KairoRedGlow)
                                    .border(
                                        width = 1.dp,
                                        color = KairoRed.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ytBridge != null && (song.videoId != null || song.source == "YouTube")) {
                                    AndroidView(
                                        factory = { ctx ->
                                            ytBridge.getOrCreateWebView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (song.hasVideo) {
                                    // Live Native MP4 Video Player using TextureView attached to MediaPlayer
                                    AndroidView(
                                        factory = { ctx ->
                                            android.view.TextureView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                                    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                        try {
                                                            val surface = android.view.Surface(surfaceTexture)
                                                            onAttachVideoSurface(surface)
                                                        } catch (e: Exception) {
                                                            android.util.Log.w("FullScreenPlayer", "Surface attach notice: ${e.message}")
                                                        }
                                                    }

                                                    override fun onSurfaceTextureSizeChanged(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {}

                                                    override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
                                                        try {
                                                            onDetachVideoSurface()
                                                        } catch (_: Exception) {}
                                                        return true
                                                    }

                                                    override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) {}
                                                }

                                                // If already available upon factory instantiation, attach immediately
                                                if (isAvailable && surfaceTexture != null) {
                                                    try {
                                                        val surface = android.view.Surface(surfaceTexture)
                                                        onAttachVideoSurface(surface)
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = song.artworkUrl,
                                        contentDescription = "MP4 Video",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            // Album Artwork with rich red glow and MP3 Audio indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .aspectRatio(1f)
                                    .shadow(28.dp, shape = RoundedCornerShape(22.dp), spotColor = KairoRedGlow)
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(KairoDarkElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = song.artworkUrl,
                                    contentDescription = "Album Artwork",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Clean High-Quality MP3 Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .border(0.5.dp, KairoRed, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(KairoRed)
                                        )
                                        Text(
                                            text = "MP3 AUDIO",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Title, Artist, Like & Share
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFFB8BBC3),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onShare(song) },
                                    modifier = Modifier.testTag("player_share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onToggleLike,
                                    modifier = Modifier.testTag("player_like_button")
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isLiked) KairoRed else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Progress Slider & Timestamps
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = currentMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                                onValueChange = {
                                    isDraggingSlider = true
                                    dragSliderPosition = it
                                },
                                onValueChangeFinished = {
                                    onSeek(dragSliderPosition.toLong())
                                    isDraggingSlider = false
                                },
                                valueRange = 0f..durationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = KairoRed,
                                    activeTrackColor = KairoRed,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_seek_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTimeMs(currentMs),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = formatTimeMs(durationMs),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Controls: Shuffle, Prev, Play/Pause, Next, Repeat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.testTag("player_shuffle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (playerState.shuffleMode) KairoRed else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Previous
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("player_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Track",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Big Play/Pause
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(16.dp, shape = CircleShape, spotColor = KairoRed)
                                    .clip(CircleShape)
                                    .background(KairoRed),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = onPlayPause,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("player_play_pause_button")
                                ) {
                                    if (playerState.isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(26.dp),
                                            color = Color.White,
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }

                            // Next
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("player_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Track",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Repeat
                            IconButton(
                                onClick = onCycleRepeat,
                                modifier = Modifier.testTag("player_repeat_button")
                            ) {
                                val (icon, tint) = when (playerState.repeatMode) {
                                    RepeatMode.OFF -> Pair(Icons.Default.Repeat, Color.Gray)
                                    RepeatMode.ALL -> Pair(Icons.Default.Repeat, KairoRed)
                                    RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, KairoRed)
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Repeat Mode",
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Volume Control Slider (Liquid Glass style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (volume > 0f) onVolumeChange(0f) else onVolumeChange(1f)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                val volIcon = when {
                                    volume <= 0.05f -> Icons.Default.VolumeMute
                                    volume < 0.5f -> Icons.Default.VolumeDown
                                    else -> Icons.Default.VolumeUp
                                }
                                Icon(
                                    imageVector = volIcon,
                                    contentDescription = "Volume",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Slider(
                                value = volume,
                                onValueChange = { onVolumeChange(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = KairoRed,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_volume_slider")
                            )

                            Text(
                                text = "${(volume * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.width(36.dp)
                            )
                        }

                        // Bottom Actions matching Notification & Expanded UI: Add to Playlist, Save/Like, More
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Add to Playlist
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenContextMenu(song) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Add to Playlist",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Add to Playlist",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Save / Like
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleLike() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Save Song",
                                    tint = if (isLiked) KairoRed else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isLiked) "Saved" else "Save",
                                    fontSize = 11.sp,
                                    color = if (isLiked) KairoRed else Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Synced Lyrics (Levyra-style)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedBottomTab = 3 } // switches to Lyrics view
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Synced Lyrics",
                                    tint = if (selectedBottomTab == 3) KairoRed else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Lyrics",
                                    fontSize = 11.sp,
                                    color = if (selectedBottomTab == 3) KairoRed else Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // More (Open Queue or Context Menu)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedBottomTab = 1 } // switches to Queue view
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Queue & More",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "More",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // QUEUE VIEW
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (playerState.playbackMode == PlaybackMode.PLAYLIST) "Playlist Queue" else "Upcoming Queue",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (playerState.playbackMode == PlaybackMode.PLAYLIST) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = KairoRed.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, KairoRed.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "Playlist Order",
                                        color = KairoRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Smart Autoplay",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(playerState.queue) { index, queueSong ->
                                val isCurrent = index == playerState.currentIndex
                                val isManualPlayNext = playerState.manualPlayNextQueue.any {
                                    it.id == queueSong.id || (it.videoId != null && it.videoId == queueSong.videoId)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) KairoDarkElevated else Color(0x14FFFFFF))
                                        .clickable { onSelectQueueItem(queueSong) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            color = if (isCurrent) KairoRed else Color.Gray,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        AsyncImage(
                                            model = queueSong.artworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = queueSong.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) KairoRed else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isManualPlayNext) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = KairoRed.copy(alpha = 0.25f)
                                                    ) {
                                                        Text(
                                                            text = "PLAY NEXT",
                                                            color = KairoRed,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = queueSong.artist,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRemoveFromQueue(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove from Queue",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        InstagramFollowButton(modifier = Modifier.fillMaxWidth())
                    }
                }
                2 -> {
                    // RELATED SONGS & SPOTIFY / YT MUSIC AUTOPLAY
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Autoplay Toggle Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x22FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = KairoRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Autoplay Similar Songs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Continue playback with similar songs when queue ends",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isAutoplayEnabled,
                                onCheckedChange = { onToggleAutoplay() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KairoRed
                                ),
                                modifier = Modifier.testTag("autoplay_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Related Songs for '${song.artist}'",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val cleanRelatedSongs = remember(relatedSongs, song) {
                            val curNorm = com.example.core.youtube.YouTubeMediaResolver.getNormalizedBaseTitle(song.title)
                            val curRaw = song.title.trim().lowercase()
                            val seen = mutableSetOf<String>(curNorm, curRaw)
                            val seenIds = mutableSetOf<String>(song.id)
                            song.videoId?.let { seenIds.add(it) }

                            relatedSongs.filter { s ->
                                val sVid = s.videoId ?: s.id
                                val sNorm = com.example.core.youtube.YouTubeMediaResolver.getNormalizedBaseTitle(s.title)
                                val sRaw = s.title.trim().lowercase()
                                if (seenIds.contains(s.id) || seenIds.contains(sVid) || seen.contains(sNorm) || seen.contains(sRaw)) {
                                    false
                                } else {
                                    seenIds.add(s.id)
                                    seenIds.add(sVid)
                                    seen.add(sNorm)
                                    seen.add(sRaw)
                                    true
                                }
                            }
                        }

                        if (isLoadingRelated) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = KairoRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Finding recommendations...", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        } else if (cleanRelatedSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No related songs found.", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(cleanRelatedSongs) { _, relSong ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x14FFFFFF))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable { onPlayRelatedSong(relSong) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            AsyncImage(
                                                model = relSong.artworkUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = relSong.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${relSong.artist} • ${formatTimeMs(relSong.durationMs)}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onQueueSong(relSong) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlaylistAdd,
                                                    contentDescription = "Add to Queue",
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onPlayRelatedSong(relSong) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = KairoRed,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        InstagramFollowButton(modifier = Modifier.fillMaxWidth())
                    }
                }
                3 -> {
                    // SYNCHRONIZED LYRICS VIEW (Levyra LRCLIB Engine)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Levyra Synced Lyrics Header Status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1FFFFFFF))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (lyricsData.isSynced) Color(0xFF00E676) else Color(0xFFFFB300))
                                )
                                Text(
                                    text = if (lyricsData.isSynced) "Synced Lyrics (LRCLIB • Levyra)" else "Standard Lyrics",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Tap line to seek audio",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingLyrics) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = KairoRed)
                            }
                        } else if (lyricsData.isSynced && lyricsData.lines.isNotEmpty()) {
                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(lyricsData.lines) { index, line ->
                                    val isActive = index == activeLyricIndex
                                    val textColor by animateColorAsState(
                                        targetValue = if (isActive) Color.White else Color(0x66FFFFFF),
                                        label = "lyric_color"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) Color(0x1AE50914) else Color.Transparent)
                                            .clickable {
                                                onSeek(line.timestampMs)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = line.text,
                                            fontSize = if (isActive) 18.sp else 15.sp,
                                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isActive) KairoRed else textColor,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 24.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Plain lyrics fallback
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = lyricsData.plainText.ifBlank { song.lyrics ?: "Lyrics not available for this track." },
                                    fontSize = 15.sp,
                                    lineHeight = 26.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xDDFFFFFF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
