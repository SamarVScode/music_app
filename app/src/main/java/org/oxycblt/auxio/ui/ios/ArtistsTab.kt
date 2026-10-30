/*
 * Copyright (c) 2026 Auxio Project
 * ArtistsTab.kt is part of Auxio.
 */
package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
fun ArtistsTab(
    homeViewModel: HomeViewModel = hiltViewModel(),
    detailViewModel: DetailViewModel = hiltViewModel()
) {
    val artistList by homeViewModel.artistList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 120.dp, end = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(artistList) { artist ->
                ArtistListItem(
                    name = artist.name.resolve(context),
                    details = "${artist.songs.size} songs, ${artist.explicitAlbums.size} albums",
                    onClick = { detailViewModel.showArtist(artist) }
                )
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
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistListItem(name: String, details: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.White)
            Text(text = details, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
        }
    }
}
