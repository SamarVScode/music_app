package org.oxycblt.auxio.ui.ios

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import org.oxycblt.auxio.ui.ios.backdrop.backdrops.rememberCombinedBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.backdrops.rememberLayerBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.backdrops.layerBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.catalog.components.LocalLiquidBottomTabScale
import org.oxycblt.auxio.ui.ios.backdrop.catalog.utils.DampedDragAnimation
import org.oxycblt.auxio.ui.ios.backdrop.catalog.utils.InteractiveHighlight
import org.oxycblt.auxio.ui.ios.backdrop.drawBackdrop
import org.oxycblt.auxio.ui.ios.backdrop.effects.blur
import org.oxycblt.auxio.ui.ios.backdrop.effects.lens
import org.oxycblt.auxio.ui.ios.backdrop.highlight.Highlight
import org.oxycblt.auxio.ui.ios.backdrop.shadow.InnerShadow
import org.oxycblt.auxio.ui.ios.backdrop.shadow.Shadow
import org.oxycblt.auxio.ui.ios.shapes.ContinuousRoundedRectangle

val LocalTabBarBackdropFrozen = compositionLocalOf<() -> Boolean> { { false } }

/**
 * NestedScrollConnection for the floating bottom navigation bar and mini player chrome.
 * Collapses into inline row on downward scroll, expands back on upward scroll.
 */
class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
    private val expandThresholdPx: Float = scrollThresholdPx,
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        if (isInline) {
            isInline = false
            accumulatedScroll = 0f
        }
    }

    fun inline() {
        if (!isInline) {
            isInline = true
            accumulatedScroll = 0f
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val scrollDelta = available.y

        // Ignore sub-pixel micro-jitter
        if (kotlin.math.abs(scrollDelta) < 1f) return Offset.Zero

        // Reset accumulated scroll immediately when direction flips
        if (accumulatedScroll > 0f && scrollDelta < 0f) {
            accumulatedScroll = 0f
        } else if (accumulatedScroll < 0f && scrollDelta > 0f) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += scrollDelta

        // Downward list scroll (finger moving up, scrollDelta < 0) collapses to inline
        if (!isInline) {
            if (accumulatedScroll > 0f) {
                accumulatedScroll = 0f
            }
            if (accumulatedScroll <= -scrollThresholdPx) {
                isInline = true
                accumulatedScroll = 0f
            }
        } else {
            // When collapsed, cap negative accumulation so no debt builds up
            if (accumulatedScroll < 0f) {
                accumulatedScroll = 0f
            }
            // Upward list scroll (finger moving down, scrollDelta > 0) expands to full
            if (accumulatedScroll >= expandThresholdPx) {
                isInline = false
                accumulatedScroll = 0f
            }
        }

        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // When at the top of the list (e.g. overscroll / unconsumed upward scroll where available.y > 0), expand immediately
        if (available.y > 1f && isInline) {
            expand()
        }
        return Offset.Zero
    }
}

/**
 * Creates and remembers a [FloatingTabBarScrollConnection].
 */
@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 60.dp,
    expandThreshold: Dp = 50.dp,
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    val expandThresholdPx = expandThreshold.toPx()
    remember(scrollThresholdPx, expandThresholdPx, initialIsInline) {
        FloatingTabBarScrollConnection(
            initialIsInline,
            scrollThresholdPx,
            expandThresholdPx,
        )
    }
}

/**
 * iOS 26 style floating bottom navigation chrome with standalone circular search button,
 * authentic Cupertino liquid glass active nav puck, and nested scroll inline collapse.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppFloatingNavBar(
    tabs: List<String> = listOf("Library", "Settings"),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSearchButtonPositioned: ((Rect) -> Unit)? = null,
    scrollConnection: FloatingTabBarScrollConnection = rememberFloatingTabBarScrollConnection(),
    showPlayerAccessory: Boolean = false,
    onPlayerClick: () -> Unit = {},
    tabPosition: (() -> Float?)? = null,
    isFrozen: (() -> Boolean)? = null,
    modifier: Modifier = Modifier,
) {
    val navTransition = updateTransition(
        targetState = scrollConnection.isInline && showPlayerAccessory,
        label = "floatingNavBarTransition"
    )

    val morphBoundsTransform = BoundsTransform { _, _ ->
        tween(durationMillis = 320, easing = FastOutSlowInEasing)
    }

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalTabBarBackdropFrozen provides {
                navTransition.currentState != navTransition.targetState || (isFrozen?.invoke() == true)
            }
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                navTransition.AnimatedContent(
                    transitionSpec = {
                        fadeIn(tween(320, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(240, easing = FastOutSlowInEasing))
                    },
                    contentAlignment = Alignment.BottomCenter
                ) { isInline ->
                if (isInline) {
                    // Collapsed Single Unified Row: [Active Tab Icon] [Compact Inline MiniPlayer] [Search Circle]
                    Row(
                        modifier = Modifier
                            .graphicsLayer()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { scrollConnection.expand() }
                            )
                            .pointerInput(scrollConnection) {
                                var totalDragY = 0f
                                val expandThreshold = 15.dp.toPx()
                                detectVerticalDragGestures(
                                    onDragStart = { totalDragY = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        totalDragY += dragAmount
                                        if (totalDragY < -expandThreshold) {
                                            change.consume()
                                            scrollConnection.expand()
                                        }
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeIcon = when (selectedTab) {
                            0 -> Icons.Filled.LibraryMusic
                            1 -> Icons.Filled.Settings
                            else -> Icons.Filled.MusicNote
                        }
                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "inlineTab"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = morphBoundsTransform,
                                    zIndexInOverlay = 1f
                                )
                                .size(56.dp)
                                .crystalGlass(
                                    shape = CircleShape,
                                    highlightAlpha = 0.5f,
                                    washAlpha = 0.035f,
                                    blurRadiusDp = 2.5f,
                                    frozen = LocalTabBarBackdropFrozen.current
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { scrollConnection.expand() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeIcon,
                                contentDescription = tabs.getOrNull(selectedTab) ?: "Active Tab",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FloatingMiniPlayer(
                            isInline = true,
                            contentColor = Color.White,
                            onClick = onPlayerClick,
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "accessory"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = morphBoundsTransform,
                                    zIndexInOverlay = 1f
                                )
                                .weight(1f)
                                .height(54.dp)
                        )

                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "standaloneTab"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = morphBoundsTransform,
                                    zIndexInOverlay = 1f
                                )
                                .size(56.dp)
                                .onGloballyPositioned { coords ->
                                    if (coords.isAttached) {
                                        onSearchButtonPositioned?.invoke(coords.boundsInRoot())
                                    }
                                }
                                .crystalGlass(
                                    shape = CircleShape,
                                    highlightAlpha = 0.5f,
                                    washAlpha = 0.035f,
                                    blurRadiusDp = 2.5f,
                                    frozen = LocalTabBarBackdropFrozen.current
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onSearchClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    // Expanded Stacked Layout: [Wide MiniPlayer Pill] above [Nav Bar Pill] + [Search Circle]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer()
                            .fillMaxWidth()
                    ) {
                        if (showPlayerAccessory) {
                            FloatingMiniPlayer(
                                isInline = false,
                                contentColor = Color.White,
                                onClick = onPlayerClick,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "accessory"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = morphBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavPill(
                                tabs = tabs,
                                selectedTab = selectedTab,
                                onTabSelected = onTabSelected,
                                tabPosition = tabPosition,
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "inlineTab"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = morphBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .weight(1f)
                            )

                            // Standalone Teardrop Search Button
                            Box(
                                modifier = Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "standaloneTab"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = morphBoundsTransform,
                                        zIndexInOverlay = 1f
                                    )
                                    .size(56.dp)
                                    .onGloballyPositioned { coords ->
                                        if (coords.isAttached) {
                                            onSearchButtonPositioned?.invoke(coords.boundsInRoot())
                                        }
                                    }
                                    .crystalGlass(
                                        shape = CircleShape,
                                        highlightAlpha = 0.5f,
                                        washAlpha = 0.035f,
                                        blurRadiusDp = 2.5f,
                                        frozen = LocalTabBarBackdropFrozen.current
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onSearchClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

/**
 * Authentic Cupertino liquid glass navigation pill containing tabs and the animated puck.
 */
@Composable
private fun NavPill(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabPosition: (() -> Float?)?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val tabsCount = tabs.size.coerceAtLeast(1)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    BoxWithConstraints(
        modifier = modifier
            .height(64.dp)
            .liquidGlass(
                config = LocalGlassEffectConfig.current,
                shape = ContinuousRoundedRectangle(percent = 50),
                highlightAlpha = 0.35f,
                frozen = LocalTabBarBackdropFrozen.current
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val spacerWidthDp = 16.dp
        val spacerWidthPx = with(density) { spacerWidthDp.toPx() }
        val tabSlotWidthPx: Float = remember(constraints.maxWidth, density, tabsCount) {
            with(density) { (constraints.maxWidth.toFloat() - 12.dp.toPx() - (tabsCount - 1) * spacerWidthPx) / tabsCount }
        }
        val tabSlotWidthDp: Dp = remember(tabSlotWidthPx, density) {
            with(density) { tabSlotWidthPx.toDp() }
        }
        val puckMarginHorizontal = 6.dp
        val puckWidthDp = tabSlotWidthDp - (puckMarginHorizontal * 2)
        val puckInsetPx = with(density) { puckMarginHorizontal.toPx() }

        val dampedDragAnimation = remember(coroutineScope, tabsCount) {
            DampedDragAnimation(
                animationScope = coroutineScope,
                initialValue = selectedTab.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.12f,
                velocityDampingRatio = 1.0f,
                stiffness = 300f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    animateToValue(targetIndex.toFloat())
                    onTabSelected(targetIndex)
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / (tabSlotWidthPx + spacerWidthPx) * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                }
            )
        }

        val puckAnimatable = remember { Animatable(selectedTab.toFloat()) }

        // Live fractional pager tracking
        if (tabPosition != null) {
            LaunchedEffect(dampedDragAnimation, tabPosition) {
                snapshotFlow { tabPosition() }.collect { position ->
                    if (position != null) {
                        puckAnimatable.snapTo(position)
                        dampedDragAnimation.snapValue(position)
                    }
                }
            }
        }

        // Selection change sync with subtle iOS spring bounce
        LaunchedEffect(selectedTab) {
            dampedDragAnimation.animateToValue(selectedTab.toFloat())
            puckAnimatable.animateTo(
                targetValue = selectedTab.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        val currentPuckPos = if (dampedDragAnimation.pressProgress > 0.01f) {
            dampedDragAnimation.value
        } else {
            puckAnimatable.value
        }

        val interactiveHighlight = remember(coroutineScope, density, tabSlotWidthPx, spacerWidthPx, isLtr) {
            InteractiveHighlight(
                animationScope = coroutineScope,
                position = { size, _ ->
                    val offset6Px = with(density) { 6.dp.toPx() }
                    Offset(
                        if (isLtr) (currentPuckPos + 0.5f) * (tabSlotWidthPx + spacerWidthPx) + offset6Px
                        else size.width - (currentPuckPos + 0.5f) * (tabSlotWidthPx + spacerWidthPx) - offset6Px,
                        size.height / 2f
                    )
                }
            )
        }

        val puckIndex by remember(tabsCount) {
            derivedStateOf {
                currentPuckPos
                    .fastRoundToInt()
                    .fastCoerceIn(0, tabsCount - 1)
            }
        }

        val puckShape = remember { RoundedCornerShape(percent = 50) }
        val pressProgress = dampedDragAnimation.pressProgress

        // 1. Authentic Cupertino Glowing Frosted Capsule (Liquid Glass Active Nav Puck)
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .graphicsLayer {
                    translationX = (if (isLtr) {
                        currentPuckPos * (tabSlotWidthPx + spacerWidthPx)
                    } else {
                        size.width - (currentPuckPos + 1f) * (tabSlotWidthPx + spacerWidthPx)
                    }) + puckInsetPx
                    scaleX = dampedDragAnimation.scaleX
                    scaleY = dampedDragAnimation.scaleY
                    val velocity = dampedDragAnimation.velocity / 10f
                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                }
                .then(interactiveHighlight.gestureModifier)
                .then(interactiveHighlight.modifier)
                .then(dampedDragAnimation.modifier)
                .width(puckWidthDp)
                .fillMaxHeight()
                .padding(vertical = 5.dp)
                .crystalGlass(
                    shape = RoundedCornerShape(percent = 50),
                    highlightAlpha = 0.5f,
                    washAlpha = 0.05f,
                    blurRadiusDp = 2.5f,
                    frozen = { false }
                )
        )

        // 3. Tab labels & icons in foreground
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                if (index > 0) {
                    Spacer(Modifier.width(spacerWidthDp))
                }
                val isSelected = index == puckIndex
                val icon = when (index) {
                    0 -> Icons.Filled.LibraryMusic
                    else -> Icons.Filled.Settings
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(index)
                            coroutineScope.launch {
                                puckAnimatable.animateTo(
                                    targetValue = index.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                            dampedDragAnimation.animateToValue(index.toFloat())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

    }
}
