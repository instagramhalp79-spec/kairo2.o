package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KairoRed

@Composable
fun DrawerContent(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOnlineStreamsClick: () -> Unit,
    onFollowInstagramClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFollowedArtistsClick: () -> Unit,
    onImportYouTubePlaylistClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 24.dp, horizontal = 20.dp)
    ) {
        // Brand Header matching TopBar signature design
        val neonRed = Color(0xFFFF1E38)
        val darkBorderRed = Color(0xFFE50914)
        val lightBorderRed = Color(0xFFFF2B40)
        val darkShadowRed = Color(0x70E50914)
        val lightShadowRed = Color(0x35FF2236)

        val headerBgBrush = if (isDarkTheme) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF2B1419).copy(alpha = 0.94f),
                    Color(0xFF140A0D).copy(alpha = 0.98f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFAF2F4)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isDarkTheme) darkShadowRed else lightShadowRed,
                    ambientColor = if (isDarkTheme) darkShadowRed else lightShadowRed
                )
                .clip(RoundedCornerShape(20.dp))
                .background(headerBgBrush)
                .border(
                    width = 1.5.dp,
                    color = if (isDarkTheme) darkBorderRed else lightBorderRed,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Diamond Badge with Glowing Red Aura
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Soft Neon Red Aura
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        neonRed.copy(alpha = if (isDarkTheme) 0.50f else 0.40f),
                                        neonRed.copy(alpha = if (isDarkTheme) 0.18f else 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Rounded Diamond (tilted square)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .rotate(45f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isDarkTheme) Color(0xFF1E0E12) else Color(0xFFFFF0F2)
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFFFF2236),
                                shape = RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            modifier = Modifier.rotate(-45f),
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            color = neonRed
                        )
                    }
                }

                // Brand Typography
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "KAIRO",
                        color = neonRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "MUSIC",
                        color = if (isDarkTheme) Color(0xFF8E7A80) else Color(0xFF756267),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 4.sp
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        DrawerItem(
            icon = Icons.Default.Radio,
            label = "Online Player / Streams",
            testTag = "drawer_online_streams",
            onClick = onOnlineStreamsClick
        )

        DrawerItem(
            icon = Icons.Default.PlaylistAdd,
            label = "Import YouTube Playlist",
            testTag = "drawer_import_youtube",
            onClick = onImportYouTubePlaylistClick
        )

        DrawerItem(
            icon = Icons.Default.History,
            label = "Play History",
            testTag = "drawer_history",
            onClick = onHistoryClick
        )

        DrawerItem(
            icon = Icons.Default.People,
            label = "Followed Artists",
            testTag = "drawer_artists",
            onClick = onFollowedArtistsClick
        )

        DrawerItem(
            icon = Icons.Default.Notifications,
            label = "Notifications",
            testTag = "drawer_notifications",
            onClick = onNotificationsClick
        )

        DrawerItem(
            icon = Icons.Default.CameraAlt,
            label = "Follow Instagram",
            testTag = "drawer_instagram",
            onClick = onFollowInstagramClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        InstagramFollowButton(
            username = "rajatkairo",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                    contentDescription = null,
                    tint = if (isDarkTheme) KairoRed else Color(0xFFFFB300)
                )
                Text(
                    text = if (isDarkTheme) "Dark Theme" else "Light Theme",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = KairoRed
                ),
                modifier = Modifier.testTag("drawer_theme_switch")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "KAIRO Music v2.4 (Zero-API Edition)",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
