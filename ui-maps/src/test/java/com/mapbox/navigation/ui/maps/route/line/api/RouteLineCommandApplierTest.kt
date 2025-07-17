package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.extension.style.expressions.generated.Expression
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

internal class RouteLineCommandApplierTest {

    private val expression = mockk<Expression>(relaxed = true)
    private val style = mockk<Style>(relaxed = true)
    private val layerId = "some-layer-id"

    @Test
    fun applyExpression_LineGradientExpressionApplier() = runBlocking {
        val applier = LineGradientCommandApplier()

        applier.applyCommand(
            style,
            layerId,
            StylePropertyValue(expression, StylePropertyValueKind.EXPRESSION),
        )

        verify { style.setStyleLayerProperty(layerId, "line-gradient", expression) }
    }

    @Test
    fun getProperty_LineGradientExpressionApplier() = runBlocking {
        val applier = LineGradientCommandApplier()

        assertEquals("line-gradient", applier.getProperty())
    }

    @Test
    fun applyExpression_LineTrimExpressionApplier() = runBlocking {
        val applier = LineTrimCommandApplier()

        applier.applyCommand(
            style,
            layerId,
            StylePropertyValue(expression, StylePropertyValueKind.EXPRESSION),
        )

        verify { style.setStyleLayerProperty(layerId, "line-trim-start", expression) }
    }

    @Test
    fun getProperty_LineTrimExpressionApplier() = runBlocking {
        val applier = LineTrimCommandApplier()

        assertEquals("line-trim-start", applier.getProperty())
    }
}
