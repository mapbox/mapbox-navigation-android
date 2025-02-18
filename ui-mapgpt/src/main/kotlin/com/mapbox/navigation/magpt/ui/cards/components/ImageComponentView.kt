package com.mapbox.navigation.magpt.ui.cards.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mapbox.navigation.mapgpt.core.api.CardComponent

@Composable
internal fun ImageComponentView(
    component: CardComponent.Image,
    isDarkMode: Boolean,
) {
    val defaultCornerRadius = 8.dp
    val conditionalAspectRatioModifier = component.aspectRatio?.let { Modifier.aspectRatio(it) }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(size = defaultCornerRadius))
            .then(conditionalAspectRatioModifier ?: Modifier),
    ) {
        val imageUri = resolveTheme(component, isDarkMode)
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(size = defaultCornerRadius)),
        )
    }
}

@Composable
private fun resolveTheme(image: CardComponent.Image, isDarkMode: Boolean): String {
    val uriDark = image.uriDark
    if (uriDark != null && isDarkMode) {
        return uriDark
    }
    return image.uriLight
}
