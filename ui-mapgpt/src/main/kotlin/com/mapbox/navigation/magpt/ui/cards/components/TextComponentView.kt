package com.mapbox.navigation.magpt.ui.cards.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mapbox.navigation.magpt.ui.MapGPTColor
import com.mapbox.navigation.magpt.ui.MapGPTTypography
import com.mapbox.navigation.magpt.ui.cards.CardOptions
import com.mapbox.navigation.magpt.ui.cards.colorForCurrentSystemTheme
import com.mapbox.navigation.mapgpt.core.api.CardComponent

@Composable
internal fun TextComponentView(
    component: CardComponent.Text,
    configuration: CardOptions,
    modifier: Modifier = Modifier,
) {
    Text(
        text = component.content,
        modifier = modifier,
        color = colorForCurrentSystemTheme(
            component.fontColor.getDesignSystemColor(),
            configuration.isDarkMode,
        ),
        style = component.fontSize.getTypography().toTextStyle(configuration.isTablet),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun String?.getDesignSystemColor(): MapGPTColor {
    return when (this) {
        "primary" -> {
            MapGPTColor.TextPrimary
        }

        "secondary" -> {
            MapGPTColor.TextSecondary
        }

        else -> {
            MapGPTColor.TextPrimary
        }
    }
}

private fun String?.getTypography(): MapGPTTypography {
    return when (this) {
        "title3" -> {
            MapGPTTypography.Title3
        }

        "title5" -> {
            MapGPTTypography.Title5
        }

        "title7" -> {
            MapGPTTypography.Title7
        }

        "body5" -> {
            MapGPTTypography.Body5
        }

        else -> {
            MapGPTTypography.Body5
        }
    }
}
