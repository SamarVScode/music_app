package org.oxycblt.auxio.ui.ios.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.oxycblt.auxio.ui.ios.LocalAppBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.Backdrop

/** saturation floor shared with [AppleTokens.shiftedForContrast] (0.08f). */
internal fun Color.isHueless(): Boolean {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    return hsl[1] < 0.08f
}

/**
 * Wraps [content] in a nested [MaterialTheme] whose colour scheme is derived from
 * the screen's own hero [tint], so every `MaterialTheme.colorScheme` read on that
 * screen agrees with the background plane.
 *
 * When [tint] is hueless (grey / black / near-white), the app-wide scheme is kept
 * intact â€” re-theming from a grey produces a strictly worse result.
 *
 * Also provides [LocalAppBackdrop], [LocalContentColor] and [LocalAccentTextColor]
 * so glass chrome and explicit colour references stay in sync.
 */
@Composable
fun HeroTintedContent(
    tint: Color,
    backdrop: Backdrop,
    content: @Composable () -> Unit,
) {
    val base = MaterialTheme.colorScheme
    val onDark = tint.luminance() <= 0.5f

    // Theme screen's explicit text color, when set, wins over the tint-derived
    // hero text colors below â€” same override vivimusicTheme's own flatText
    // already applies to plain MaterialTheme.colorScheme reads, extended to
    // this screen-local system so hero screens (Home, Search, Library) agree
    // with it too instead of always following their artwork tint.
    val appTextColorInt = 0

    // Was `base.accentText(tint, onDark)` alone, so any screen under this â€” Album,
    // Artist, Library, Search, every playlist screen â€” that reads
    // MaterialTheme.colorScheme.onSurface/onSurfaceVariant/secondary directly
    // (rather than through LocalContentColor/LocalAccentTextColor below) fell back
    // to the hero-tint-derived color and ignored the global text color setting.
    // flatText is the same override vivimusicTheme applies at the root; it has to
    // be re-applied here because this MaterialTheme rebuilds the scheme from scratch.
    val scheme = remember(base, tint, onDark, appTextColorInt) {
        val tinted = base.accentText(tint, onDark)
        if (appTextColorInt != 0) tinted.flatText(Color(appTextColorInt)) else tinted
    }
    val contentColor = if (appTextColorInt != 0) Color(appTextColorInt) else AppleTokens.onColor(tint)
    val headingColor = if (appTextColorInt != 0) Color(appTextColorInt) else AppleTokens.onColorHeading(tint)

    if (tint.isHueless()) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAppBackdrop provides backdrop,
            LocalContentColor provides contentColor,
            LocalAccentTextColor provides headingColor,
        ) {
            content()
        }
    } else {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalAppBackdrop provides backdrop,
                LocalContentColor provides contentColor,
                LocalAccentTextColor provides headingColor,
            ) {
                content()
            }
        }
    }
}



val LocalAccentTextColor = androidx.compose.runtime.compositionLocalOf { Color.Unspecified }

fun ColorScheme.appleSurfaces() = copy(
    background = AppleTokens.Bg,
    surface = AppleTokens.Bg,
    surfaceContainerLowest = AppleTokens.Bg,
    surfaceContainerLow = AppleTokens.BgElevated,
    surfaceContainer = AppleTokens.Card,
    surfaceContainerHigh = AppleTokens.Card,
    surfaceContainerHighest = AppleTokens.CardSecondary,
    surfaceVariant = AppleTokens.CardSecondary,
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = AppleTokens.CardSecondary,
    onPrimaryContainer = Color.White,
)

fun accentTextColor(accent: Color, dark: Boolean): Color {
    val blended = if (dark) {
        androidx.compose.ui.graphics.lerp(accent, Color.White, 0.35f)
    } else {
        androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.45f)
    }
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(blended.toArgb(), hsl)
    hsl[2] = if (dark) hsl[2].coerceAtLeast(0.72f) else hsl[2].coerceAtMost(0.32f)
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

fun ColorScheme.accentText(accent: Color, dark: Boolean): ColorScheme {
    val body = if (dark) {
        androidx.compose.ui.graphics.lerp(accent, Color.White, 0.86f)
    } else {
        androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.86f)
    }
    val secondary = accentTextColor(accent, dark)
    return copy(
        onSurface = body,
        onBackground = body,
        onSurfaceVariant = secondary,
        onSecondaryContainer = secondary,
        onTertiaryContainer = secondary,
        onPrimaryContainer = secondary,
        onSecondary = secondary,
        secondary = secondary,
        primary = secondary,
        onPrimary = if (dark) Color.Black else Color.White,
    )
}

fun ColorScheme.flatText(color: Color) = copy(
    onSurface = color,
    onBackground = color,
    onSurfaceVariant = color,
    onSecondaryContainer = color,
    onTertiaryContainer = color,
    onPrimaryContainer = color,
    onSecondary = color,
    secondary = color,
    primary = color,
    onPrimary = if (color.luminance() > 0.5f) Color.Black else Color.White,
)


