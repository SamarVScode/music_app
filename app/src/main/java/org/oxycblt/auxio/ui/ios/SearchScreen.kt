/*
 * Copyright (c) 2026 Auxio Project
 * SearchScreen.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.oxycblt.auxio.ui.ios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.search.SearchViewModel

/**
 * Authentic iOS Search Modal Overlay:
 * - Originates PRECISELY from the circular search button on the navigation bar
 * - Morphs smoothly from the search button circle into the full rounded rectangular card (28dp corners)
 * - Sits cleanly above the bottom navigation bar without overlapping
 * - Live crystal glass blur with zero stale background caching
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    isTransitioning: () -> Boolean = { false },
    searchProgress: Float = 1f,
    bottomPadding: Dp = 100.dp,
    searchButtonBounds: Rect = Rect.Zero
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 100.dp.toPx() }
    val dragOffsetYAnimatable = remember { Animatable(0f) }

    var cardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    val dragGestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { },
            onDragCancel = {
                coroutineScope.launch {
                    dragOffsetYAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
            },
            onVerticalDrag = { change, dragAmount ->
                if (dragAmount > 0 || dragOffsetYAnimatable.value > 0) {
                    change.consume()
                    val resistance = if (dragOffsetYAnimatable.value > dismissThresholdPx) 0.6f else 1f
                    val nextOffset = (dragOffsetYAnimatable.value + dragAmount * resistance).coerceAtLeast(0f)
                    coroutineScope.launch {
                        dragOffsetYAnimatable.snapTo(nextOffset)
                    }
                }
            },
            onDragEnd = {
                if (dragOffsetYAnimatable.value > dismissThresholdPx) {
                    onBack?.invoke()
                } else {
                    coroutineScope.launch {
                        dragOffsetYAnimatable.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
        )
    }

    // Scrim: Smoothly fades in / out
    val scrimAlpha = (searchProgress * 0.6f).coerceIn(0f, 0.6f)
    val clampedProgress = searchProgress.coerceIn(0f, 1f)

    // Constant shape: at 56dp size it is a 100% circle, at full modal size it is a clean rounded rectangle.
    // Zero dome distortion!
    val cardShape = remember { RoundedCornerShape(28.dp) }

    // Content fade: Keep text clean and fade in gracefully as card blooms
    val contentAlpha = ((clampedProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Invisible Target Anchor: strictly measures the settled modal layout above the nav bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = bottomPadding)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        cardBoundsInRoot = coords.boundsInRoot()
                    }
                }
        )

        // Scrim / Dimmed backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onBack?.invoke() }
                )
        )

        // Interpolate geometry from search button (56dp circle) to target modal bounds
        val size56Px = with(density) { 56.dp.toPx() }
        // Synchronous Frame-0 Target Bounds (no 1-frame async layout lag)
        val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
        val targetLeft = with(density) { 16.dp.toPx() }
        val targetTop = statusBarTopPx + with(density) { 16.dp.toPx() }
        val targetWidth = screenWidthPx - with(density) { 32.dp.toPx() }
        val targetHeight = screenHeightPx - targetTop - with(density) { (bottomPadding + 16.dp).toPx() }

        val hasValidSearchBtn = searchButtonBounds != Rect.Zero && searchButtonBounds.width > 0f
        val originLeft = if (hasValidSearchBtn) {
            searchButtonBounds.center.x - size56Px / 2f
        } else {
            targetLeft + targetWidth - size56Px
        }
        val originTop = if (hasValidSearchBtn) {
            searchButtonBounds.center.y - size56Px / 2f
        } else {
            targetTop + targetHeight + with(density) { 16.dp.toPx() }
        }
        val originWidth = size56Px
        val originHeight = size56Px

        // Directional open/close progress:
        // Subtle iOS bounce overshoot (up to 1.04f) strictly when opening (> 0.95f)
        // Clean linear/smooth interpolation [0f, 1f] when closing directly targeting searchButtonBounds
        val motionProgress = if (searchProgress > 1f) {
            searchProgress.coerceAtMost(1.04f)
        } else {
            searchProgress.coerceIn(0f, 1f)
        }

        val currentLeftPx = lerp(originLeft, targetLeft, motionProgress)
        val currentTopPx = lerp(originTop, targetTop, motionProgress) + dragOffsetYAnimatable.value
        val currentWidthPx = lerp(originWidth, targetWidth, motionProgress).coerceAtLeast(size56Px)
        val currentHeightPx = lerp(originHeight, targetHeight, motionProgress).coerceAtLeast(size56Px)

        val currentLeftDp = with(density) { currentLeftPx.toDp() }
        val currentTopDp = with(density) { currentTopPx.toDp() }
        val currentWidthDp = with(density) { currentWidthPx.toDp() }
        val currentHeightDp = with(density) { currentHeightPx.toDp() }

        Box(
            modifier = Modifier
                .offset(x = currentLeftDp, y = currentTopDp)
                .size(width = currentWidthDp, height = currentHeightDp)
        ) {
            SearchCard(
                modifier = Modifier.fillMaxSize(),
                shape = cardShape,
                contentAlpha = contentAlpha,
                searchViewModel = searchViewModel,
                playbackViewModel = playbackViewModel,
                onBack = onBack,
                dragGestureModifier = dragGestureModifier,
                frozen = { false }
            )
        }
    }
}
