package com.mapbox.navigation.core.routealternatives

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.RouteProgressData
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AlternativeRouteProgressDataProviderTest {

    @Test
    fun `fork passed`() {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(4, primaryRouteGeometryIndex, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex - 1,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 40
        )
        val expected = RouteProgressData(3, 90, 40)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `fork passed, alternative is longer than primary`() {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(3, primaryRouteGeometryIndex, 40)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 3,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex - 1,
            primaryForkLegGeometryIndex = 40,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 130,
            alternativeForkLegGeometryIndex = 80
        )
        val expected = RouteProgressData(5, 130, 80)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `on fork`() {
        val primaryRouteGeometryIndex = 200
        val primaryRouteProgressData = RouteProgressData(4, primaryRouteGeometryIndex, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = primaryRouteGeometryIndex,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 40
        )
        val expected = RouteProgressData(3, 90, 40)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `before fork, alternative starts together with the primary route`() {
        val primaryRouteProgressData = RouteProgressData(4, 200, 30)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = 250,
            primaryForkLegGeometryIndex = 80,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 250,
            alternativeForkLegGeometryIndex = 80
        )
        val expected = primaryRouteProgressData

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `before fork, alternative starts after the primary route on the first leg`() {
        // primary leg and route indices diff is 165
        val primaryRouteProgressData = RouteProgressData(4, 200, 35)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = 250,
            primaryForkLegGeometryIndex = 85,
            alternativeForkLegIndex = 5,
            alternativeForkRouteGeometryIndex = 220,
            alternativeForkLegGeometryIndex = 59
        )
        val expected = RouteProgressData(4, 170, 9)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `before fork, alternative starts after the primary route on the second leg`() {
        // primary leg and route indices diff is 165
        val primaryRouteProgressData = RouteProgressData(4, 200, 35)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 5,
            primaryForkRouteGeometryIndex = 250,
            primaryForkLegGeometryIndex = 85,
            alternativeForkLegIndex = 3,
            alternativeForkRouteGeometryIndex = 220,
            alternativeForkLegGeometryIndex = 59
        )
        val expected = RouteProgressData(2, 170, 9)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `before fork, alternative starts after the primary route on the current leg`() {
        // primary leg and route indices diff is 140
        val primaryRouteProgressData = RouteProgressData(4, 200, 60)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 4,
            primaryForkRouteGeometryIndex = 250,
            primaryForkLegGeometryIndex = 110,
            alternativeForkLegIndex = 0,
            alternativeForkRouteGeometryIndex = 90,
            alternativeForkLegGeometryIndex = 90
        )
        val expected = RouteProgressData(0, 40, 40)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `before fork, primary starts after the alternative route on the current leg`() {
        // primary leg and route indices diff is 140
        val primaryRouteProgressData = RouteProgressData(0, 40, 40)
        val alternativeMetadata = alternativeRouteMetadata(
            primaryForkLegIndex = 0,
            primaryForkRouteGeometryIndex = 90,
            primaryForkLegGeometryIndex = 90,
            alternativeForkLegIndex = 4,
            alternativeForkRouteGeometryIndex = 250,
            alternativeForkLegGeometryIndex = 110
        )
        val expected = RouteProgressData(4, 200, 60)

        val actual = AlternativeRouteProgressDataProvider.getRouteProgressData(
            primaryRouteProgressData,
            alternativeMetadata
        )

        assertEquals(expected, actual)
    }

    private fun alternativeRouteMetadata(
        primaryForkLegIndex: Int,
        primaryForkRouteGeometryIndex: Int,
        primaryForkLegGeometryIndex: Int,
        alternativeForkLegIndex: Int,
        alternativeForkRouteGeometryIndex: Int,
        alternativeForkLegGeometryIndex: Int,
    ): AlternativeRouteMetadata {
        return AlternativeRouteMetadata(
            mockk(),
            AlternativeRouteIntersection(
                Point.fromLngLat(1.1, 2.2),
                alternativeForkRouteGeometryIndex,
                alternativeForkLegGeometryIndex,
                alternativeForkLegIndex
            ),
            AlternativeRouteIntersection(
                Point.fromLngLat(3.3, 4.4),
                primaryForkRouteGeometryIndex,
                primaryForkLegGeometryIndex,
                primaryForkLegIndex
            ),
            mockk(),
            mockk(),
            0
        )
    }
}
