package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.common.Cancelable
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.internal.camera.FollowingFramingModeHolder
import com.mapbox.navigation.ui.maps.internal.camera.RouteOverviewViewportDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers the [OverviewChangeObserver] notifications of [MapboxNavigationViewportDataSource]: a
 * switch of the overview mode, or of the active leg while the overview frames the active leg,
 * moves the overview frame by a large amount.
 */
class MapboxNavigationViewportDataSourceOverviewChangeTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxMap: MapboxMap = mockk(relaxed = true)

    // What the route overview data source reports for the next evaluation. The detection itself
    // lives in RouteOverviewViewportDataSource, so it is stubbed here.
    private var overviewChanged = false
    private val routeOverviewViewportDataSource = mockk<RouteOverviewViewportDataSource>(
        relaxed = true,
    ) {
        every { evaluateOverviewChange() } answers {
            overviewChanged.also { overviewChanged = false }
        }
    }

    private var overviewChangeCount = 0
    private lateinit var dataSource: MapboxNavigationViewportDataSource

    @OptIn(MapboxExperimental::class)
    @Before
    fun setUp() {
        val actionSlot = slot<() -> Unit>()
        every { mapboxMap.whenSizeReady(capture(actionSlot)) } answers {
            actionSlot.captured()
            mockk<Cancelable>(relaxed = true)
        }

        dataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            mockk<FollowingFramingModeHolder>(relaxed = true),
            routeOverviewViewportDataSource,
        )
        dataSource.registerOverviewChangeObserver { overviewChangeCount++ }
    }

    @Test
    fun `unregistered observer is not notified`() {
        // GIVEN
        var unregisteredChanges = 0
        val observer = OverviewChangeObserver { unregisteredChanges++ }
        dataSource.registerOverviewChangeObserver(observer)
        dataSource.unregisterOverviewChangeObserver(observer)

        // WHEN
        overviewChanged = true
        dataSource.evaluate()

        // THEN
        assertEquals(0, unregisteredChanges)
        assertEquals(1, overviewChangeCount)
    }

    @Test
    fun `overview change observers see the up to date viewport data`() {
        // GIVEN
        dataSource.followingZoomPropertyOverride(10.0)
        dataSource.evaluate()
        var zoomSeenByObserver: Double? = null
        dataSource.registerOverviewChangeObserver {
            zoomSeenByObserver = dataSource.getViewportData().cameraForFollowing.zoom
        }

        // WHEN
        dataSource.followingZoomPropertyOverride(11.0)
        overviewChanged = true
        dataSource.evaluate()

        // THEN
        assertEquals(11.0, zoomSeenByObserver!!, 0.000001)
    }
}
