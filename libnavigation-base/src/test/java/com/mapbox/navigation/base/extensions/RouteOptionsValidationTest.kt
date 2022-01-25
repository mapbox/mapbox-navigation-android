package com.mapbox.navigation.base.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import junit.framework.Assert.*
import org.junit.Test

class RouteOptionsValidationTest {

    @Test
    fun `calling applyDefaultNavigationOptions for driving makes options valid`() {
        val options = minimalValidOptionsBuilder().build()
        assertTrue(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `calling applyDefaultNavigationOptions for all profiles makes options valid`() {
        val allProfiles = listOf(
            DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            DirectionsCriteria.PROFILE_DRIVING,
            DirectionsCriteria.PROFILE_WALKING,
            DirectionsCriteria.PROFILE_CYCLING
        )
        for (profile in allProfiles) {
            val options = minimalValidOptionsBuilder(profile).build()
            assertTrue(
                "$profile profile isn't valid with default options",
                options.areRequiredArgumentsInPlace()
            )
        }
    }

    @Test
    fun `route options for driving without all required annotations is invalid`() {
        val options = minimalValidOptionsBuilder()
            .annotations(DirectionsCriteria.ANNOTATION_DISTANCE)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `route options for cycling without all required annotations is invalid`() {
        val options = minimalValidOptionsBuilder(DirectionsCriteria.PROFILE_CYCLING)
            .annotations(DirectionsCriteria.ANNOTATION_MAXSPEED)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `driving profile without full overview is invalid`() {
        val options = minimalValidOptionsBuilder()
            .overview(DirectionsCriteria.OVERVIEW_FALSE)
            .build()
        val result = options.areRequiredArgumentsInPlace()
        assertFalse(result)
    }

    @Test
    fun `options without roundabout exits aren't valid`() {
        val options = minimalValidOptionsBuilder()
            .roundaboutExits(false)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `options without voice instructions aren't valid`() {
        val options = minimalValidOptionsBuilder()
            .voiceInstructions(false)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `options without banner instructions aren't valid`() {
        val options = minimalValidOptionsBuilder()
            .bannerInstructions(false)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `driving profile isn't valid without continue straight option`() {
        val options = minimalValidOptionsBuilder()
            .continueStraight(false)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `options for cycling profile aren't valid with continue straight option`() {
        val options = minimalValidOptionsBuilder(DirectionsCriteria.PROFILE_CYCLING)
            .continueStraight(true)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `options for driving profile aren't valid without refresh`() {
        val options = minimalValidOptionsBuilder()
            .enableRefresh(false)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    @Test
    fun `options for walking profile aren't valid with refresh`() {
        val options = minimalValidOptionsBuilder(DirectionsCriteria.PROFILE_WALKING)
            .enableRefresh(true)
            .build()
        assertFalse(options.areRequiredArgumentsInPlace())
    }

    fun `optional options doesn't make request invalid`() {

    }
}

private fun minimalValidOptionsBuilder(
    @DirectionsCriteria.ProfileCriteria profile: String =
        DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
) = RouteOptions.builder()
    .coordinatesList(
        listOf(
            Point.fromLngLat(0.0, 0.0),
            Point.fromLngLat(1.1, 1.1),
        )
    )
    .applyDefaultNavigationOptions(profile)
