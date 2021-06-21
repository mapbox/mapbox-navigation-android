package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.models.RouteOptions
import org.junit.Assert.assertEquals

object MapboxRouteOptionsUpdateCommonTest {

    fun checkImmutableFields(routeOptions: RouteOptions, updated: RouteOptions) {
        assertEquals("Check Overview", routeOptions.overview(), updated.overview())
        assertEquals("Check Annotations", routeOptions.annotations(), updated.annotations())
        assertEquals("Check Profile", routeOptions.profile(), updated.profile())
        assertEquals("Check Token", routeOptions.accessToken(), updated.accessToken())
        assertEquals(
            "Check Alternatives",
            routeOptions.alternatives(),
            updated.alternatives()
        )
        assertEquals("Check Steps", routeOptions.steps(), updated.steps())
        assertEquals(
            "Check BannerInstructions",
            routeOptions.bannerInstructions(),
            updated.bannerInstructions()
        )
        assertEquals("Check Base URL", routeOptions.baseUrl(), updated.baseUrl())
        assertEquals(
            "Check Continue Straight",
            routeOptions.continueStraight(),
            updated.continueStraight()
        )
        assertEquals("Check Exclude", routeOptions.exclude(), updated.exclude())
        assertEquals("Check Language", routeOptions.language(), updated.language())
        assertEquals("Check Geometries", routeOptions.geometries(), updated.geometries())
        assertEquals("Check User", routeOptions.user(), updated.user())
        assertEquals(
            "Check Roundabout Exits",
            routeOptions.roundaboutExits(),
            updated.roundaboutExits()
        )
        assertEquals(
            "Check alleyBias", routeOptions.alleyBias(), updated.alleyBias()
        )
        assertEquals(
            "Check walkingSpeed", routeOptions.walkingSpeed(), updated.walkingSpeed()
        )
        assertEquals(
            "Check walkwayBias", routeOptions.walkwayBias(), updated.walkwayBias()
        )
        assertEquals(
            "Check Voice Instructions",
            routeOptions.voiceInstructions(),
            updated.voiceInstructions()
        )
    }
}
