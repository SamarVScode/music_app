package org.oxycblt.auxio.ui.ios

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.music.IndexingState
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.ui.ios.shapes.ContinuousRoundedRectangle

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
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

    // Fully transparent background — song list scrolls behind glass header
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (statistics == null || statistics?.songs == 0) {
            // Empty State
            Card(
                modifier = Modifier.align(Alignment.Center).padding(32.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No Music Found", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select a folder containing your music library.", color = Color.White.copy(alpha = 0.65f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            launcher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Select Music Folder (SAF)", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            musicSettings.locationMode = org.oxycblt.auxio.music.locations.LocationMode.MEDIA_STORE
                            musicViewModel.rescan()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = Color.White),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Scan Device Music (MediaStore)", color = Color.White)
                    }
                }
            }
        } else {
            SongsTab(
                listState = listState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Scanning Progress Pill
        AnimatedVisibility(
            visible = indexingState is IndexingState.Indexing,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 84.dp)
        ) {
            val progress = (indexingState as? IndexingState.Indexing)?.progress
            val pillShape = RoundedCornerShape(50)
            Surface(
                shape = pillShape,
                color = Color(0xFF1C1C1E).copy(alpha = 0.88f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val loaded = if (progress is org.oxycblt.musikr.IndexingProgress.Songs) progress.loaded else 0
                    val explored = if (progress is org.oxycblt.musikr.IndexingProgress.Songs) progress.explored else 0
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scanning... $loaded/$explored",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryHeader(
    isScrolled: Boolean,
    pillHeight: Dp,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    frozen: () -> Boolean = { false }
) {
    // iOS Cupertino Floating Glass Header
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title + Count Pill — wrapContentWidth, independent frosted glass, animated height
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(pillHeight)
                .liquidGlass(
                    config = LocalGlassEffectConfig.current,
                    shape = ContinuousRoundedRectangle(percent = 50),
                    highlightAlpha = 0.35f,
                    frozen = frozen
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isScrolled,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "headerContent"
            ) { scrolled ->
                if (scrolled) {
                    Text(
                        text = "Songs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Music Library",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Refresh Pill — circular, independent frosted glass
        Box(
            modifier = Modifier
                .size(if (isScrolled) 44.dp else 56.dp)
                .liquidGlass(
                    config = LocalGlassEffectConfig.current,
                    shape = ContinuousRoundedRectangle(percent = 50),
                    highlightAlpha = 0.35f,
                    frozen = frozen
                )
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
