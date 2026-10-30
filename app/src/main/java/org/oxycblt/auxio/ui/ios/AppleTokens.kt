package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppleTokens {
    val AccentRed = Color(0xFFFA2D48)
    
    val Bg: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF000000) else Color(0xFFFFFFFF)
        
    val BgElevated: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color(0xFFF2F2F7)
        
    val Card: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
        
    val CardSecondary: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
        
    val Metadata: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF8E8E93) else Color(0xFF8E8E93)

    val TextPrimary: Color
        @Composable get() = if (isSystemInDarkTheme()) Color.White else Color.Black
        
    val TextSecondary: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
        
    val GlassSurface: Color
        @Composable get() = if (isSystemInDarkTheme()) Color.Black else Color.White

    val Gutter = 20.dp
    val ItemGap = 16.dp
    val Artwork = 12.dp
    val Control = 12.dp
    val CardCorner = 22.dp
}
