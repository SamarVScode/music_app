/*
 * Copyright (c) 2026 Auxio Project
 * AuxioApp.kt is part of Auxio.
 */
package org.oxycblt.auxio.ui.ios

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.playback.queue.QueueViewModel
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.search.SearchViewModel
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.ui.ios.backdrop.backdrops.rememberBackdropFreeze

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuxioAppEntryPoint {
    fun imageLoader(): coil3.ImageLoader
}

enum class Tab { Library, Settings }

@Composable
fun AuxioApp(
    homeViewModel: HomeViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
    detailViewModel: DetailViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        try {
            EntryPointAccessors.fromApplication(context.applicationContext, AuxioAppEntryPoint::class.java).imageLoader()
        } catch (e: Exception) {
            null
        }
    }
    LaunchedEffect(imageLoader) {
        imageLoader?.let {
            try {
                coil3.SingletonImageLoader.setUnsafe(it)
            } catch (e: Exception) {}
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { Tab.values().size })

    var playerSheetOpen by remember { mutableStateOf(false) }
    var detailOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var contextMenuSong by remember { mutableStateOf<Pair<org.oxycblt.musikr.Song, androidx.compose.ui.geometry.Offset>?>(null) }

    val currentSong by playbackViewModel.song.collectAsStateWithLifecycle()
    val song = currentSong

    val scrollConnection = rememberFloatingTabBarScrollConnection(
        scrollThreshold = 60.dp,
        expandThreshold = 50.dp
    )
    val backdropFreeze = rememberBackdropFreeze()
    val libraryListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            libraryListState.firstVisibleItemIndex > 0 || libraryListState.firstVisibleItemScrollOffset > 120
        }
    }
    val pillHeight by animateDpAsState(
        targetValue = if (isScrolled) 44.dp else 72.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "pillHeight"
    )

    val isAppScrolling = remember(libraryListState, pagerState) {
        { libraryListState.isScrollInProgress || pagerState.isScrollInProgress }
    }
    val isModalFrozen = remember {
        { false }
    }
    val isAppContentFrozen = remember {
        { false }
    }
    val searchProgress by animateFloatAsState(
        targetValue = if (searchOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessLow
        ),
        label = "searchProgress"
    )
    var searchButtonBounds by remember { mutableStateOf(Rect.Zero) }

    // Android back button hierarchy
    BackHandler(enabled = contextMenuSong != null) {
        contextMenuSong = null
    }
    BackHandler(enabled = playerSheetOpen) {
        playerSheetOpen = false
    }
    BackHandler(enabled = searchOpen) {
        searchOpen = false
    }
    BackHandler(enabled = detailOpen) {
        detailOpen = false
    }
    BackHandler(enabled = !playerSheetOpen && !searchOpen && !detailOpen && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = spring(dampingRatio = 1.0f, stiffness = 300f)
            )
        }
    }

    val appleDarkScheme = darkColorScheme(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceContainer = Color(0xFF1C1C1E),
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0x99FFFFFF),
        primary = Color.White,
        onPrimary = Color.Black
    )

    MaterialTheme(colorScheme = appleDarkScheme) {
        BackdropProvider { appBackdrop ->
            CompositionLocalProvider(
                LocalAppBackdrop provides appBackdrop,
                LocalSongContextMenu provides { menuSong, menuOffset -> contextMenuSong = menuSong to menuOffset }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // 1. Content Layer (sampled by glass overlays, sibling to overlays)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .appBackdropContent(
                                appBackdrop = appBackdrop,
                                isScrolling = isAppScrolling,
                                isModalFrozen = isModalFrozen
                            )
                            .background(Color.Black)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollConnection)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (Tab.values()[page]) {
                                    Tab.Library -> {
                                        if (detailOpen) {
                                            DetailScreen(
                                                title = "Album",
                                                subtitle = "Artist",
                                                onBack = { detailOpen = false }
                                            )
                                        } else {
                                            LibraryScreen(listState = libraryListState)
                                        }
                                    }
                                    Tab.Settings -> SettingsScreen()
                                }
                            }
                        }
                    }

                    // Floating Header Pill (outside recorded HorizontalPager, floats over content)
                    if (pagerState.currentPage == 0 && !detailOpen) {
                        LibraryHeader(
                            isScrolled = isScrolled,
                            pillHeight = pillHeight,
                            onRefresh = { musicViewModel.rescan() },
                            modifier = Modifier.align(Alignment.TopCenter),
                            frozen = isAppContentFrozen
                        )
                    }

                // 2. Floating Bottom Navigation Chrome
                AppFloatingNavBar(
                    tabs = Tab.values().map { it.name },
                    selectedTab = pagerState.currentPage,
                    onTabSelected = { targetPage ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
                            )
                        }
                    },
                    onSearchClick = { searchOpen = true },
                    onSearchButtonPositioned = { searchButtonBounds = it },
                    scrollConnection = scrollConnection,
                    showPlayerAccessory = song != null,
                    onPlayerClick = { playerSheetOpen = true },
                    tabPosition = { if (pagerState.isScrollInProgress) pagerState.currentPage + pagerState.currentPageOffsetFraction else null },
                    isFrozen = isAppContentFrozen,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                )

                // 3. Standalone Search Screen Overlay
                if (searchOpen || searchProgress > 0.001f) {
                    val searchBottomPadding = if (song != null) 160.dp else 88.dp
                    SearchScreen(
                        onBack = { searchOpen = false },
                        playbackViewModel = playbackViewModel,
                        searchViewModel = searchViewModel,
                        searchProgress = searchProgress,
                        bottomPadding = searchBottomPadding,
                        searchButtonBounds = searchButtonBounds
                    )
                }

                // 4. Full Player Sheet Overlay
                AnimatedVisibility(
                    visible = playerSheetOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessLow)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessLow)
                    )
                ) {
                    PlayerV2(
                        playbackViewModel = playbackViewModel,
                        queueViewModel = queueViewModel,
                        onClose = { playerSheetOpen = false }
                    )
                }

                // 5. Floating Song Context Menu Overlay (outside recorded content layer)
                contextMenuSong?.let { (menuSong, menuOffset) ->
                    SongFloatingContextMenu(
                        song = menuSong,
                        touchOffset = menuOffset,
                        onDismiss = { contextMenuSong = null }
                    )
                }
                }
            }
        }
    }
}
