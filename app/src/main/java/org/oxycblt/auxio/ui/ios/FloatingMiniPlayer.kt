/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.oxycblt.auxio.ui.ios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import org.oxycblt.auxio.ui.ios.shapes.ContinuousRoundedRectangle
import org.oxycblt.auxio.ui.ios.LocalGlassEffectConfig
import org.oxycblt.auxio.ui.ios.liquidGlass
import org.oxycblt.auxio.ui.ios.LocalAppBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.drawBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.effects.blur
import org.oxycblt.auxio.ui.ios.backdrop.effects.lens
import org.oxycblt.auxio.ui.ios.backdrop.highlight.Highlight
import org.oxycblt.auxio.ui.ios.backdrop.shadow.Shadow
import org.oxycblt.auxio.ui.ios.backdrop.shadow.InnerShadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.auxio.playback.PlaybackViewModel
import kotlin.math.abs

@Composable
fun FloatingMiniPlayer(
    isInline: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLyricsClick: (() -> Unit)? = null,
    onQueueClick: (() -> Unit)? = null,
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val song by playbackViewModel.song.collectAsStateWithLifecycle()
    val isPlaying by playbackViewModel.isPlaying.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 56.dp.toPx() }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isSpringAnimating by remember { mutableStateOf(false) }
    var dragAnimationJob by remember { mutableStateOf<Job?>(null) }

    val pressInteractionSource = remember { MutableInteractionSource() }
    val isPressed by pressInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "miniPlayerPressScale",
    )

    val parentFrozen = LocalTabBarBackdropFrozen.current
    val isMiniPlayerFrozen: () -> Boolean = {
        isDragging || isSpringAnimating || abs(dragOffset) > 1f || (parentFrozen?.invoke() == true)
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(modifier)
            .background(Color.Transparent)
            .liquidGlass(
                config = LocalGlassEffectConfig.current.copy(
                    surfaceOpacity = 0.18f,
                    blurRadius = 8f,
                    lensHeight = 0.20f,
                    lensAmount = 0.30f,
                    chromaticAberration = false,
                    highlightOpacity = 0.45f
                ),
                shape = ContinuousRoundedRectangle(percent = 50),
                highlightAlpha = 0.35f,
                frozen = isMiniPlayerFrozen
            )
            .clipToBounds()
            .clickable(
                interactionSource = pressInteractionSource,
                indication = null,
                onClick = onClick,
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAnimationJob?.cancel()
                        isSpringAnimating = false
                        isDragging = true
                    },
                    onDragCancel = {
                        isDragging = false
                        isSpringAnimating = true
                        dragAnimationJob = coroutineScope.launch {
                            try {
                                Animatable(dragOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) {
                                    dragOffset = value
                                }
                            } finally {
                                dragOffset = 0f
                                isSpringAnimating = false
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val currentOffset = dragOffset
                        // Smooth physics resistance dampening
                        val resistance = 0.85f / (1f + (abs(currentOffset) / (thresholdPx * 2.5f)))
                        dragOffset = currentOffset + (dragAmount * resistance)
                    },
                    onDragEnd = {
                        isDragging = false
                        val currentOffset = dragOffset
                        if (currentOffset > thresholdPx) {
                            // Swiped right -> previous track
                            playbackViewModel.prev()
                        } else if (currentOffset < -thresholdPx) {
                            // Swiped left -> next track
                            playbackViewModel.next()
                        }
                        isSpringAnimating = true
                        dragAnimationJob = coroutineScope.launch {
                            try {
                                Animatable(dragOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) {
                                    dragOffset = value
                                }
                            } finally {
                                dragOffset = 0f
                                isSpringAnimating = false
                            }
                        }
                    }
                )
            }
    ) {
        val artSize = if (isInline) 36.dp else 44.dp
        val btnSize = if (isInline) 36.dp else 44.dp

        // -----------------------------------------------------------------
        // Layer 1: Anchored Background Swipe Action Icons
        // -----------------------------------------------------------------
        // Previous Track Button (Anchored at Alignment.CenterStart)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = if (isInline) 8.dp else 12.dp)
                .size(36.dp)
                .graphicsLayer {
                    val currentOffset = dragOffset
                    val absOffset = abs(currentOffset)
                    val t = (absOffset / thresholdPx).coerceAtMost(1.5f)
                    val scale = if (absOffset > 6f) {
                        if (t < 1.0f) {
                            0.35f + 0.65f * kotlin.math.sin(t * (Math.PI.toFloat() / 2f))
                        } else {
                            1.0f + kotlin.math.sin((t - 1f) * (Math.PI.toFloat() / 2f)) * 0.18f
                        }
                    } else 0f
                    val alphaVal = if (absOffset > 6f) (absOffset / (thresholdPx * 0.35f)).coerceIn(0f, 1f) else 0f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (currentOffset > 0f) alphaVal else 0f
                    translationX = (currentOffset * 0.20f).coerceAtMost(16.dp.toPx())
                }
                .crystalGlass(
                    shape = CircleShape,
                    highlightAlpha = 0.5f,
                    washAlpha = 0.035f,
                    blurRadiusDp = 2.5f,
                    frozen = { false }
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Next Track Button (Anchored at Alignment.CenterEnd)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = if (isInline) 8.dp else 12.dp)
                .size(36.dp)
                .graphicsLayer {
                    val currentOffset = dragOffset
                    val absOffset = abs(currentOffset)
                    val t = (absOffset / thresholdPx).coerceAtMost(1.5f)
                    val scale = if (absOffset > 6f) {
                        if (t < 1.0f) {
                            0.35f + 0.65f * kotlin.math.sin(t * (Math.PI.toFloat() / 2f))
                        } else {
                            1.0f + kotlin.math.sin((t - 1f) * (Math.PI.toFloat() / 2f)) * 0.18f
                        }
                    } else 0f
                    val alphaVal = if (absOffset > 6f) (absOffset / (thresholdPx * 0.35f)).coerceIn(0f, 1f) else 0f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (currentOffset < 0f) alphaVal else 0f
                    translationX = (currentOffset * 0.20f).coerceAtLeast(-16.dp.toPx())
                }
                .crystalGlass(
                    shape = CircleShape,
                    highlightAlpha = 0.5f,
                    washAlpha = 0.035f,
                    blurRadiusDp = 2.5f,
                    frozen = { false }
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // -----------------------------------------------------------------
        // Layer 2: Foreground Content Row (Album Art + Track Info + Play/Pause Button)
        // Entire Row translates with finger drag; fixed children eliminate relayout thrashing
        // -----------------------------------------------------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isInline) 8.dp else 12.dp,
                    vertical = if (isInline) 0.dp else 8.dp
                )
                .graphicsLayer {
                    translationX = dragOffset
                }
        ) {
            // 1. AsyncImage album art
            AsyncImage(
                model = song?.cover,
                contentDescription = "Cover Art",
                fallback = painterResource(id = R.drawable.ic_song_48),
                placeholder = painterResource(id = R.drawable.ic_song_48),
                error = painterResource(id = R.drawable.ic_song_48),
                modifier = Modifier
                    .size(artSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // 2. Spacer between album art and track info
            Spacer(Modifier.width(if (isInline) 8.dp else 12.dp))

            // 3. Track Info Column (Song Title & Artist)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = pressInteractionSource,
                        indication = null,
                        onClick = onClick,
                    )
            ) {
                val context = LocalContext.current
                Text(
                    text = song?.name?.resolve(context) ?: "No Song Playing",
                    style = if (isInline) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artists?.resolveNames(context) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. Spacer between track info and play/pause button
            Spacer(Modifier.width(8.dp))

            // 5. Play/Pause button Box
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .crystalGlass(
                        shape = CircleShape,
                        highlightAlpha = 0.5f,
                        washAlpha = 0.035f,
                        blurRadiusDp = 2.5f,
                        frozen = { parentFrozen?.invoke() == true }
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = { playbackViewModel.togglePlaying() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(if (isInline) 18.dp else 22.dp)
                )
            }
        }
    }
}
