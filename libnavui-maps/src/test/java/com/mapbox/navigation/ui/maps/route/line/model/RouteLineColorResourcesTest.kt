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
            .routeDefaultColor(4)
            .routeLowCongestionColor(5)
            .routeModerateCongestionColor(6)
            .routeHeavyCongestionColor(7)
            .routeSevereCongestionColor(8)
            .routeUnknownCongestionColor(3)
            .inactiveRouteLegLowCongestionColor(95)
            .inactiveRouteLegModerateCongestionColor(96)
            .inactiveRouteLegHeavyCongestionColor(97)
            .inactiveRouteLegSevereCongestionColor(98)
            .inactiveRouteLegUnknownCongestionColor(93)
            .restrictedRoadColor(17)
            .routeClosureColor(19)
            .inactiveRouteLegRestrictedRoadColor(917)
            .inactiveRouteLegClosureColor(919)
            .alternativeRouteDefaultColor(11)
            .alternativeRouteLowCongestionColor(12)
            .alternativeRouteModerateCongestionColor(13)
            .alternativeRouteHeavyCongestionColor(14)
            .alternativeRouteSevereCongestionColor(15)
            .alternativeRouteUnknownCongestionColor(10)
            .alternativeRouteRestrictedRoadColor(18)
            .alternativeRouteClosureColor(20)
            .routeLineTraveledColor(1)
            .routeLineTraveledCasingColor(2)
            .routeCasingColor(9)
            .alternativeRouteCasingColor(16)
            .inActiveRouteLegsColor(21)
            .inactiveRouteLegCasingColor(22)
    }

    @Test
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
        val resources =
            RouteLineColorResources.Builder().routeUnknownCongestionColor(5).build()

        assertEquals(5, resources.routeUnknownCongestionColor)
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
        val resources =
            RouteLineColorResources.Builder().routeModerateCongestionColor(5).build()

        assertEquals(5, resources.routeModerateCongestionColor)
    }

    @Test
    fun withRouteHeavyColor() {
        val resources = RouteLineColorResources.Builder().routeHeavyCongestionColor(5).build()

        assertEquals(5, resources.routeHeavyCongestionColor)
    }

    @Test
    fun withRouteSevereColor() {
        val resources =
            RouteLineColorResources.Builder().routeSevereCongestionColor(5).build()

        assertEquals(5, resources.routeSevereCongestionColor)
    }

    @Test
    fun withRouteCasingColor() {
        val resources = RouteLineColorResources.Builder().routeCasingColor(5).build()

        assertEquals(5, resources.routeCasingColor)
    }

    @Test
    fun withAlternativeRouteUnknownColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteUnknownCongestionColor(5)
                .build()

        assertEquals(5, resources.alternativeRouteUnknownCongestionColor)
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
            RouteLineColorResources.Builder().alternativeRouteLowCongestionColor(5).build()

        assertEquals(5, resources.alternativeRouteLowCongestionColor)
    }

    @Test
    fun withAlternativeRouteModerateColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteModerateCongestionColor(5)
                .build()

        assertEquals(5, resources.alternativeRouteModerateCongestionColor)
    }

    @Test
    fun withAlternativeRouteHeavyColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteHeavyCongestionColor(5).build()

        assertEquals(5, resources.alternativeRouteHeavyCongestionColor)
    }

    @Test
    fun withAlternativeRouteSevereColor() {
        val resources =
            RouteLineColorResources.Builder().alternativeRouteSevereCongestionColor(5).build()

        assertEquals(5, resources.alternativeRouteSevereCongestionColor)
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
            .routeDefaultColor(4)
            .routeLowCongestionColor(5)
            .routeModerateCongestionColor(6)
            .routeHeavyCongestionColor(7)
            .routeSevereCongestionColor(8)
            .routeUnknownCongestionColor(3)
            .restrictedRoadColor(17)
            .routeClosureColor(19)
            .alternativeRouteDefaultColor(11)
            .alternativeRouteLowCongestionColor(12)
            .alternativeRouteModerateCongestionColor(13)
            .alternativeRouteHeavyCongestionColor(14)
            .alternativeRouteSevereCongestionColor(15)
            .alternativeRouteUnknownCongestionColor(10)
            .alternativeRouteRestrictedRoadColor(18)
            .alternativeRouteClosureColor(20)
            .routeLineTraveledColor(1)
            .routeLineTraveledCasingColor(2)
            .routeCasingColor(9)
            .alternativeRouteCasingColor(16)
            .inActiveRouteLegsColor(21)
            .build()

        val result = routeLineColorResources.toBuilder().build()

        assertEquals(1, result.routeLineTraveledColor)
        assertEquals(2, result.routeLineTraveledCasingColor)
        assertEquals(3, result.routeUnknownCongestionColor)
        assertEquals(4, result.routeDefaultColor)
        assertEquals(5, result.routeLowCongestionColor)
        assertEquals(6, result.routeModerateCongestionColor)
        assertEquals(7, result.routeHeavyCongestionColor)
        assertEquals(8, result.routeSevereCongestionColor)
        assertEquals(9, result.routeCasingColor)
        assertEquals(10, result.alternativeRouteUnknownCongestionColor)
        assertEquals(11, result.alternativeRouteDefaultColor)
        assertEquals(12, result.alternativeRouteLowCongestionColor)
        assertEquals(13, result.alternativeRouteModerateCongestionColor)
        assertEquals(14, result.alternativeRouteHeavyCongestionColor)
        assertEquals(15, result.alternativeRouteSevereCongestionColor)
        assertEquals(16, result.alternativeRouteCasingColor)
        assertEquals(17, result.restrictedRoadColor)
        assertEquals(18, result.alternativeRouteRestrictedRoadColor)
        assertEquals(19, result.routeClosureColor)
        assertEquals(20, result.alternativeRouteClosureColor)
        assertEquals(21, result.inActiveRouteLegsColor)
    }
}
