/*
 * Copyright (c) 2026 Auxio Project
 * GenresTab.kt is part of Auxio.
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.music.resolve

@Composable
fun GenresTab(
    homeViewModel: HomeViewModel = hiltViewModel(),
    detailViewModel: DetailViewModel = hiltViewModel()
) {
    val genreList by homeViewModel.genreList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(genreList) { genre ->
            GenreCard(
                name = genre.name.resolve(context),
                count = "${genre.songs.size} tracks",
                onClick = { detailViewModel.showGenre(genre) }
            )
        }
    }
}

@Composable
fun GenreCard(name: String, count: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Text(text = count, fontSize = 14.sp, color = Color.White.copy(alpha = 0.65f))
        }
    }
}
