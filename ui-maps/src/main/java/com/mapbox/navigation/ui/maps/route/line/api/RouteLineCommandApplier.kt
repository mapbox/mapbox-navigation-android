package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.MainThread
import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.extension.style.expressions.generated.Expression

internal abstract class RouteLineCommandApplier<T> {

    @MainThread
    abstract fun applyCommand(
        style: Style,
        layerId: String,
        command: T,
    )

    abstract fun getProperty(): String
}

internal class LineGradientCommandApplier : RouteLineCommandApplier<StylePropertyValue>() {

    @MainThread
    override fun applyCommand(
        style: Style,
        layerId: String,
        command: StylePropertyValue,
    ) {
        style.setStyleLayerProperty(layerId, getProperty(), command.value)
    }

    override fun getProperty(): String {
        return "line-gradient"
    }
}

/**
 * Represents a function that returns an [Expression]. The expression this provider is expected
 * to produce is a lineTrimOffset expression. This is a specific type of expression that will
 * make a line transparent between two values representing sections of a line.
 *
 * For example a call like literal(listOf(0.0, 0.5)) would produce a trim offset expression
 * that made a line transparent from the beginning of the line to the midpoint of the line. In other
 * words the first 50% of the line would be transparent.  A call like literal(listOf(0.25, 0.5))
 * would make the line transparent starting at 25% of the line's length to 50% of the line's length.
 * The line's color would be represented in the other sections of the line.  See the Map API documentation
 * regarding lineTrimOffset for more information.
 */
internal class LineTrimCommandApplier : RouteLineCommandApplier<StylePropertyValue>() {

    @MainThread
    override fun applyCommand(
        style: Style,
        layerId: String,
        command: StylePropertyValue,
    ) {
        style.setStyleLayerProperty(
            layerId,
            getProperty(),
            command.value,
        )
    }

    override fun getProperty(): String {
        return "line-trim-end"
    }
}
