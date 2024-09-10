package com.mapbox.navigation.utils.internal

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import org.junit.Test

class RouteExTest {

    @Test
    fun `verify geometryPoints`() {
        mockkStatic("com.mapbox.navigation.utils.internal.RouteExKt") {
            val mockRouteLeg = mockk<RouteLeg>()
            val stepGeometry = provideStepsGeometry()
            every {
                mockRouteLeg.stepsGeometryToPoints(any())
            } returns stepGeometry

            val result = mockRouteLeg.geometryPoints(-1)

            assertEquals(
                listOf(
                    Point.fromLngLat(1.0, 2.0),
                    Point.fromLngLat(3.0, 4.0),
                    Point.fromLngLat(5.0, 6.0),
                    Point.fromLngLat(7.0, 8.0),
                ),
                result,
            )
        }
    }

    // geometry rules:
    // - if step contains only 2 points - they are the same and they have to be squashed to 1 point
    // - the first point of upcoming step is the same as the last point of the previous step - they
    //   have to be squashed
    private fun provideStepsGeometry(): List<List<Point>> =
        listOf(
            listOf(
                Point.fromLngLat(1.0, 2.0),
                Point.fromLngLat(1.0, 2.0),
            ),
            listOf(
                Point.fromLngLat(1.0, 2.0),
                Point.fromLngLat(3.0, 4.0),
                Point.fromLngLat(5.0, 6.0),
                Point.fromLngLat(7.0, 8.0),
            ),
            listOf(
                Point.fromLngLat(7.0, 8.0),
                Point.fromLngLat(7.0, 8.0),
            ),
        )
}
