package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.expressions.generated.Expression
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteSetValueTest {

    @Test
    fun toMutableValue() {
        val primaryRouteSource = mockk<FeatureCollection>()
        val trafficLineExpressionProvider = mockk<RouteLineExpressionProvider>()
        val routeLineExpression = mockk<Expression>()
        val casingLineExpression = mockk<Expression>()
        val altRoute1TrafficExpression = mockk<RouteLineExpressionProvider>()
        val altRoute2TrafficExpression = mockk<RouteLineExpressionProvider>()
        val alternativeRoute1Source = mockk<FeatureCollection>()
        val alternativeRoute2Source = mockk<FeatureCollection>()
        val waypointsSource = mockk<FeatureCollection>()

        val result = RouteSetValue(
            primaryRouteSource,
            trafficLineExpressionProvider,
            routeLineExpression,
            casingLineExpression,
            altRoute1TrafficExpression,
            altRoute2TrafficExpression,
            alternativeRoute1Source,
            alternativeRoute2Source,
            waypointsSource
        ).toMutableValue()

        assertEquals(primaryRouteSource, result.primaryRouteSource)
        assertEquals(trafficLineExpressionProvider, result.trafficLineExpressionProvider)
        assertEquals(routeLineExpression, result.routeLineExpression)
        assertEquals(casingLineExpression, result.casingLineExpression)
        assertEquals(altRoute1TrafficExpression, result.altRoute1TrafficExpression)
        assertEquals(altRoute2TrafficExpression, result.altRoute2TrafficExpression)
        assertEquals(alternativeRoute1Source, result.alternativeRoute1Source)
        assertEquals(alternativeRoute2Source, result.alternativeRoute2Source)
        assertEquals(waypointsSource, result.waypointsSource)
    }

    @Test
    fun toImmutableValue() {
        val primaryRouteSource = mockk<FeatureCollection>()
        val trafficLineExpressionProvider = mockk<RouteLineExpressionProvider>()
        val routeLineExpression = mockk<Expression>()
        val casingLineExpression = mockk<Expression>()
        val altRoute1TrafficExpression = mockk<RouteLineExpressionProvider>()
        val altRoute2TrafficExpression = mockk<RouteLineExpressionProvider>()
        val alternativeRoute1Source = mockk<FeatureCollection>()
        val alternativeRoute2Source = mockk<FeatureCollection>()
        val waypointsSource = mockk<FeatureCollection>()

        val replacedPrimaryRouteSource = mockk<FeatureCollection>()
        val replacedTrafficLineExpressionProvider = mockk<RouteLineExpressionProvider>()
        val replacedRouteLineExpression = mockk<Expression>()
        val replacedCasingLineExpression = mockk<Expression>()
        val replacedAltRoute1TrafficExpression = mockk<RouteLineExpressionProvider>()
        val replacedAltRoute2TrafficExpression = mockk<RouteLineExpressionProvider>()
        val replacedAlternativeRoute1Source = mockk<FeatureCollection>()
        val replacedAlternativeRoute2Source = mockk<FeatureCollection>()
        val replacedWaypointsSource = mockk<FeatureCollection>()
        val immutableResult = RouteSetValue(
            primaryRouteSource,
            trafficLineExpressionProvider,
            routeLineExpression,
            casingLineExpression,
            altRoute1TrafficExpression,
            altRoute2TrafficExpression,
            alternativeRoute1Source,
            alternativeRoute2Source,
            waypointsSource
        ).toMutableValue()
        immutableResult.primaryRouteSource = replacedPrimaryRouteSource
        immutableResult.trafficLineExpressionProvider = replacedTrafficLineExpressionProvider
        immutableResult.routeLineExpression = replacedRouteLineExpression
        immutableResult.casingLineExpression = replacedCasingLineExpression
        immutableResult.altRoute1TrafficExpression = replacedAltRoute1TrafficExpression
        immutableResult.altRoute2TrafficExpression = replacedAltRoute2TrafficExpression
        immutableResult.alternativeRoute1Source = replacedAlternativeRoute1Source
        immutableResult.alternativeRoute2Source = replacedAlternativeRoute2Source
        immutableResult.waypointsSource = replacedWaypointsSource

        val result = immutableResult.toImmutableValue()

        assertEquals(replacedPrimaryRouteSource, result.primaryRouteSource)
        assertEquals(replacedTrafficLineExpressionProvider, result.trafficLineExpressionProvider)
        assertEquals(replacedRouteLineExpression, result.routeLineExpression)
        assertEquals(replacedCasingLineExpression, result.casingLineExpression)
        assertEquals(replacedAltRoute1TrafficExpression, result.altRoute1TrafficExpressionProvider)
        assertEquals(replacedAltRoute2TrafficExpression, result.altRoute2TrafficExpressionProvider)
        assertEquals(replacedAlternativeRoute1Source, result.alternativeRoute1Source)
        assertEquals(replacedAlternativeRoute2Source, result.alternativeRoute2Source)
        assertEquals(replacedWaypointsSource, result.waypointsSource)
    }
}
