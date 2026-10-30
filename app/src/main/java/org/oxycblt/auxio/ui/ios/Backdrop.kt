/*
 * Copyright (c) 2026 Auxio Project
 * Backdrop.kt is part of Auxio.
 */
package org.oxycblt.auxio.ui.ios

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.oxycblt.auxio.ui.ios.backdrop.Backdrop

@Stable
class SharedLiquidBackdropState(
    val rawLayer: GraphicsLayer,
    val blurredAtlasLayer: GraphicsLayer,
    val scaleFactor: Float = 0.25f
) : Backdrop {
    override val isCoordinatesDependent: Boolean = true
    var isDirty: Boolean = true
    var contentVersion: Int by mutableIntStateOf(0)
    private var frameParity: Int = 0
    private var wasScrollingOrAnimating: Boolean = false
    var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    fun markDirty() {
        isDirty = true
        contentVersion++
    }

    fun shouldUpdateAtlas(isScrollingOrAnimating: Boolean, isModalFrozen: Boolean): Boolean {
        if (isModalFrozen) return false
        if (wasScrollingOrAnimating && !isScrollingOrAnimating) {
            wasScrollingOrAnimating = false
            isDirty = true // Force one final settle update when scrolling stops
        }
        if (!isDirty && !isScrollingOrAnimating) return false
        if (isScrollingOrAnimating) {
            wasScrollingOrAnimating = true
            frameParity = (frameParity + 1) and 1
            if (frameParity == 0) {
                contentVersion++
                return true
            }
            return false
        }
        isDirty = false
        contentVersion++
        return true
    }

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val coords = coordinates ?: return
        val layerCoords = layerCoordinates ?: return
        val offset = try {
            layerCoords.localPositionOf(coords)
        } catch (_: Exception) {
            coords.positionInWindow() - layerCoords.positionInWindow()
        }
        translate(-offset.x, -offset.y) {
            drawLayer(rawLayer)
        }
    }
}

@Composable
fun rememberSharedLiquidBackdropState(scaleFactor: Float = 0.25f): SharedLiquidBackdropState {
    val rawLayer = rememberGraphicsLayer()
    val blurredAtlasLayer = rememberGraphicsLayer()
    return remember(rawLayer, blurredAtlasLayer, scaleFactor) {
        SharedLiquidBackdropState(rawLayer, blurredAtlasLayer, scaleFactor)
    }
}

fun Modifier.recordSharedBackdrop(
    state: SharedLiquidBackdropState,
    blurRadiusDp: Dp = 32.dp,
    isScrolling: () -> Boolean = { false },
    isModalFrozen: () -> Boolean = { false }
): Modifier = this
    .onGloballyPositioned { coords ->
        if (coords.isAttached) {
            state.layerCoordinates = coords
        }
    }
    .drawWithCache {
        val scale = state.scaleFactor
        val scaledW = (size.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (size.height * scale).toInt().coerceAtLeast(1)
        val scaledBlurPx = (blurRadiusDp.toPx() * scale).coerceAtLeast(1f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            state.blurredAtlasLayer.renderEffect = RenderEffect
                .createBlurEffect(scaledBlurPx, scaledBlurPx, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }

        onDrawWithContent {
            // 1. Record sharp 1.0x content
            state.rawLayer.record(IntSize(size.width.toInt(), size.height.toInt())) {
                this@onDrawWithContent.drawContent()
            }
            // 2. Render sharp content to screen at 120Hz
            drawLayer(state.rawLayer)

            // 3. Single shared 0.25x blur pass (only when dirty/scrolling)
            if (state.shouldUpdateAtlas(isScrolling(), isModalFrozen())) {
                state.blurredAtlasLayer.record(IntSize(scaledW, scaledH)) {
                    scale(scale, scale, pivot = Offset.Zero) {
                        drawLayer(state.rawLayer)
                    }
                }
            }
        }
    }

@Composable
fun BackdropProvider(
    scaleFactor: Float = 0.25f,
    content: @Composable (appBackdrop: SharedLiquidBackdropState) -> Unit
) {
    val appBackdrop = rememberSharedLiquidBackdropState(scaleFactor = scaleFactor)

    CompositionLocalProvider(
        LocalAppBackdrop provides appBackdrop,
        LocalGlassEffectConfig provides GlassEffectConfig()
    ) {
        content(appBackdrop)
    }
}

/**
 * Modifier that captures the background content into [appBackdrop].
 * Apply ONLY to background scrollable screen content, NEVER to floating glass overlays!
 */
fun Modifier.appBackdropContent(
    appBackdrop: SharedLiquidBackdropState,
    blurRadiusDp: Dp = 32.dp,
    isScrolling: () -> Boolean = { false },
    isModalFrozen: () -> Boolean = { false }
): Modifier = this.recordSharedBackdrop(
    state = appBackdrop,
    blurRadiusDp = blurRadiusDp,
    isScrolling = isScrolling,
    isModalFrozen = isModalFrozen
)

@Composable
fun Backdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BackdropProvider { appBackdrop ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .appBackdropContent(appBackdrop)
        ) {
            content()
        }
    }
}
