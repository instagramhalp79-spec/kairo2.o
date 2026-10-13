package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SampleData
import com.example.model.Song
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ContextMenuModal
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DrawerContent
import com.example.ui.components.FollowInstagramDialog
import com.example.ui.components.FullScreenPlayer
import com.example.ui.components.ImportPlaylistDialog
import com.example.ui.components.ImportYouTubePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NotificationSettingsDialog
import com.example.ui.components.ShareModal
import com.example.ui.components.TopBar
import com.example.ui.screens.EmptyPlayingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.KairoMusicTheme
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.NavigationTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var viewModelRef: MusicViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.player.KairoPlaybackService.start(this)
        handlePlaybackIntent(intent)
        setContent {
            val viewModel: MusicViewModel = viewModel()
            viewModelRef = viewModel
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            var isSplashFinished by rememberSaveable { mutableStateOf(false) }

            KairoMusicTheme(darkTheme = isDarkTheme) {
                if (!isSplashFinished) {
                    com.example.ui.components.SplashScreen(
                        onSplashFinished = { isSplashFinished = true }
                    )
                } else {
                    KairoMusicApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePlaybackIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelRef?.onAppClosed()
        com.example.player.KairoPlaybackService.stop(this)
    }

    private fun handlePlaybackIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_PLAY_PAUSE" -> viewModelRef?.player?.togglePlayPause()
            "ACTION_NEXT" -> viewModelRef?.player?.playNext()
            "ACTION_PREV" -> viewModelRef?.player?.playPrevious()
            "ACTION_STOP" -> viewModelRef?.player?.stopPlayback()
        }
    }
}

@Composable
fun KairoMusicApp(viewModel: MusicViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val savedLikedSongs by viewModel.savedLikedSongs.collectAsState()
    val historySongIds by viewModel.historySongIds.collectAsState()
    val savedHistorySongs by viewModel.savedHistorySongs.collectAsState()
    val followedArtistIds by viewModel.followedArtistIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    val isFullScreenPlayerOpen by viewModel.isFullScreenPlayerOpen.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val spotlightSongs by viewModel.spotlightSongs.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val youtubeSearchResults by viewModel.youtubeSearchResults.collectAsState()
    val isSearchingYouTube by viewModel.isSearchingYouTube.collectAsState()
    val searchErrorMessage by viewModel.searchErrorMessage.collectAsState()

    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    val contextMenuSong by viewModel.contextMenuSong.collectAsState()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsState()
    val isCreatePlaylistDialogOpen by viewModel.isCreatePlaylistDialogOpen.collectAsState()
    val isShareDialogOpen by viewModel.isShareDialogOpen.collectAsState()
    val shareSong by viewModel.shareSong.collectAsState()
    val isImportPlaylistDialogOpen by viewModel.isImportPlaylistDialogOpen.collectAsState()
    val isImportYouTubeDialogOpen by viewModel.isImportYouTubeDialogOpen.collectAsState()
    val isImportingYouTube by viewModel.isImportingYouTube.collectAsState()
    val importYouTubeErrorMessage by viewModel.importYouTubeErrorMessage.collectAsState()
    val isNotificationDialogOpen by viewModel.isNotificationDialogOpen.collectAsState()
    val isFollowInstagramDialogOpen by viewModel.isFollowInstagramDialogOpen.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    val context = LocalContext.current
    val relatedSongs by viewModel.relatedSongs.collectAsState()
    val isLoadingRelated by viewModel.isLoadingRelated.collectAsState()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsState()
    var currentVolume by remember { mutableFloatStateOf(1f) }

    // Request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.toggleNotifications()
            }
        }
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Show error snackbar if playback errors occur
    LaunchedEffect(playerState.errorMessage) {
        playerState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Comprehensive BackHandler to prevent accidental app closes / automatic back exits
    androidx.activity.compose.BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (isFullScreenPlayerOpen) {
            viewModel.closeFullScreenPlayer()
            if (currentTab == NavigationTab.PLAYING) {
                viewModel.setTab(NavigationTab.HOME)
            }
        } else if (selectedPlaylist != null) {
            viewModel.selectPlaylist(null)
        } else if (currentTab != NavigationTab.HOME) {
            viewModel.setTab(NavigationTab.HOME)
        } else {
            // Keep music playing smoothly in background instead of killing process
            (context as? ComponentActivity)?.moveTaskToBack(true)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { viewModel.toggleTheme() },
                onOnlineStreamsClick = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setTab(NavigationTab.HOME)
                },
                onFollowInstagramClick = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setFollowInstagramDialogOpen(true)
                },
                onNotificationsClick = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setNotificationDialogOpen(true)
                },
                onHistoryClick = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setTab(NavigationTab.LIBRARY)
                },
                onFollowedArtistsClick = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setTab(NavigationTab.HOME)
                },
                onImportYouTubePlaylistClick = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setImportYouTubeDialogOpen(true)
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopBar(
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    isDarkTheme = isDarkTheme
                )
            },
            bottomBar = {
                Column {
                    // Mini Player above navigation bar when a track is loaded
                    AnimatedVisibility(
                        visible = playerState.currentSong != null && !isFullScreenPlayerOpen && currentTab != NavigationTab.PLAYING,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        playerState.currentSong?.let { activeSong ->
                            MiniPlayer(
                                playerState = playerState,
                                isLiked = viewModel.isSongLiked(activeSong.id),
                                onPlayPauseClick = { viewModel.player.togglePlayPause() },
                                onNextClick = { viewModel.player.playNext() },
                                onLikeClick = { viewModel.toggleLike(activeSong.id) },
                                onClick = { viewModel.openFullScreenPlayer() }
                            )
                        }
                    }

                    // Bottom Navigation Bar
                    BottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            if (tab == NavigationTab.PLAYING) {
                                val lastPlayed = viewModel.getLastPlayedSong()
                                if (playerState.currentSong == null && lastPlayed != null) {
                                    viewModel.restoreLastPlayedSong(lastPlayed)
                                }
                                viewModel.setTab(NavigationTab.PLAYING)
                                if (playerState.currentSong != null || lastPlayed != null) {
                                    viewModel.openFullScreenPlayer()
                                } else {
                                    // First time opening app: NO song has ever been played
                                    viewModel.closeFullScreenPlayer()
                                }
                            } else {
                                viewModel.closeFullScreenPlayer()
                                viewModel.setTab(tab)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            allSongs = allSongs,
                            playedSongs = savedHistorySongs,
                            spotlightSongs = spotlightSongs,
                            currentPlayingSongId = playerState.currentSong?.id,
                            artists = SampleData.artists,
                            followedArtistIds = followedArtistIds,
                            onSongClick = { viewModel.playSong(it) },
                            onQueueSong = { viewModel.addToUserQueue(it) },
                            onOpenContextMenu = { viewModel.openContextMenu(it) },
                            onFollowArtistToggle = { viewModel.toggleFollowArtist(it) },
                            onArtistClick = { artist ->
                                viewModel.setSearchQuery(artist.name)
                                viewModel.setTab(NavigationTab.SEARCH)
                            }
                        )
                    }
                    NavigationTab.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearchSubmit = { viewModel.executeImmediateSearch(it) },
                            selectedFilter = selectedFilter,
                            onFilterChange = { viewModel.setSelectedFilter(it) },
                            youtubeResults = youtubeSearchResults,
                            localSongs = localSongs,
                            isSearchingYouTube = isSearchingYouTube,
                            searchErrorMessage = searchErrorMessage,
                            currentPlayingSongId = playerState.currentSong?.id,
                            onSongClick = { viewModel.playSong(it) },
                            onOpenContextMenu = { viewModel.openContextMenu(it) },
                            onImportYouTubeUrl = { url ->
                                viewModel.importYouTubePlaylist(url, playImmediately = true)
                            }
                        )
                    }
                    NavigationTab.LIBRARY -> {
                        LibraryScreen(
                            allSongs = allSongs,
                            likedSongIds = likedSongIds,
                            savedLikedSongs = savedLikedSongs,
                            historySongIds = historySongIds,
                            localSongs = localSongs,
                            playlists = playlists,
                            selectedPlaylist = selectedPlaylist,
                            currentPlayingSongId = playerState.currentSong?.id,
                            isDarkTheme = isDarkTheme,
                            onSelectPlaylist = { viewModel.selectPlaylist(it) },
                            onSongClick = { viewModel.playSong(it) },
                            onPlayPlaylist = { viewModel.playPlaylist(it) },
                            onSongInPlaylistClick = { playlist, song -> viewModel.playPlaylist(playlist, song) },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onOpenContextMenu = { viewModel.openContextMenu(it) },
                            onClearHistory = { viewModel.clearHistory() },
                            onCreatePlaylistClick = { viewModel.setCreatePlaylistDialogOpen(true) },
                            onImportPlaylistInfoClick = { viewModel.setImportPlaylistDialogOpen(true) },
                            onImportYouTubePlaylistClick = { viewModel.setImportYouTubeDialogOpen(true) },
                            onImportLocalFiles = { viewModel.importAudioFiles(it) },
                            onRemoveSongFromPlaylist = { playlistId, songId ->
                                viewModel.removeSongFromPlaylist(playlistId, songId)
                            }
                        )
                    }
                    NavigationTab.PLAYING -> {
                        if (playerState.currentSong == null) {
                            EmptyPlayingScreen(
                                spotlightSongs = spotlightSongs.take(6),
                                onSongClick = { viewModel.playSong(it) },
                                onExploreClick = { viewModel.setTab(NavigationTab.HOME) }
                            )
                        }
                    }
                }



                // Full-Screen Player Overlay
                AnimatedVisibility(
                    visible = (isFullScreenPlayerOpen || currentTab == NavigationTab.PLAYING) && playerState.currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    playerState.currentSong?.let { currentSong ->
                        FullScreenPlayer(
                            playerState = playerState,
                            isLiked = viewModel.isSongLiked(currentSong.id),
                            lyricsData = currentLyrics,
                            isLoadingLyrics = isLoadingLyrics,
                            onClose = {
                                viewModel.closeFullScreenPlayer()
                                if (currentTab == NavigationTab.PLAYING) {
                                    viewModel.setTab(NavigationTab.HOME)
                                }
                            },
                            onPlayPause = { viewModel.player.togglePlayPause() },
                            onNext = { viewModel.player.playNext() },
                            onPrevious = { viewModel.player.playPrevious() },
                            onSeek = { viewModel.player.seekTo(it) },
                            onToggleShuffle = { viewModel.player.toggleShuffle() },
                            onCycleRepeat = { viewModel.player.cycleRepeatMode() },
                            onToggleLike = { viewModel.toggleLike(currentSong.id) },
                            onOpenContextMenu = { viewModel.openContextMenu(it) },
                            onShare = { viewModel.openShareDialog(it) },
                            onSelectQueueItem = { viewModel.playSong(it, playerState.queue) },
                            onRemoveFromQueue = { index ->
                                playerState.queue.getOrNull(index)?.let { viewModel.removeTrackFromQueue(it) }
                            },
                            ytBridge = viewModel.player.ytBridge,
                            onAttachVideoSurface = { viewModel.player.attachVideoSurface(it) },
                            onDetachVideoSurface = { viewModel.player.detachVideoSurface() },
                            relatedSongs = relatedSongs,
                            isLoadingRelated = isLoadingRelated,
                            isAutoplayEnabled = isAutoplayEnabled,
                            onToggleAutoplay = { viewModel.toggleAutoplay() },
                            onPlayRelatedSong = { viewModel.playSong(it) },
                            onQueueSong = { viewModel.addToUserQueue(it) },
                            volume = currentVolume,
                            onVolumeChange = {
                                currentVolume = it
                                viewModel.setVolume(it)
                            }
                        )
                    }
                }
            }
        }
    }

    // Context Menu Bottom Sheet
    ContextMenuModal(
        song = contextMenuSong,
        isLiked = contextMenuSong?.let { viewModel.isSongLiked(it.id) } ?: false,
        onDismiss = { viewModel.closeContextMenu() },
        onPlayNext = { viewModel.playNextSong(it) },
        onAddToQueue = { viewModel.addToUserQueue(it) },
        onAddToPlaylist = { viewModel.openAddToPlaylistSheet(it) },
        onToggleLike = { viewModel.toggleLike(it.id) },
        onShare = { viewModel.openShareDialog(it) }
    )

    // Add to Playlist Sheet
    AddToPlaylistSheet(
        song = songToAddToPlaylist,
        playlists = playlists,
        onDismiss = { viewModel.closeAddToPlaylistSheet() },
        onAddToPlaylist = { pId, sId -> viewModel.addSongToPlaylist(pId, sId) },
        onCreateNewPlaylistClick = {
            // Keep songToAddToPlaylist so the new playlist is automatically created with this song & its thumbnail
            viewModel.setCreatePlaylistDialogOpen(true)
        }
    )

    // Create Playlist Dialog
    CreatePlaylistDialog(
        isOpen = isCreatePlaylistDialogOpen,
        song = songToAddToPlaylist,
        onDismiss = {
            viewModel.setCreatePlaylistDialogOpen(false)
            viewModel.closeAddToPlaylistSheet()
        },
        onCreate = { name, desc ->
            viewModel.createPlaylist(name, desc, songToAddToPlaylist)
        }
    )

    // Share Modal
    ShareModal(
        isOpen = isShareDialogOpen,
        song = shareSong,
        onDismiss = { viewModel.closeShareDialog() }
    )

    // Import Info Dialog
    ImportPlaylistDialog(
        isOpen = isImportPlaylistDialogOpen,
        onDismiss = { viewModel.setImportPlaylistDialogOpen(false) }
    )

    // Notification Dialog
    NotificationSettingsDialog(
        isOpen = isNotificationDialogOpen,
        notificationsEnabled = notificationsEnabled,
        onToggle = { viewModel.toggleNotifications() },
        onDismiss = { viewModel.setNotificationDialogOpen(false) }
    )

    // Follow Instagram Dialog
    FollowInstagramDialog(
        isOpen = isFollowInstagramDialogOpen,
        onDismiss = { viewModel.setFollowInstagramDialogOpen(false) }
    )

    // YouTube Playlist Import Dialog (Zero API Keys)
    ImportYouTubePlaylistDialog(
        isOpen = isImportYouTubeDialogOpen,
        isImporting = isImportingYouTube,
        errorMessage = importYouTubeErrorMessage,
        onDismiss = { viewModel.setImportYouTubeDialogOpen(false) },
        onImport = { url, playImmediately ->
            viewModel.importYouTubePlaylist(url, playImmediately)
        }
    )
}
