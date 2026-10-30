package org.oxycblt.auxio.ui.ios

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RepeatOne
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur as composeBlur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import org.oxycblt.auxio.ui.ios.backdrop.drawBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.effects.blur
import org.oxycblt.auxio.ui.ios.backdrop.effects.lens
import org.oxycblt.auxio.ui.ios.backdrop.highlight.Highlight
import org.oxycblt.auxio.ui.ios.backdrop.shadow.InnerShadow
import org.oxycblt.auxio.ui.ios.backdrop.shadow.Shadow
import org.oxycblt.auxio.ui.ios.backdrop.catalog.utils.InteractiveHighlight
import org.oxycblt.auxio.ui.ios.LocalAppBackdrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.playback.state.RepeatMode
import kotlin.math.sin

@Composable
fun PlayerV2(
    playbackViewModel: PlaybackViewModel,
    queueViewModel: Any,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val song by playbackViewModel.song.collectAsStateWithLifecycle()
    val isPlaying by playbackViewModel.isPlaying.collectAsStateWithLifecycle()
    val positionDs by playbackViewModel.positionDs.collectAsStateWithLifecycle()
    val durationMs = song?.durationMs ?: 0L
    val repeatMode by playbackViewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffled by playbackViewModel.isShuffled.collectAsStateWithLifecycle()

    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(positionDs) {
        position = positionDs * 100L
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 100.dp.toPx() }
    val dragOffsetYAnimatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        dragOffsetYAnimatable.snapTo(0f)
    }

    val backdrop = LocalAppBackdrop.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dragOffsetYAnimatable.value
            }
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp) },
                        backdropScale = 0.5f,
                        effects = {
                            blur(18f.dp.toPx())
                            lens(8f.dp.toPx(), 12f.dp.toPx(), chromaticAberration = false)
                        },
                        onDrawSurface = {
                            drawRect(Color(0xFF121214).copy(alpha = 0.40f))
                            drawRect(Color.White.copy(alpha = 0.22f))
                        },
                        highlight = null,
                        shadow = { Shadow.Default.copy(alpha = 0.25f) },
                        frozen = { false }
                    )
                } else {
                    Modifier.background(Color(0xFF1C1C1E))
                }
            )
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetYAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0 || dragOffsetYAnimatable.value > 0) {
                            change.consume()
                            val nextOffset = (dragOffsetYAnimatable.value + dragAmount).coerceAtLeast(0f)
                            coroutineScope.launch {
                                dragOffsetYAnimatable.snapTo(nextOffset)
                            }
                        }
                    },
                    onDragEnd = {
                        if (dragOffsetYAnimatable.value > dismissThresholdPx) {
                            coroutineScope.launch {
                                dragOffsetYAnimatable.snapTo(0f)
                            }
                            onClose()
                        } else {
                            coroutineScope.launch {
                                dragOffsetYAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle pill at the top
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.35f))
                    .clickable { onClose() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Spring animated artwork
            val artScale by animateFloatAsState(
                targetValue = if (isPlaying) 1f else 0.88f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "artScale"
            )
            val artElevation by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 4.dp,
                animationSpec = tween(400),
                label = "artElevation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding((32.dp * (1f - artScale)).coerceAtLeast(0.dp))
                    .shadow(artElevation, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
            ) {
                AsyncImage(
                    model = song?.cover,
                    contentDescription = "Cover Art",
                    fallback = painterResource(id = R.drawable.ic_song_48),
                    placeholder = painterResource(id = R.drawable.ic_song_48),
                    error = painterResource(id = R.drawable.ic_song_48),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata: song.name, song.artists, song.album.name resolved properly
            val songTitle = song?.name?.resolve(context) ?: "Unknown Song"
            val songArtists = song?.artists?.resolveNames(context) ?: "Unknown Artist"
            val songAlbum = song?.album?.name?.resolve(context) ?: ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = songArtists,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (songAlbum.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = songAlbum,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White
                    )
                }
            }

            // Wavy progress bar with smooth thumb scrubbing
            val currentProgress = if (durationMs > 0L) (position.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

            WavyProgressBar(
                progress = currentProgress,
                onSeek = { seekFrac ->
                    val targetDs = ((seekFrac * durationMs) / 100f).toLong()
                    playbackViewModel.seekTo(targetDs)
                },
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPosText = formatTime(position)
                val remainPosText = "-" + formatTime(maxOf(0L, durationMs - position))
                Text(currentPosText, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                Text(remainPosText, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Side-by-side layout: Vertical Volume Panel (Left) & Controls Area (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Interactive Vertical Volume Panel
                VerticalVolumePanel(
                    modifier = Modifier
                        .width(54.dp)
                        .height(165.dp),
                    backdrop = backdrop
                )

                // Right Side: Media Control Buttons
                Column(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Previous (48dp), Play/Pause (68dp), Next (48dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularGlassControl(
                            onClick = { playbackViewModel.prev() },
                            size = 48.dp
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        CircularGlassControl(
                            onClick = { playbackViewModel.togglePlaying() },
                            size = 68.dp
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        CircularGlassControl(
                            onClick = { playbackViewModel.next() },
                            size = 48.dp
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Bottom Row directly below buttons: Repeat & Shuffle
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { playbackViewModel.toggleRepeatMode() }) {
                            val repeatIcon = when (repeatMode) {
                                RepeatMode.TRACK -> Icons.Filled.RepeatOne
                                else -> Icons.Filled.Repeat
                            }
                            val repeatTint = when (repeatMode) {
                                RepeatMode.NONE -> Color.White.copy(alpha = 0.45f)
                                else -> Color.White
                            }
                            Icon(
                                imageVector = repeatIcon,
                                contentDescription = "Repeat",
                                tint = repeatTint
                            )
                        }

                        IconButton(onClick = { playbackViewModel.toggleShuffled() }) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffled) Color.White else Color.White.copy(alpha = 0.45f)
                            )
                        }

                        IconButton(onClick = {
                            val pm = context.packageManager
                            val powerampIntent = pm.getLaunchIntentForPackage("com.maxmpz.equalizer")
                            if (powerampIntent != null) {
                                powerampIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(powerampIntent)
                                } catch (_: Exception) {}
                            } else {
                                val systemIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(systemIntent)
                                } catch (_: Exception) {}
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thin, sleek wavy progress bar with animated sine ripple during playback
 * and smooth thumb dragging with instant responsive touch feedback.
 */
@Composable
fun WavyProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val waveAmplitudeAnim by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "waveAmplitude"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "thumbScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        dragProgress = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                onSeek(dragProgress)
                                break
                            }
                            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            change.consume()
                        }
                        isDragging = false
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val trackStroke = 3.dp.toPx()
            val maxAmplitude = 3.2.dp.toPx() * waveAmplitudeAnim
            val wavelength = 20.dp.toPx()
            val thumbX = (displayProgress * width).coerceIn(0f, width)

            // Inactive track (behind thumb to right)
            if (thumbX < width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(thumbX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = trackStroke,
                    cap = StrokeCap.Round
                )
            }

            // Active track (from 0 to thumb)
            if (thumbX > 0f) {
                if (maxAmplitude > 0.1f) {
                    val path = Path().apply {
                        moveTo(0f, centerY)
                        val stepPx = 2.dp.toPx()
                        var x = 0f
                        while (x < thumbX) {
                            x += stepPx
                            if (x > thumbX) x = thumbX
                            val taperStart = (x / (14.dp.toPx())).coerceIn(0f, 1f)
                            val taperEnd = ((thumbX - x) / (14.dp.toPx())).coerceIn(0f, 1f)
                            val envelope = taperStart * taperEnd
                            val y = centerY + sin((x / wavelength) * (2f * Math.PI).toFloat() + wavePhase) * (maxAmplitude * envelope)
                            lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = trackStroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, centerY),
                        end = Offset(thumbX, centerY),
                        strokeWidth = trackStroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Sleek circular thumb
            val baseRadius = 6.dp.toPx()
            val currentRadius = baseRadius * thumbScale
            // Ambient soft drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = currentRadius + 2.dp.toPx(),
                center = Offset(thumbX, centerY + 1.dp.toPx())
            )
            // Crisp White thumb
            drawCircle(
                color = Color.White,
                radius = currentRadius,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

/**
 * Interactive vertical volume panel with crystal liquid glass fill,
 * frosted background container, and real-time Android AudioManager volume sync.
 */
@Composable
fun VerticalVolumePanel(
    modifier: Modifier = Modifier,
    backdrop: org.oxycblt.auxio.ui.ios.backdrop.Backdrop? = null
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var currentVolumeRatio by remember {
        mutableFloatStateOf(
            (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume).coerceIn(0f, 1f)
        )
    }

    DisposableEffect(context, audioManager, maxVolume) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val newVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    currentVolumeRatio = (newVol.toFloat() / maxVolume).coerceIn(0f, 1f)
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        val contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val newVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                currentVolumeRatio = (newVol.toFloat() / maxVolume).coerceIn(0f, 1f)
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (e: Exception) {}

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            try { context.contentResolver.unregisterContentObserver(contentObserver) } catch (e: Exception) {}
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(27.dp))
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(27.dp) },
                        effects = {
                            blur(8f.dp.toPx())
                            lens(4f.dp.toPx(), 6f.dp.toPx(), chromaticAberration = false)
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) },
                        highlight = null,
                        shadow = null
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.15f))
                }
            )
            .pointerInput(maxVolume) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val heightPx = size.height.toFloat()
                        if (heightPx > 0f) {
                            val newRatio = (1f - (down.position.y / heightPx)).coerceIn(0f, 1f)
                            currentVolumeRatio = newRatio
                            val targetVol = (newRatio * maxVolume).roundToInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                        }
                        val pointer = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointer } ?: break
                            if (!change.pressed) break
                            val newRatio = (1f - (change.position.y / heightPx)).coerceIn(0f, 1f)
                            currentVolumeRatio = newRatio
                            val targetVol = (newRatio * maxVolume).roundToInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val totalHeight = maxHeight
        val fillHeight = (totalHeight * currentVolumeRatio).coerceAtLeast(0.dp)

        // Volume progress track filled from bottom to top with Crystal Liquid Glass
        if (currentVolumeRatio > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fillHeight)
                    .clip(RoundedCornerShape(27.dp))
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(27.dp) },
                                backdropScale = 1f,
                                effects = {
                                    blur(2.5f.dp.toPx())
                                    lens(8f.dp.toPx(), 12f.dp.toPx(), chromaticAberration = false)
                                },
                                highlight = { Highlight.Default.copy(alpha = 0.5f) },
                                shadow = null,
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.035f)) }
                            )
                        } else {
                            Modifier.background(Color.White.copy(alpha = 0.15f))
                        }
                    )
            )
        }

        // Live percentage text centered inside panel
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${(currentVolumeRatio * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Circular iOS liquid glass control button with specular highlight rim,
 * centered circular ripple effect, and 0.88x press bounce animation.
 */
@Composable
fun CircularGlassControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressProgress"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "circularControlScale"
    )

    val backdrop = LocalAppBackdrop.current
    val coroutineScope = rememberCoroutineScope()
    val interactiveHighlight = remember(coroutineScope) {
        InteractiveHighlight(
            animationScope = coroutineScope,
            position = { size, _ -> Offset(size.width / 2f, size.height / 2f) }
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(interactiveHighlight.gestureModifier)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        backdropScale = 1f,
                        effects = {
                            blur(2.5f.dp.toPx())
                            lens(
                                8f.dp.toPx() + 2f.dp.toPx() * pressProgress,
                                12f.dp.toPx() + 2f.dp.toPx() * pressProgress,
                                chromaticAberration = false
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.5f + 0.5f * pressProgress) },
                        shadow = { Shadow.Default.copy(alpha = 0.25f + 0.45f * pressProgress) },
                        innerShadow = { InnerShadow.Default.copy(radius = 8f.dp, alpha = 0.35f + 0.65f * pressProgress) },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.035f * (1f - 0.5f * pressProgress)))
                        }
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.18f + 0.12f * pressProgress), CircleShape)
                }
            )
            .clip(CircleShape)
            .then(interactiveHighlight.modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.White
                ),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
