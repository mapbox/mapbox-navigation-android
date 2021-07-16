package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.KClass

class RouteLineColorResourcesTest :
    BuilderTest<RouteLineColorResources, RouteLineColorResources.Builder>() {

    override fun getImplementationClass(): KClass<RouteLineColorResources> =
        RouteLineColorResources::class

    override fun getFilledUpBuilder(): RouteLineColorResources.Builder {
        return RouteLineColorResources.Builder()
            .routeLineTraveledColor(1)
            .routeLineTraveledCasingColor(2)
            .routeUnknownTrafficColor(3)
            .routeDefaultColor(4)
            .routeLowCongestionColor(5)
            .routeModerateColor(6)
            .routeHeavyColor(7)
            .routeSevereColor(8)
            .routeCasingColor(9)
            .alternativeRouteUnknownTrafficColor(10)
            .alternativeRouteDefaultColor(11)
            .alternativeRouteLowColor(12)
            .alternativeRouteModerateColor(13)
            .alternativeRouteHeavyColor(14)
            .alternativeRouteSevereColor(15)
            .alternativeRouteCasingColor(16)
            .restrictedRoadColor(17)
            .alternativeRouteRestrictedRoadColor(18)
            .routeClosureColor(19)
            .alternativeRouteClosureColor(20)
            .inActiveRouteLegsColor(21)
    }

    override fun trigger() {
        //
    }

    @Test
    fun withRouteLineTraveledColor() {
        val resources = RouteLineColorResources.Builder().routeLineTraveledColor(5).build()

        assertEquals(5, resources.routeLineTraveledColor)
    }

    @Test
    fun withRouteLineTraveledCasingColor() {
        val resources =
            RouteLineColorResources.Builder().routeLineTraveledCasingColor(5).build()

        assertEquals(5, resources.routeLineTraveledCasingColor)
    }

    @Test
    fun withRouteUnknownTrafficColor() {
        val resources = RouteLineColorResources.Builder().routeUnknownTrafficColor(5).build()

        assertEquals(5, resources.routeUnknownTrafficColor)
    }

    @Test
    fun withRouteDefaultColor() {
        val resources = RouteLineColorResources.Builder().routeDefaultColor(5).build()

        assertEquals(5, resources.routeDefaultColor)
    }

    @Test
    fun withRouteLowCongestionColor() {
        val resources = RouteLineColorResources.Builder().routeLowCongestionColor(5).build()

        assertEquals(5, resources.routeLowCongestionColor)
    }

    @Test
    fun withRouteModerateColor() {
        val resources = RouteLineColorResources.Builder().routeModerateColor(5).build()

        assertEquals(5, resources.routeModerateColor)
    }

    @Test
    fun withRouteHeavyColor() {
        val resources = RouteLineColorResources.Builder().routeHeavyColor(5).build()

        assertEquals(5, resources.routeHeavyColor)
    }

    @Test
    fun withRouteSevereColor() {
        val resources = RouteLineColorResources.Builder().routeSevereColor(5).build()

        assertEquals(5, resources.routeSevereColor)
    }

    @Test
    fun withRouteCasingColor() {
        val resources = RouteLineColorResources.Builder().routeCasingColor(5).build()

        assertEquals(5, resources.routeCasingColor)
    }

    @Test
    fun withAlternativeRouteUnknownColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteUnknownTrafficColor(5).build()

        assertEquals(5, resources.alternativeRouteUnknownTrafficColor)
    }

    @Test
    fun withAlternativeRouteDefaultColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteDefaultColor(5).build()

        assertEquals(5, resources.alternativeRouteDefaultColor)
    }

    @Test
    fun withAlternativeRouteLowColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteLowColor(5).build()

        assertEquals(5, resources.alternativeRouteLowColor)
    }

    @Test
    fun withAlternativeRouteModerateColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteModerateColor(5).build()

        assertEquals(5, resources.alternativeRouteModerateColor)
    }

    @Test
    fun withAlternativeRouteHeavyColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteHeavyColor(5).build()

        assertEquals(5, resources.alternativeRouteHeavyColor)
    }

    @Test
    fun withAlternativeRouteSevereColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteSevereColor(5).build()

        assertEquals(5, resources.alternativeRouteSevereColor)
    }

    @Test
    fun withAlternativeRouteCasingColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteCasingColor(5).build()

        assertEquals(5, resources.alternativeRouteCasingColor)
    }

    @Test
    fun withRestrictedRoadColor() {
        val resources =
            RouteLineColorResources.Builder().restrictedRoadColor(5).build()

        assertEquals(5, resources.restrictedRoadColor)
    }

    @Test
    fun withAlternateRouteRestrictedRoadColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteRestrictedRoadColor(5).build()

        assertEquals(5, resources.alternativeRouteRestrictedRoadColor)
    }

    @Test
    fun withRoadClosure() {
        val resources =
            RouteLineColorResources.Builder().routeClosureColor(5).build()

        assertEquals(5, resources.routeClosureColor)
    }

    @Test
    fun alternativeRouteClosureColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteClosureColor(5).build()

        assertEquals(5, resources.alternativeRouteClosureColor)
    }

    @Test
    fun inActiveRouteLegsColor() {
        val resources =
            RouteLineColorResources.Builder().inActiveRouteLegsColor(5).build()

        assertEquals(5, resources.inActiveRouteLegsColor)
    }

    @Test
    fun toBuilder() {
        val routeLineColorResources = RouteLineColorResources.Builder()
            .routeLineTraveledColor(1)
            .routeLineTraveledCasingColor(2)
            .routeUnknownTrafficColor(3)
            .routeDefaultColor(4)
            .routeLowCongestionColor(5)
            .routeModerateColor(6)
            .routeHeavyColor(7)
            .routeSevereColor(8)
            .routeCasingColor(9)
            .alternativeRouteUnknownTrafficColor(10)
            .alternativeRouteDefaultColor(11)
            .alternativeRouteLowColor(12)
            .alternativeRouteModerateColor(13)
            .alternativeRouteHeavyColor(14)
            .alternativeRouteSevereColor(15)
            .alternativeRouteCasingColor(16)
            .restrictedRoadColor(17)
            .alternativeRouteRestrictedRoadColor(18)
            .routeClosureColor(19)
            .alternativeRouteClosureColor(20)
            .inActiveRouteLegsColor(21)
            .build()

        val result = routeLineColorResources.toBuilder().build()

        assertEquals(1, result.routeLineTraveledColor)
        assertEquals(2, result.routeLineTraveledCasingColor)
        assertEquals(3, result.routeUnknownTrafficColor)
        assertEquals(4, result.routeDefaultColor)
        assertEquals(5, result.routeLowCongestionColor)
        assertEquals(6, result.routeModerateColor)
        assertEquals(7, result.routeHeavyColor)
        assertEquals(8, result.routeSevereColor)
        assertEquals(9, result.routeCasingColor)
        assertEquals(10, result.alternativeRouteUnknownTrafficColor)
        assertEquals(11, result.alternativeRouteDefaultColor)
        assertEquals(12, result.alternativeRouteLowColor)
        assertEquals(13, result.alternativeRouteModerateColor)
        assertEquals(14, result.alternativeRouteHeavyColor)
        assertEquals(15, result.alternativeRouteSevereColor)
        assertEquals(16, result.alternativeRouteCasingColor)
        assertEquals(17, result.restrictedRoadColor)
        assertEquals(18, result.alternativeRouteRestrictedRoadColor)
        assertEquals(19, result.routeClosureColor)
        assertEquals(20, result.alternativeRouteClosureColor)
        assertEquals(21, result.inActiveRouteLegsColor)
    }
}
