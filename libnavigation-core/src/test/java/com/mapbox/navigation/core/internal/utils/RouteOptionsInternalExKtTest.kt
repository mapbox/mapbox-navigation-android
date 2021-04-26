package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultOptions
import org.junit.Assert
import org.junit.Test

class RouteOptionsInternalExKtTest {
    @Test
    fun `check valid uuid for cases route options is null, uuid is empty and uuid is offline`() {
        val cases = listOf(
            Triple(
                "Route Options is null",
                null,
                false
            ),
            Triple(
                "Uuid is empty",
                provideRouteOptions(""),
                false
            ),
            Triple(
                "Uuid is offline",
                provideRouteOptions("offline"),
                false
            ),
            Triple(
                "Valid uuid",
                provideRouteOptions("dusa1is21asi"),
                true
            )
        )

        cases.forEach { (message, routeOptions, isValidExpected) ->
            Assert.assertEquals(message, isValidExpected, routeOptions.isUuidValidForRefresh())
        }
    }

    private fun provideRouteOptions(uuid: String): RouteOptions =
        RouteOptions.builder()
            .applyDefaultOptions()
            .coordinates(listOf(Point.fromLngLat(0.0, 0.0), Point.fromLngLat(1.1, 1.1)))
            .accessToken("pk.**")
            .requestUuid(uuid)
            .build()
}
