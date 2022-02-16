package com.mapbox.navigation.dropin.component.roadlabel

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RoadNameComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `onAttached renders location matcher results`() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
        val mockRoad = mockk<Road>()
        val locationMaterResult = mockk<LocationMatcherResult> {
            every { road } returns mockRoad
        }
        val roadNameView = mockk<MapboxRoadNameView>(relaxed = true)
        val mapboxNavigation = mockk<MapboxNavigation> {
            every { flowLocationMatcherResult() } returns flowOf(locationMaterResult)
        }

        RoadNameComponent(roadNameView).onAttached(mapboxNavigation)

        verify { roadNameView.renderRoadName(mockRoad) }
        unmockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
    }
}
