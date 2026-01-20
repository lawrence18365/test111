package com.google.jetstream.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    val targetColor = if (isFavorite) Color.Red else Color.White.copy(alpha = 0.8f)
    
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "color"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Icon(
        imageVector = icon,
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        tint = color,
        modifier = modifier
            .scale(scale)
            .size(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onToggle()
                // Simple recoil effect logic could be handled here or via state change
                isPressed = false
            }
            .padding(8.dp)
    )
}
