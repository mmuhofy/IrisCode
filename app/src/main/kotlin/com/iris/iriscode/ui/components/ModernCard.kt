package com.iris.iriscode.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.iris.iriscode.ui.theme.IrisPrimary
import com.iris.iriscode.ui.theme.IrisSurfaceContainer

private val CardGradient = Brush.horizontalGradient(
    colors = listOf(
        IrisPrimary.copy(alpha = 0.04f),
        IrisSurfaceContainer,
        IrisSurfaceContainer
    ),
    startX = 0f,
    endX = 400f
)

@Composable
fun ModernCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentContent: @Composable () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(CardGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(start = 0.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentBar()
        Spacer(modifier = Modifier.width(14.dp))
        accentContent()
    }
}

@Composable
private fun AccentBar() {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(IrisPrimary)
    )
}
