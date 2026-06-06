package com.bbg.cloudapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bbg.cloudapp.ui.theme.StorageAmberColor
import com.bbg.cloudapp.ui.theme.StorageGreenColor
import com.bbg.cloudapp.ui.theme.StorageRedColor

@Composable
fun StorageProgressBar(
    percent: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = Color.Black.copy(alpha = 0.1f)
) {
    val progressColor = when {
        percent < 0.60f -> StorageGreenColor
        percent < 0.85f -> StorageAmberColor
        else -> StorageRedColor
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percent.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(progressColor)
        )
    }
}
