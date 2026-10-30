/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop â€” Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package org.oxycblt.auxio.ui.ios.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import org.oxycblt.auxio.ui.ios.backdrop.BackdropEffectScope
import org.oxycblt.auxio.ui.ios.backdrop.RuntimeShader
import org.oxycblt.auxio.ui.ios.backdrop.internal.RuntimeShaderEffect
import org.oxycblt.auxio.ui.ios.backdrop.internal.chain
import org.oxycblt.auxio.ui.ios.backdrop.isRenderEffectSupported
import org.oxycblt.auxio.ui.ios.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}

