/*
 * Copyright (c) 2026 Auxio Project
 * SongsTab.kt is part of Auxio.
 */
package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.io.File
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.musikr.Song

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsTab(
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val songList by homeViewModel.songList.collectAsStateWithLifecycle()
    val playWith = homeViewModel.playWith
    val context = LocalContext.current

    val openContextMenu = LocalSongContextMenu.current
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var pressOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp, top = 144.dp, bottom = 148.dp)
        ) {
            items(
                items = songList,
                key = { it.uri.toString() }
            ) { song ->
                var rowPositionInRoot by remember { mutableStateOf(Offset.Zero) }
                var lastTouchOffset by remember { mutableStateOf(Offset.Zero) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                rowPositionInRoot = coords.positionInRoot()
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                lastTouchOffset = down.position
                            }
                        }
                        .combinedClickable(
                            onClick = { playbackViewModel.play(song, playWith) },
                            onLongClick = {
                                val touchOffset = if (lastTouchOffset != Offset.Zero) {
                                    rowPositionInRoot + lastTouchOffset
                                } else {
                                    rowPositionInRoot + Offset(200f, 40f)
                                }
                                if (openContextMenu != null) {
                                    openContextMenu(song, touchOffset)
                                } else {
                                    pressOffsetInRoot = touchOffset
                                    selectedSong = song
                                }
                            }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.name.resolve(context),
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artists.resolveNames(context),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    AsyncImage(
                        model = song.cover,
                        contentDescription = "Cover",
                        fallback = androidx.compose.ui.res.painterResource(id = org.oxycblt.auxio.R.drawable.ic_song_48),
                        placeholder = androidx.compose.ui.res.painterResource(id = org.oxycblt.auxio.R.drawable.ic_song_48),
                        error = androidx.compose.ui.res.painterResource(id = org.oxycblt.auxio.R.drawable.ic_song_48),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }

        // Fast alphabet scroller overlay on right edge
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
        
        if (openContextMenu == null) {
            selectedSong?.let { song ->
                SongFloatingContextMenu(
                    song = song,
                    touchOffset = pressOffsetInRoot,
                    onDismiss = { selectedSong = null }
                )
            }
        }
    }
}
