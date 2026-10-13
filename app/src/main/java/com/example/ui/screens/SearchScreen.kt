package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.components.ListMusicCard
import com.example.ui.theme.KairoDarkElevated
import com.example.ui.theme.KairoRed
import com.example.ui.theme.KairoRedGlow

data class SearchFolderItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val gradient: Brush,
    val searchQuery: String
)

@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    youtubeResults: List<Song>,
    localSongs: List<Song>,
    isSearchingYouTube: Boolean,
    searchErrorMessage: String?,
    currentPlayingSongId: String?,
    onSongClick: (Song) -> Unit,
    onOpenContextMenu: (Song) -> Unit,
    onImportYouTubeUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val filterOptions = listOf("All", "Online Songs", "YouTube", "Local MP3s")

    val isYoutubeUrl = searchQuery.contains("youtube.com", ignoreCase = true) ||
        searchQuery.contains("youtu.be", ignoreCase = true) ||
        searchQuery.startsWith("PL", ignoreCase = true) ||
        searchQuery.contains("list=", ignoreCase = true)

    val displayList: List<Song> = when (selectedFilter) {
        "Local MP3s" -> {
            if (searchQuery.isBlank()) localSongs else localSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
        "Online Songs" -> {
            youtubeResults.filter { it.source == "Online" }
        }
        "YouTube" -> {
            youtubeResults.filter { it.source == "YouTube" }
        }
        else -> {
            val localMatched = if (searchQuery.isBlank()) emptyList() else localSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
            (youtubeResults + localMatched).distinctBy { it.videoId ?: it.id }
        }
    }

    // Curated Search Folders & Genres (Requirement: "searching folder ko bi improve kr do")
    val musicFolders = listOf(
        SearchFolderItem(
            title = "Bollywood Hits",
            subtitle = "Arijit, Pritam, Bad Newz",
            icon = "🎬",
            gradient = Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFF620308))),
            searchQuery = "Bollywood Hits"
        ),
        SearchFolderItem(
            title = "Punjabi Beats",
            subtitle = "Karan Aujla, Diljit, Honey",
            icon = "🌾",
            gradient = Brush.linearGradient(listOf(Color(0xFFFF8F00), Color(0xFFBF360C))),
            searchQuery = "Punjabi Hits"
        ),
        SearchFolderItem(
            title = "Haryanvi Ragni",
            subtitle = "Masoom, Gulzaar, Renuka",
            icon = "🚜",
            gradient = Brush.linearGradient(listOf(Color(0xFFD50000), Color(0xFF4A0000))),
            searchQuery = "Haryanvi Songs"
        ),
        SearchFolderItem(
            title = "Remix & DJ Mix",
            subtitle = "Club, Bass Boost, Dance",
            icon = "🎛️",
            gradient = Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF311B92))),
            searchQuery = "Remix"
        ),
        SearchFolderItem(
            title = "Romantic & Lofi",
            subtitle = "Love songs, Chill & Flip",
            icon = "💖",
            gradient = Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF880E4F))),
            searchQuery = "Romantic Songs"
        ),
        SearchFolderItem(
            title = "Party & Gym Bass",
            subtitle = "High Energy & Workout",
            icon = "⚡",
            gradient = Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF004D40))),
            searchQuery = "Party Songs"
        ),
        SearchFolderItem(
            title = "Hip Hop & Rap",
            subtitle = "DIVINE, MC Stan, Seedhe Maut",
            icon = "🎤",
            gradient = Brush.linearGradient(listOf(Color(0xFF00B0FF), Color(0xFF01579B))),
            searchQuery = "Hip Hop Hindi"
        ),
        SearchFolderItem(
            title = "Devotional & Bhakti",
            subtitle = "Aarti, Bhajan, Shabad",
            icon = "🕉️",
            gradient = Brush.linearGradient(listOf(Color(0xFFFF6D00), Color(0xFFE65100))),
            searchQuery = "Bhakti Songs"
        )
    )

    val trendingKeywords = listOf(
        "Tauba Tauba", "Millionaire", "Kesariya Remix", "Softly Karan Aujla",
        "Big Dawgs", "Systumm", "Chatak Matak", "O Sajni Re"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search songs, artists, albums, or paste YouTube link...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.5.sp
                )
            },
            leadingIcon = {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        onSearchSubmit(searchQuery)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag("search_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearchSubmit(searchQuery)
                }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = KairoRed,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                cursorColor = KairoRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_input")
        )

        // YouTube URL detected banner
        if (isYoutubeUrl) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KairoRed.copy(alpha = 0.15f))
                    .border(1.dp, KairoRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onImportYouTubeUrl(searchQuery) }
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = KairoRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "YouTube Link Detected!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to import full playlist & play immediately",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { onImportYouTubeUrl(searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = KairoRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Import", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) KairoRed else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) KairoRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onFilterChange(filter) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("filter_$filter")
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // HIGH-FIDELITY LOADING UI DESIGN (Requirement: "hamare app ka loding ui desion bi improbe kr do")
        if (isSearchingYouTube) {
            val infiniteTransition = rememberInfiniteTransition(label = "search_loading_pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 30.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pulsing Neon Equalizer Ring
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(KairoRed.copy(alpha = 0.15f))
                            .border(2.dp, KairoRed.copy(alpha = pulseAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Searching",
                            tint = KairoRed.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Searching High-Fidelity Streams...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Matching exact MP3 audio tracks via Levyra engine",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Shimmer skeleton preview cards
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(3) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KairoDarkElevated.copy(alpha = pulseAlpha * 0.7f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KairoRed.copy(alpha = 0.2f))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.4f)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Error State
        else if (searchErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = searchErrorMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onSearchSubmit(searchQuery) }
                    ) {
                        Text("Retry", color = KairoRed)
                    }
                }
            }
        }
        // Empty Search Query: SEARCHING FOLDER UI (Requirement: "searching folder ko bi improve kr do")
        else if (searchQuery.isBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trending Search Chips
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = KairoRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Trending Searches",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(trendingKeywords) { keyword ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(0.75.dp, KairoRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            onQueryChange(keyword)
                                            onSearchSubmit(keyword)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = keyword,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Explore Music Folders & Genres Section
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = KairoRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Music Folders & Categories",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // 2-Column Grid of Music Folders (Requirement: "searching folder ko bi improve kr do")
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (i in musicFolders.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val item1 = musicFolders[i]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(88.dp)
                                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(item1.gradient)
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            onQueryChange(item1.searchQuery)
                                            onSearchSubmit(item1.searchQuery)
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${item1.icon} ${item1.title}",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.25f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("MP3", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                        Text(
                                            text = item1.subtitle,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (i + 1 < musicFolders.size) {
                                    val item2 = musicFolders[i + 1]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(88.dp)
                                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(item2.gradient)
                                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                            .clickable {
                                                onQueryChange(item2.searchQuery)
                                                onSearchSubmit(item2.searchQuery)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${item2.icon} ${item2.title}",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.25f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text("MP3", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                            Text(
                                                text = item2.subtitle,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        // Empty Search Results
        else if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No songs found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Try searching with title or artist name",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Result List
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayList) { song ->
                    ListMusicCard(
                        song = song,
                        isPlayingThis = song.id == currentPlayingSongId,
                        onClick = { onSongClick(song) },
                        onMenuClick = { onOpenContextMenu(song) }
                    )
                }
            }
        }
    }
}
