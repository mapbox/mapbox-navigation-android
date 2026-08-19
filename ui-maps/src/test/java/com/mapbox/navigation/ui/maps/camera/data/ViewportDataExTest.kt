package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportDataExTest {

    private fun cameraOptions(lng: Double = 1.0, lat: Double = 2.0, zoom: Double = 10.0) =
        CameraOptions.Builder()
            .center(Point.fromLngLat(lng, lat))
            .zoom(zoom)
            .bearing(0.0)
            .pitch(0.0)
            .build()

    private fun viewportData(
        following: CameraOptions = cameraOptions(),
        overview: CameraOptions = cameraOptions(),
        pointsOverview: CameraOptions = cameraOptions(),
    ) = ViewportData(
        cameraForFollowing = following,
        cameraForOverview = overview,
        cameraForPointsOverview = pointsOverview,
    )

    @Test
    fun `standstill when all three frames match`() {
        assertTrue(viewportData().isStandstill(viewportData()))
    }

    @Test
    fun `not standstill when following frame differs`() {
        assertFalse(
            viewportData(following = cameraOptions(zoom = 15.0)).isStandstill(viewportData()),
        )
    }

    @Test
    fun `not standstill when overview frame differs`() {
        assertFalse(
            viewportData(overview = cameraOptions(zoom = 15.0)).isStandstill(viewportData()),
        )
    }

    /**
     * Regression: the points-overview frame used to be excluded from the comparison, so a points
     * overview update produced while the vehicle was stationary (following and route overview frames
     * unchanged) was never published to the observers, leaving the camera on a stale frame.
     */
    @Test
    fun `not standstill when only points overview frame differs`() {
        assertFalse(
            viewportData(pointsOverview = cameraOptions(zoom = 15.0)).isStandstill(viewportData()),
        )
    }

    @Test
    fun `not standstill when only points overview center differs`() {
        assertFalse(
            viewportData(pointsOverview = cameraOptions(lng = 30.0, lat = 40.0))
                .isStandstill(viewportData()),
        )
    }
}
