package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.coroutines.coroutineContext

internal suspend fun checkAppliedExpression(
    expectedExpression: List<(Value) -> Unit>,
    commandHolder: RouteLineValueCommandHolder,
    viewOptions: RouteLineViewOptionsData,
    property: String,
) {
    checkExpression(
        expectedExpression,
        getAppliedExpression(commandHolder, viewOptions, property),
    )
}

internal suspend fun getAppliedExpression(
    commandHolder: RouteLineValueCommandHolder,
    viewOptions: RouteLineViewOptionsData,
    property: String,
): Expression {
    return getAppliedValue(commandHolder, viewOptions, property) as Expression
}

internal suspend fun getAppliedValue(
    commandHolder: RouteLineValueCommandHolder,
    viewOptions: RouteLineViewOptionsData,
    property: String,
): Value {
    val style = mockk<Style>(relaxed = true)
    val layerId = "some-layer-id"
    val exp = commandHolder.provider.generateCommand(coroutineContext, viewOptions)
    commandHolder.applier.applyCommand(style, layerId, exp)
    val expressionSlot = slot<Value>()
    verify {
        style.setStyleLayerProperty(layerId, property, capture(expressionSlot))
    }
    return expressionSlot.captured
}
