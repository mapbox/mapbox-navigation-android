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
        assertTrue(options.areCompatibleWithSDK())
    }

    @Test
    fun `calling applyDefaultNavigationOptions for all profiles makes options compatible`() {
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
                options.areCompatibleWithSDK()
            )
        }
    }

    @Test
    fun `route options with steps aren't compatible with SDK`() {
        val options = minimalValidOptionsBuilder().steps(false).build()
        assertFalse(options.areCompatibleWithSDK())
    }

    @Test
    fun `wrong options could be compatible with SDK`() {
        val options = minimalValidOptionsBuilder(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FALSE)
            .build()
        assertTrue(options.areCompatibleWithSDK())
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
