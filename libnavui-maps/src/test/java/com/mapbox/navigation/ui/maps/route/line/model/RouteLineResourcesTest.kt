package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.reflect.KClass

class RouteLineResourcesTest : BuilderTest<RouteLineResources, RouteLineResources.Builder>() {

    override fun getImplementationClass(): KClass<RouteLineResources> = RouteLineResources::class

    override fun getFilledUpBuilder(): RouteLineResources.Builder {
        val backFill = listOf<String>("foobar")
        val routeLineExpression = mockk<Expression>()
        val routeCasingExpression = mockk<Expression>()
        val trafficExpression = mockk<Expression>()

        return RouteLineResources.Builder()
            .routeLineTraveledColor(1)
            .routeLineTraveledCasingColor(2)
            .routeUnknownTrafficColor(3)
            .routeDefaultColor(4)
            .routeLowCongestionColor(5)
            .routeModerateColor(6)
            .routeHeavyColor(7)
            .routeSevereColor(8)
            .routeCasingColor(9)
            .roundedLineCap(false)
            .alternativeRouteUnknownTrafficColor(10)
            .alternativeRouteDefaultColor(11)
            .alternativeRouteLowColor(12)
            .alternativeRouteModerateColor(13)
            .alternativeRouteHeavyColor(14)
            .alternativeRouteSevereColor(15)
            .alternativeRouteCasingColor(16)
            .originWaypointIcon(17)
            .destinationWaypointIcon(18)
            .trafficBackfillRoadClasses(backFill)
            .routeLineScaleExpression(routeLineExpression)
            .routeCasingLineScaleExpression(routeCasingExpression)
            .routeTrafficLineScaleExpression(trafficExpression)
    }

    override fun trigger() {
        //
    }

    @Test
    fun withRouteLineTraveledColor() {
        val resources = RouteLineResources.Builder().routeLineTraveledColor(5).build()

        assertEquals(5, resources.routeLineTraveledColor)
    }

    @Test
    fun withRouteLineTraveledCasingColor() {
        val resources = RouteLineResources.Builder().routeLineTraveledCasingColor(5).build()

        assertEquals(5, resources.routeLineTraveledCasingColor)
    }

    @Test
    fun withRouteUnknownTrafficColor() {
        val resources = RouteLineResources.Builder().routeUnknownTrafficColor(5).build()

        assertEquals(5, resources.routeUnknownTrafficColor)
    }

    @Test
    fun withRouteDefaultColor() {
        val resources = RouteLineResources.Builder().routeDefaultColor(5).build()

        assertEquals(5, resources.routeDefaultColor)
    }

    @Test
    fun withRouteLowCongestionColor() {
        val resources = RouteLineResources.Builder().routeLowCongestionColor(5).build()

        assertEquals(5, resources.routeLowCongestionColor)
    }

    @Test
    fun withRouteModerateColor() {
        val resources = RouteLineResources.Builder().routeModerateColor(5).build()

        assertEquals(5, resources.routeModerateColor)
    }

    @Test
    fun withRouteHeavyColor() {
        val resources = RouteLineResources.Builder().routeHeavyColor(5).build()

        assertEquals(5, resources.routeHeavyColor)
    }

    @Test
    fun withRouteSevereColor() {
        val resources = RouteLineResources.Builder().routeSevereColor(5).build()

        assertEquals(5, resources.routeSevereColor)
    }

    @Test
    fun withRouteCasingColor() {
        val resources = RouteLineResources.Builder().routeCasingColor(5).build()

        assertEquals(5, resources.routeCasingColor)
    }

    @Test
    fun withRoundedLineCap() {
        val resources = RouteLineResources.Builder().roundedLineCap(false).build()

        assertFalse(resources.roundedLineCap)
    }

    @Test
    fun withAlternativeRouteUnknownColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteUnknownTrafficColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteUnknownTrafficColor)
    }

    @Test
    fun withAlternativeRouteDefaultColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteDefaultColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteDefaultColor)
    }

    @Test
    fun withAlternativeRouteLowColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteLowColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteLowColor)
    }

    @Test
    fun withAlternativeRouteModerateColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteModerateColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteModerateColor)
    }

    @Test
    fun withAlternativeRouteHeavyColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteHeavyColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteHeavyColor)
    }

    @Test
    fun withAlternativeRouteSevereColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteSevereColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteSevereColor)
    }

    @Test
    fun withAlternativeRouteCasingColor() {
        val resources = RouteLineResources.Builder()
            .alternativeRouteCasingColor(5)
            .build()

        assertEquals(5, resources.alternativeRouteCasingColor)
    }

    @Test
    fun withOriginWaypointIcon() {
        val resources = RouteLineResources.Builder()
            .originWaypointIcon(5)
            .build()

        assertEquals(5, resources.originWaypointIcon)
    }

    @Test
    fun withDestinationWaypointIcon() {
        val resources = RouteLineResources.Builder()
            .destinationWaypointIcon(5)
            .build()

        assertEquals(5, resources.destinationWaypointIcon)
    }

    @Test
    fun withTrafficBackfillRoadClasses() {
        val roadClasses = listOf<String>()

        val resources = RouteLineResources.Builder()
            .trafficBackfillRoadClasses(roadClasses)
            .build()

        assertEquals(roadClasses, resources.trafficBackfillRoadClasses)
    }

    @Test
    fun withRouteLineScaleValues() {
        val expression = Expression.ExpressionBuilder("").build()

        val resources = RouteLineResources.Builder()
            .routeLineScaleExpression(expression)
            .build()

        assertEquals(expression, resources.routeLineScaleExpression)
    }

    @Test
    fun withRouteLineCasingScaleValues() {
        val expression = Expression.ExpressionBuilder("").build()

        val resources = RouteLineResources.Builder()
            .routeCasingLineScaleExpression(expression)
            .build()

        assertEquals(expression, resources.routeCasingLineScaleExpression)
    }

    @Test
    fun withRouteLineTrafficScaleValues() {
        val expression = Expression.ExpressionBuilder("").build()

        val resources = RouteLineResources.Builder()
            .routeTrafficLineScaleExpression(expression)
            .build()

        assertEquals(expression, resources.routeTrafficLineScaleExpression)
    }

    @Test
    fun toBuilder() {
        val backFill = listOf<String>()
        val routeLineExpression = mockk<Expression>()
        val routeCasingExpression = mockk<Expression>()
        val trafficExpression = mockk<Expression>()

        val result = RouteLineResources.Builder()
            .routeLineTraveledColor(1)
            .routeLineTraveledCasingColor(2)
            .routeUnknownTrafficColor(3)
            .routeDefaultColor(4)
            .routeLowCongestionColor(5)
            .routeModerateColor(6)
            .routeHeavyColor(7)
            .routeSevereColor(8)
            .routeCasingColor(9)
            .roundedLineCap(false)
            .alternativeRouteUnknownTrafficColor(10)
            .alternativeRouteDefaultColor(11)
            .alternativeRouteLowColor(12)
            .alternativeRouteModerateColor(13)
            .alternativeRouteHeavyColor(14)
            .alternativeRouteSevereColor(15)
            .alternativeRouteCasingColor(16)
            .originWaypointIcon(17)
            .destinationWaypointIcon(18)
            .trafficBackfillRoadClasses(backFill)
            .routeLineScaleExpression(routeLineExpression)
            .routeCasingLineScaleExpression(routeCasingExpression)
            .routeTrafficLineScaleExpression(trafficExpression)
            .build()
            .toBuilder()
            .build()

        assertEquals(1, result.routeLineTraveledColor)
        assertEquals(2, result.routeLineTraveledCasingColor)
        assertEquals(3, result.routeUnknownTrafficColor)
        assertEquals(4, result.routeDefaultColor)
        assertEquals(5, result.routeLowCongestionColor)
        assertEquals(6, result.routeModerateColor)
        assertEquals(7, result.routeHeavyColor)
        assertEquals(8, result.routeSevereColor)
        assertEquals(9, result.routeCasingColor)
        assertFalse(result.roundedLineCap)
        assertEquals(10, result.alternativeRouteUnknownTrafficColor)
        assertEquals(11, result.alternativeRouteDefaultColor)
        assertEquals(12, result.alternativeRouteLowColor)
        assertEquals(13, result.alternativeRouteModerateColor)
        assertEquals(14, result.alternativeRouteHeavyColor)
        assertEquals(15, result.alternativeRouteSevereColor)
        assertEquals(16, result.alternativeRouteCasingColor)
        assertEquals(17, result.originWaypointIcon)
        assertEquals(18, result.destinationWaypointIcon)
        assertEquals(backFill, result.trafficBackfillRoadClasses)
        assertEquals(routeLineExpression, result.routeLineScaleExpression)
        assertEquals(routeCasingExpression, result.routeCasingLineScaleExpression)
        assertEquals(trafficExpression, result.routeTrafficLineScaleExpression)
    }
}
