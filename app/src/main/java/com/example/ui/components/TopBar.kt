package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Signature Neon-Glow KAIRO Header matching reference designs for both Dark and Light mode.
 *
 * Design features:
 * - Rounded pill container with vibrant neon red border & glowing outer aura
 * - Glossy rounded-square glass hamburger menu tile on the left
 * - Glowing KAIRO Diamond logo badge with upright letter 'K'
 * - Bold neon red "KAIRO" with letter-spaced "MUSIC" subtitle
 * - Strictly only the buttons shown in reference image (all other extraneous buttons removed)
 */
@Composable
fun TopBar(
    onMenuClick: () -> Unit,
    isDarkTheme: Boolean = true,
    onSearchClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    hasNotificationBadge: Boolean = false,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Theme palette constants matching the provided Dark and Light mode images
    val neonRed = Color(0xFFFF1E38)

    val darkBorderRed = Color(0xFFE50914)
    val darkShadowRed = Color(0x95E50914)
    val darkBgBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF2B1419).copy(alpha = 0.94f),
            Color(0xFF140A0D).copy(alpha = 0.98f)
        )
    )

    val lightBorderRed = Color(0xFFFF2B40)
    val lightShadowRed = Color(0x50FF2236)
    val lightBgBrush = Brush.verticalGradient(
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFAF2F4)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Outer Glass Capsule with Neon Red Border and Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDarkTheme) darkShadowRed else lightShadowRed,
                    ambientColor = if (isDarkTheme) darkShadowRed else lightShadowRed
                )
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDarkTheme) darkBgBrush else lightBgBrush)
                .border(
                    width = 1.6.dp,
                    color = if (isDarkTheme) darkBorderRed else lightBorderRed,
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle specular glass reflection along the top curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isDarkTheme) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // LEFT: Hamburger Menu Tile
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(46.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 6.dp else 4.dp,
                        shape = RoundedCornerShape(13.dp),
                        spotColor = if (isDarkTheme) Color(0x40000000) else Color(0x18000000),
                        ambientColor = if (isDarkTheme) Color(0x30000000) else Color(0x10000000)
                    )
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (isDarkTheme) {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x30FFFFFF),
                                    Color(0x12FFFFFF),
                                    Color(0xFF201317)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF3ECEE)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isDarkTheme) {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x45FFFFFF),
                                    Color(0x18FFFFFF),
                                    Color(0x28E50914)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x60FFFFFF),
                                    Color(0x35E50914)
                                )
                            )
                        },
                        shape = RoundedCornerShape(13.dp)
                    )
                    .clickable(onClick = onMenuClick)
                    .testTag("menu_button"),
                contentAlignment = Alignment.Center
            ) {
                // 3 Hamburger Horizontal Bars
                val barColor = if (isDarkTheme) Color(0xFFDAD5D8) else Color(0xFF70676A)
                Column(
                    modifier = Modifier.width(18.dp),
                    verticalArrangement = Arrangement.spacedBy(3.6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.2.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.2.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.2.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                }
            }

            // CENTER: KAIRO Diamond Logo & Brand Typography
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Diamond Badge with Glowing Red Aura
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Soft Neon Red Aura / Halo
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        neonRed.copy(alpha = if (isDarkTheme) 0.52f else 0.42f),
                                        neonRed.copy(alpha = if (isDarkTheme) 0.20f else 0.14f),
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

                Spacer(modifier = Modifier.width(12.dp))

                // Brand Typography
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "KAIRO",
                        color = neonRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "MUSIC",
                        color = if (isDarkTheme) Color(0xFF8E7A80) else Color(0xFF756267),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 4.5.sp
                    )
                }
            }
        }
    }
}
