package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

fun ContinuousRoundedRectangle(radius: Dp): Shape = GenericShape { size: Size, _ ->
    // Approximating G2 continuous squircle
    val radiusValue = radius.value * 2.5f // Adjusted for squircle feel
    addRoundRect(
        RoundRect(
            0f, 0f, size.width, size.height,
            CornerRadius(radiusValue, radiusValue)
        )
    )
}
