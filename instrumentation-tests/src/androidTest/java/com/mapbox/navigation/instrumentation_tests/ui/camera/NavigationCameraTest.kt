package com.mapbox.navigation.instrumentation_tests.ui.camera

import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.instrumentation_tests.ui.SimpleMapViewNavigationTest
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class NavigationCameraTest : SimpleMapViewNavigationTest() {

    /**
     * https://github.com/mapbox/mapbox-navigation-android/issues/4453
     * https://github.com/mapbox/mapbox-navigation-android/pull/4575
     * https://cs.android.com/android/_/android/platform/frameworks/base/+/3dbaae1ef4f221b3626810f4ba19eec068dd6304
     *
     * Checks for an issue where Android Animators where running into a cancellation loop
     * on some API levels (definitely 21-24, maybe later) and caused stack overflow.
     */
    @Test
    fun navigation_camera_mode_changes_completes() = sdkTest {
        withSimpleMapViewNavigation { mapboxNavigation ->
            addNavigationCamera(mapboxNavigation)

            navigationCamera.requestNavigationCameraToFollowing()
            navigationCamera.requestNavigationCameraToOverview()
            navigationCamera.requestNavigationCameraToFollowing()
        }
    }

    /**
     * A camera that is asked to follow before the viewport data source has computed its first
     * frame must not fly towards the placeholders of that frame: that zooms the map out and back
     * in once the first frame arrives.
     */
    @Test
    fun following_requested_before_the_first_frame_does_not_zoom_out() = sdkTest {
        val mapboxMap = activity.mapboxMap
        val position = Point.fromLngLat(53.8949337, 27.5558128)
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(position)
                .zoom(START_ZOOM)
                .bearing(0.0)
                .pitch(0.0)
                .build(),
        )
        val viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        val camera = NavigationCamera(mapboxMap, activity.binding.mapView.camera, viewportDataSource)
        var lowestZoom = mapboxMap.cameraState.zoom
        val subscription = mapboxMap.subscribeCameraChanged {
            lowestZoom = min(lowestZoom, mapboxMap.cameraState.zoom)
        }

        // the position is known, but evaluate() has not computed the first frame yet
        viewportDataSource.onLocationChanged(
            Location.Builder()
                .longitude(position.longitude())
                .latitude(position.latitude())
                .build(),
        )
        camera.requestNavigationCameraToFollowing()
        delay(UNEVALUATED_WAIT_MS)
        viewportDataSource.evaluate()
        delay(EVALUATED_WAIT_MS)
        subscription.cancel()

        assertTrue(
            "the zoom dropped to $lowestZoom from $START_ZOOM",
            lowestZoom >= START_ZOOM - ZOOM_TOLERANCE,
        )
    }

    /**
     * Nothing is known about where to go: the camera must stay where the map is.
     */
    @Test
    fun following_requested_with_nothing_known_does_not_move_the_camera() = sdkTest {
        val mapboxMap = activity.mapboxMap
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 0.0))
                .zoom(NOTHING_KNOWN_ZOOM)
                .bearing(0.0)
                .pitch(0.0)
                .build(),
        )
        val viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        val camera = NavigationCamera(mapboxMap, activity.binding.mapView.camera, viewportDataSource)
        val before = mapboxMap.cameraState

        camera.requestNavigationCameraToFollowing()
        delay(UNEVALUATED_WAIT_MS)

        val after = mapboxMap.cameraState
        assertEquals(before.zoom, after.zoom, ZOOM_TOLERANCE)
        assertEquals(before.center.longitude(), after.center.longitude(), CENTER_TOLERANCE)
        assertEquals(before.center.latitude(), after.center.latitude(), CENTER_TOLERANCE)
    }

    /**
     * The center that the app overrides is where the camera goes before the first frame is
     * computed, without zooming out on the way.
     */
    @Test
    fun following_requested_before_the_first_frame_goes_to_the_overridden_center() = sdkTest {
        val mapboxMap = activity.mapboxMap
        val start = Point.fromLngLat(53.8949337, 27.5558128)
        val overriddenCenter = Point.fromLngLat(53.8959337, 27.5568128)
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(start)
                .zoom(START_ZOOM)
                .bearing(0.0)
                .pitch(0.0)
                .build(),
        )
        val viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        val camera = NavigationCamera(mapboxMap, activity.binding.mapView.camera, viewportDataSource)
        var lowestZoom = mapboxMap.cameraState.zoom
        val subscription = mapboxMap.subscribeCameraChanged {
            lowestZoom = min(lowestZoom, mapboxMap.cameraState.zoom)
        }

        viewportDataSource.followingCenterPropertyOverride(overriddenCenter)
        camera.requestNavigationCameraToFollowing()
        delay(UNEVALUATED_WAIT_MS)
        subscription.cancel()

        val center = mapboxMap.cameraState.center
        assertEquals(overriddenCenter.longitude(), center.longitude(), CENTER_TOLERANCE)
        assertEquals(overriddenCenter.latitude(), center.latitude(), CENTER_TOLERANCE)
        assertTrue(
            "the zoom dropped to $lowestZoom from $START_ZOOM",
            lowestZoom >= START_ZOOM - ZOOM_TOLERANCE,
        )
    }

    /**
     * An overview that has no points to frame has nowhere to move the camera to.
     */
    @Test
    fun overview_requested_before_the_first_frame_does_not_move_the_camera() = sdkTest {
        val mapboxMap = activity.mapboxMap
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 0.0))
                .zoom(NOTHING_KNOWN_ZOOM)
                .bearing(0.0)
                .pitch(0.0)
                .build(),
        )
        val viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        val camera = NavigationCamera(mapboxMap, activity.binding.mapView.camera, viewportDataSource)
        val before = mapboxMap.cameraState

        camera.requestNavigationCameraToOverview()
        delay(UNEVALUATED_WAIT_MS)

        val after = mapboxMap.cameraState
        assertEquals(before.zoom, after.zoom, ZOOM_TOLERANCE)
        assertEquals(before.center.longitude(), after.center.longitude(), CENTER_TOLERANCE)
        assertEquals(before.center.latitude(), after.center.latitude(), CENTER_TOLERANCE)
    }

    private companion object {
        const val START_ZOOM = 15.0
        const val NOTHING_KNOWN_ZOOM = 5.0
        const val ZOOM_TOLERANCE = 0.05
        const val CENTER_TOLERANCE = 1e-6
        const val UNEVALUATED_WAIT_MS = 600L
        const val EVALUATED_WAIT_MS = 2_000L
    }
}
