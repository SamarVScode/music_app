package org.oxycblt.auxio.ui.ios

import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.ui.ios.shapes.ContinuousRoundedRectangle
import org.oxycblt.musikr.Song
import java.io.File
import kotlin.math.roundToInt

/**
 * Authentic iOS Cupertino Floating Context Menu Modal originating directly from the touch coordinates of the long-press.
 */
@Composable
fun SongFloatingContextMenu(
    song: Song,
    touchOffset: Offset,
    onDismiss: () -> Unit,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            musicViewModel.rescan()
        }
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val menuWidthDp = 260.dp
    val menuWidthPx = with(density) { menuWidthDp.toPx() }
    val menuEstimatedHeightPx = with(density) { 320.dp.toPx() }

    val isBottomHalf = touchOffset.y > (screenHeightPx * 0.52f)
    val anchorY = if (isBottomHalf) 1.0f else 0.0f
    val menuY = if (isBottomHalf) {
        (touchOffset.y - menuEstimatedHeightPx - with(density) { 12.dp.toPx() })
            .coerceAtLeast(with(density) { 64.dp.toPx() })
    } else {
        (touchOffset.y + with(density) { 12.dp.toPx() })
            .coerceAtMost(screenHeightPx - menuEstimatedHeightPx - with(density) { 64.dp.toPx() })
    }
    val menuX = (touchOffset.x - menuWidthPx / 2f).coerceIn(
        with(density) { 16.dp.toPx() },
        screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() }
    )
    val anchorX = ((touchOffset.x - menuX) / menuWidthPx).coerceIn(0.1f, 0.9f)

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.76f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-screen dismiss overlay with subtle blur darkening
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * anim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // 2. Floating Context Menu Container blooming from touch origin
        val glassConfig = LocalGlassEffectConfig.current.copy(
            blurRadius = 16f,
            lensHeight = 0.35f,
            lensAmount = 0.50f
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(menuX.roundToInt(), menuY.roundToInt()) }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(anchorX, anchorY)
                    scaleX = 0.2f + 0.8f * anim.value
                    scaleY = 0.2f + 0.8f * anim.value
                    alpha = anim.value
                }
                .width(menuWidthDp)
                .shadow(16.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                .liquidGlass(
                    config = glassConfig,
                    shape = ContinuousRoundedRectangle(20.dp),
                    applyEdgeEffects = true,
                    blurRadiusDp = 16f,
                    highlightAlpha = 0.50f,
                    frozen = { false }
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.15f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                // Song Header Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.cover,
                        contentDescription = "Cover",
                        fallback = painterResource(id = R.drawable.ic_song_48),
                        placeholder = painterResource(id = R.drawable.ic_song_48),
                        error = painterResource(id = R.drawable.ic_song_48),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.name.resolve(context),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.artists.resolveNames(context),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    thickness = 0.5.dp
                )

                // Menu Actions
                ContextMenuItem(
                    icon = Icons.Filled.SkipNext,
                    label = "Play Next",
                    onClick = {
                        playbackViewModel.playNext(song)
                        onDismiss()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Filled.QueueMusic,
                    label = "Add to Queue",
                    onClick = {
                        playbackViewModel.addToQueue(song)
                        onDismiss()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Filled.PlaylistAdd,
                    label = "Add to Playlist",
                    onClick = {
                        musicViewModel.addToPlaylist(song)
                        onDismiss()
                    }
                )

                ContextMenuItem(
                    icon = Icons.Filled.Info,
                    label = "Song Details",
                    onClick = {
                        showDetailsDialog = true
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    thickness = 0.5.dp
                )

                // Destructive Delete Action
                ContextMenuItem(
                    icon = Icons.Filled.Delete,
                    label = "Delete Song",
                    tint = Color(0xFFFF453A),
                    onClick = {
                        showDeleteConfirmation = true
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete Song?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${song.name.resolve(context)}\" from device storage?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(song.uri))
                                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                try {
                                    context.contentResolver.delete(song.uri, null, null)
                                    musicViewModel.rescan()
                                } catch (e: RecoverableSecurityException) {
                                    val pi = e.userAction.actionIntent
                                    deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                                }
                            } else {
                                context.contentResolver.delete(song.uri, null, null)
                                musicViewModel.rescan()
                            }
                        } catch (e: Exception) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    File(song.path.toString()).delete()
                                } catch (_: Exception) {}
                                musicViewModel.rescan()
                            }
                        }
                        showDeleteConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF453A), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xF01C1C1E),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Song Details Dialog
    if (showDetailsDialog) {
        val title = song.name.resolve(context)
        val artist = song.artists.resolveNames(context)
        val album = song.album.name.resolve(context)
        val duration = "${song.durationMs / 60000}:${String.format("%02d", (song.durationMs % 60000) / 1000)}"
        val bitrate = "${song.bitrateKbps} kbps"
        val path = song.path.toString()

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Text("Song Details", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Title", title)
                    DetailRow("Artist", artist)
                    DetailRow("Album", album)
                    DetailRow("Duration", duration)
                    DetailRow("Bitrate", bitrate)
                    DetailRow("Path", path)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color(0xF01C1C1E),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Normal)
    }
}

val LocalSongContextMenu = compositionLocalOf<((Song, Offset) -> Unit)?> { null }

/**
 * Backward compatibility stub for callers expecting SongContextSheet
 */
@Composable
fun SongContextSheet(
    song: Song,
    onDismiss: () -> Unit,
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val openMenu = LocalSongContextMenu.current
    if (openMenu != null) {
        LaunchedEffect(song) {
            openMenu(song, Offset(540f, 1000f))
            onDismiss()
        }
    } else {
        SongFloatingContextMenu(
            song = song,
            touchOffset = Offset(540f, 1000f),
            onDismiss = onDismiss,
            playbackViewModel = playbackViewModel
        )
    }
}
