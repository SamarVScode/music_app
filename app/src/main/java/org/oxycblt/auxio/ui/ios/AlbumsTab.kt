/*
 * Copyright (c) 2026 Auxio Project
 * AlbumsTab.kt is part of Auxio.
 */
package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.io.File
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames

@Composable
fun AlbumsTab(
    homeViewModel: HomeViewModel = hiltViewModel(),
    detailViewModel: DetailViewModel = hiltViewModel()
) {
    val albumList by homeViewModel.albumList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(end = 24.dp, start = 8.dp, top = 8.dp, bottom = 120.dp)) {
            items(albumList) { album ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailViewModel.showAlbum(album) }
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = album.covers.covers.firstOrNull(),
                        contentDescription = "Cover",
                        fallback = androidx.compose.ui.res.painterResource(id = org.oxycblt.auxio.R.drawable.ic_album_48),
                        placeholder = androidx.compose.ui.res.painterResource(id = org.oxycblt.auxio.R.drawable.ic_album_48),
                        error = androidx.compose.ui.res.painterResource(id = org.oxycblt.auxio.R.drawable.ic_album_48),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = album.name.resolve(context), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = album.artists.resolveNames(context), fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    }
}
