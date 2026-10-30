/*
 * Copyright (c) 2026 Auxio Project
 * PlaylistsTab.kt is part of Auxio.
 */
package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.music.resolve

@Composable
fun PlaylistsTab(
    homeViewModel: HomeViewModel = hiltViewModel(),
    detailViewModel: DetailViewModel = hiltViewModel()
) {
    val playlistList by homeViewModel.playlistList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable { /* New Playlist */ }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Playlist", tint = Color(0xFFFA2D48))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "New Playlist", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color(0xFFFA2D48))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(playlistList) { playlist ->
            PlaylistListItem(
                name = playlist.name.resolve(context),
                count = "${playlist.songs.size} tracks",
                onClick = { detailViewModel.showPlaylist(playlist) }
            )
        }
    }
}

@Composable
fun PlaylistListItem(name: String, count: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2C2C2E))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.White)
            Text(text = count, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
        }
    }
}
