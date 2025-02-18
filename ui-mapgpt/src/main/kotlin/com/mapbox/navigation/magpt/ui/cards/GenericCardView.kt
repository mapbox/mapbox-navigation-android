package com.mapbox.navigation.magpt.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.navigation.magpt.ui.MapGPTColor
import com.mapbox.navigation.magpt.ui.cards.components.AnchoredIconView
import com.mapbox.navigation.magpt.ui.cards.components.ImageComponentView
import com.mapbox.navigation.magpt.ui.cards.components.StackComponentView
import com.mapbox.navigation.magpt.ui.cards.components.TextComponentView
import com.mapbox.navigation.mapgpt.core.api.CardComponent
import com.mapbox.navigation.mapgpt.core.api.SessionFrame.SendEvent.Body.Entity.Data.Card

data class CardOptions(
    val isDarkMode: Boolean,
    val isTablet: Boolean,
    val enableAnchoredIcon: Boolean,
)

@Composable
fun GenericCardView(card: Card, configuration: CardOptions, modifier: Modifier = Modifier) {
    Box {
        CardComponentRowView(card.components, configuration, modifier)
        val anchoredIcon = card.anchoredComponent
        if (configuration.enableAnchoredIcon && anchoredIcon is CardComponent.Image) {
            AnchoredIconView(anchoredIcon, configuration.isDarkMode)
        }
    }
}

@Composable
internal fun CardComponentRowView(
    components: List<CardComponent>,
    configuration: CardOptions,
    modifier: Modifier = Modifier,
) {
    if (components.isSplitText()) {
        SplitTextRowView(
            startText = components[0] as CardComponent.Text,
            endText = components[2] as CardComponent.Text,
            configuration = configuration,
            modifier = modifier,
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        for (component in components) {
            when (component) {
                is CardComponent.Text -> {
                    TextComponentView(component, configuration)
                }

                is CardComponent.Image -> {
                    ImageComponentView(component, configuration.isDarkMode)
                }

                is CardComponent.Stack -> {
                    StackComponentView(component, configuration)
                }

                is CardComponent.Spacer -> {
                    Spacer(modifier = Modifier.weight(1F))
                }
            }
        }
    }
}

internal fun colorForCurrentSystemTheme(color: MapGPTColor, isDarkMode: Boolean): Color {
    return if (isDarkMode) {
        color.dark
    } else {
        color.light
    }
}
