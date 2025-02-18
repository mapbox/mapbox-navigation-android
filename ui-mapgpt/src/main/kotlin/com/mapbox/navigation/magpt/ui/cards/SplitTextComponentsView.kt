package com.mapbox.navigation.magpt.ui.cards

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.navigation.magpt.ui.cards.components.TextComponentView
import com.mapbox.navigation.mapgpt.core.api.CardComponent

/**
 * When a component row contains Text, Spacer, Text; this function will render the row with the
 * priority to show the right text component in full. The left text component will be truncated
 * if necessary.
 */
@Composable
internal fun SplitTextRowView(
    startText: CardComponent.Text,
    endText: CardComponent.Text,
    configuration: CardOptions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextComponentView(
            component = startText,
            configuration = configuration,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        TextComponentView(
            component = endText,
            configuration = configuration,
        )
    }
}

fun List<CardComponent>?.isSplitText(): Boolean {
    val components = this ?: return false
    return size == 3 && components[0] is CardComponent.Text
        && this[1] is CardComponent.Spacer
        && this[2] is CardComponent.Text
}
