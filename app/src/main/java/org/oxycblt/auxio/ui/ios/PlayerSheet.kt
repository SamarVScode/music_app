package org.oxycblt.auxio.ui.ios

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.oxycblt.musikr.Song
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames

@Composable
fun PlayerSheet(
    song: Song?,
    isPlaying: Boolean,
    progress: Float,
    durationMs: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    audioSessionId: Int = 0,
    isShuffleOn: Boolean = false,
    isRepeatOn: Boolean = false
) {
    val context = LocalContext.current
    val title = song?.name?.resolve(context) ?: "Unknown Title"
    val artist = song?.artists?.resolveNames(context) ?: "Unknown Artist"

    var showQueue by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Empty background for pure frosted acrylic blur

        // Cupertino glass player container overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val artSize by animateDpAsState(
                targetValue = if (isPlaying) 320.dp else 260.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "artSize"
            )
            val shadowElevation by animateDpAsState(
                targetValue = if (isPlaying) 32.dp else 12.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "shadowElevation"
            )

            // Dynamic spring artwork with ambient glow shadow
            Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = song?.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(artSize)
                        .shadow(shadowElevation, RoundedCornerShape(24.dp), ambientColor = Color.White, spotColor = Color.Black)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.DarkGray)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White, maxLines = 1)
            Text(text = artist, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            var isDragging by remember { mutableStateOf(false) }
            var dragProgress by remember { mutableStateOf(0f) }
            
            val displayProgress = if (isDragging) dragProgress else progress
            val elapsedMs = (displayProgress * durationMs).toLong()
            val remainingMs = durationMs - elapsedMs
            
            fun formatTime(ms: Long): String {
                if (ms < 0) return "0:00"
                val totalSeconds = ms / 1000
                val m = totalSeconds / 60
                val s = totalSeconds % 60
                return String.format("%d:%02d", m, s)
            }

            Slider(
                value = displayProgress,
                onValueChange = {
                    isDragging = true
                    dragProgress = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeek((dragProgress * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(elapsedMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Text("-${formatTime(remainingMs)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (isShuffleOn) Color.White else Color.White.copy(alpha = 0.5f))
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = onToggleRepeat) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = if (isRepeatOn) Color.White else Color.White.copy(alpha = 0.5f))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val pm = context.packageManager
                    val powerampIntent = pm.getLaunchIntentForPackage("com.maxmpz.equalizer")
                    if (powerampIntent != null) {
                        powerampIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        powerampIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                        powerampIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        context.startActivity(powerampIntent)
                    } else {
                        val systemIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(systemIntent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No equalizer app installed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = Color.White.copy(alpha = 0.7f))
                }
                IconButton(onClick = { showQueue = !showQueue }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = if (showQueue) Color.White else Color.White.copy(alpha = 0.7f))
                }
            }
            
            if (showQueue) {
                // Placeholder for Queue Sheet
                Text("Queue Sheet Overlay", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }
    }
}
