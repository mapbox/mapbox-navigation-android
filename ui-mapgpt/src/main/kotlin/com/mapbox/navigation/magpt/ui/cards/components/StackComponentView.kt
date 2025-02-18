package com.mapbox.navigation.magpt.ui.cards.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.navigation.magpt.ui.cards.CardComponentRowView
import com.mapbox.navigation.magpt.ui.cards.CardOptions
import com.mapbox.navigation.mapgpt.core.api.CardComponent

@Composable
internal fun StackComponentView(component: CardComponent.Stack, configuration: CardOptions) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 3.dp),
    ) {
        val modifier = Modifier.weight(1f)
        CardComponentRowView(
            components = component.topRow ?: listOf(),
            configuration = configuration,
            modifier = modifier,
        )
        CardComponentRowView(
            components = component.bottomRow ?: listOf(),
            configuration = configuration,
            modifier = modifier,
        )
    }
}
