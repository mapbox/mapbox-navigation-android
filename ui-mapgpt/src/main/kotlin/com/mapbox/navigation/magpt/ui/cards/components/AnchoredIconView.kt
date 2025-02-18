package com.mapbox.navigation.magpt.ui.cards.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mapbox.navigation.mapgpt.core.api.CardComponent

@Composable
fun BoxScope.AnchoredIconView(anchoredIcon: CardComponent.Image, isDarkMode: Boolean) {
    val imageUrl = anchoredIcon.uriDark.takeIf { isDarkMode } ?: anchoredIcon.uriLight
    Box(
        modifier = Modifier
            .offset(y = (-8).dp)
            .height(24.dp)
            .align(Alignment.TopEnd),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(size = 6.dp)),
        )
    }
}
