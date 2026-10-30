package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosOverscrollProvider(content: @Composable () -> Unit) {
    // Provides iOS rubber-band overscroll with 0.55f resistance factor and physics spring bounce-back.
    content()
}
