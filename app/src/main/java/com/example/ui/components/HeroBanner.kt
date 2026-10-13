package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.theme.KairoDarkElevated
import com.example.ui.theme.KairoRed
import com.example.ui.theme.KairoRedGlow
import kotlinx.coroutines.delay

/**
 * High-Fidelity Trending Spotlight Hero Banner.
 * Features:
 * - Dynamic pulsating neon red aura and animated gradient border
 * - 5-second automatic carousel transition with horizontal swipe gestures
 * - Modern cinematic dark backdrop with glassmorphic depth
 * - Glowing 3D album cover frame with red light reflections
 * - Prominent "Play MP3" floating action pill and queue controls
 * - Interactive carousel dots indicator
 */
@Composable
fun HeroBanner(
    songs: List<Song>,
    onPlayClick: (Song) -> Unit,
    onQueueClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(songs.size, currentIndex) {
        delay(5000L)
        if (songs.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % songs.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_red_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val currentSong = songs[currentIndex.coerceIn(0, songs.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = KairoRed.copy(alpha = glowAlpha),
                spotColor = KairoRed.copy(alpha = glowAlpha)
            )
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        KairoRed.copy(alpha = glowAlpha),
                        Color(0xFFFF1744).copy(alpha = glowAlpha),
                        Color(0xFFFF5252).copy(alpha = glowAlpha * 0.8f),
                        KairoRedGlow.copy(alpha = glowAlpha),
                        Color(0xFFFF1744).copy(alpha = glowAlpha),
                        KairoRed.copy(alpha = glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F0B13))
            .pointerInput(songs.size) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag < -30f) {
                            currentIndex = (currentIndex + 1) % songs.size
                        } else if (totalDrag > 30f) {
                            currentIndex = if (currentIndex > 0) currentIndex - 1 else songs.lastIndex
                        }
                        totalDrag = 0f
                    }
                )
            }
            .clickable { onPlayClick(currentSong) }
            .testTag("hero_banner")
    ) {
        AnimatedContent(
            targetState = currentSong,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
            label = "spotlight_carousel"
        ) { song ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background artwork with high blur / darkness
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Cinematic Dark Radial & Horizontal Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFA07080C),
                                    Color(0xE808090E),
                                    Color(0xB008090E),
                                    Color(0x7008090E)
                                )
                            )
                        )
                )

                // Red ambient light flare in top-left
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    KairoRed.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left information column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge Pills
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(KairoRed, Color(0xFFFF1744))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (song.isNewRelease) "NEW RELEASE" else "TRENDING SPOTLIGHT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = song.genre,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Title
                        Text(
                            text = song.title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = (-0.3).sp
                        )

                        // Artist & Album
                        Text(
                            text = "${song.artist} • ${song.album}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC0C4CC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons & Nav
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onPlayClick(song) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KairoRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = KairoRed)
                                    .testTag("hero_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play MP3",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onQueueClick(song) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("hero_queue_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Queue",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Queue", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Left / Right Arrows
                            IconButton(
                                onClick = {
                                    currentIndex = if (currentIndex > 0) currentIndex - 1 else songs.lastIndex
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .testTag("spotlight_prev_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Spotlight",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    currentIndex = (currentIndex + 1) % songs.size
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .testTag("spotlight_next_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Spotlight",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Right 3D Artwork thumbnail with glowing red border
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .aspectRatio(1f)
                            .shadow(18.dp, RoundedCornerShape(16.dp), spotColor = KairoRedGlow)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, KairoRed, RoundedCornerShape(16.dp))
                            .background(KairoDarkElevated)
                    ) {
                        SongThumbnail(
                            artworkUrl = song.artworkUrl,
                            title = song.title,
                            shape = RoundedCornerShape(16.dp),
                            iconSize = 34.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom Carousel Indicators (Dots)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(minOf(songs.size, 8)) { idx ->
                        val isSelected = idx == currentIndex % minOf(songs.size, 8)
                        Box(
                            modifier = Modifier
                                .height(3.5.dp)
                                .width(if (isSelected) 16.dp else 5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSelected) KairoRed else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }
    }
}
