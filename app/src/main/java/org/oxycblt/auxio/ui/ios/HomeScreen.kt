/*
 * Copyright (c) 2026 Auxio Project
 * HomeScreen.kt is part of Auxio.
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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.music.IndexingState
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.oxycblt.auxio.music.MusicSettings

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeScreenEntryPoint {
    fun musicSettings(): MusicSettings
}

enum class HomeCategory { Songs, Albums, Artists, Genres, Playlists }

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    var selectedCategory by remember { mutableStateOf(HomeCategory.Songs) }
    val statistics by musicViewModel.statistics.collectAsState()
    val indexingState by musicViewModel.indexingState.collectAsState()
    
    val context = LocalContext.current
    val musicSettings = remember {
        EntryPointAccessors.fromApplication(context, HomeScreenEntryPoint::class.java).musicSettings()
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            val unopened = org.oxycblt.musikr.fs.Location.Unopened.from(context, uri)
            val opened = unopened?.open(context)
            if (opened != null) {
                musicSettings.safQuery = musicSettings.safQuery.copy(source = listOf(opened))
                musicSettings.locationMode = org.oxycblt.auxio.music.locations.LocationMode.SAF
                musicViewModel.rescan()
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (statistics == null || statistics?.songs == 0) {
            // Empty State
            Card(
                modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No Music Found", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select a folder containing your music library.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            launcher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Select Music Folder (SAF)", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            musicSettings.locationMode = org.oxycblt.auxio.music.locations.LocationMode.MEDIA_STORE
                            musicViewModel.rescan()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Scan Device Music (MediaStore)", color = Color.Black)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                    selectedTabIndex = selectedCategory.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}
                ) {
                    HomeCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            text = {
                                Text(
                                    text = category.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .background(
                                    color = if (isSelected) Color.Black else Color.LightGray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .height(32.dp)
                        )
                    }
                }
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedCategory) {
                        HomeCategory.Songs -> SongsTab()
                        HomeCategory.Albums -> AlbumsTab()
                        HomeCategory.Artists -> ArtistsTab()
                        HomeCategory.Genres -> GenresTab()
                        HomeCategory.Playlists -> PlaylistsTab()
                    }
                }
            }
        }
        
        // Scanning Progress Pill
        if (indexingState is IndexingState.Indexing) {
            val progress = (indexingState as IndexingState.Indexing).progress
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val loaded = if (progress is org.oxycblt.musikr.IndexingProgress.Songs) progress.loaded else 0
                    val explored = if (progress is org.oxycblt.musikr.IndexingProgress.Songs) progress.explored else 0
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning... $loaded/$explored", fontSize = 14.sp, color = Color.Black)
                }
            }
        }
    }
}
