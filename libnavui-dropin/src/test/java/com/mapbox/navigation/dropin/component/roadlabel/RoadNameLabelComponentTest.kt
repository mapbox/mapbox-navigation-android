package com.mapbox.navigation.dropin.component.roadlabel

import androidx.core.view.isVisible
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.Expected
import com.mapbox.maps.Style
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.extensions.flowRouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RoadNameLabelComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    lateinit var style: Style

    @Before
    fun setUp() {
        style = mockk {
            every { styleURI } returns NavigationStyles.NAVIGATION_DAY_STYLE
        }
    }

    @Test
    fun `onAttached should hide roadNameView when road name data is NOT available`() =
        coroutineRule.runBlockingTest {
            val roadNameView = mockk<MapboxRoadNameView>(relaxed = true)
            val matcherResult = mockk<LocationMatcherResult> {
                every { road } returns mockk {
                    every { components } returns listOf()
                }
            }
            val mapboxNavigation = mockk<MapboxNavigation>()
            val locationViewModel = mockk<LocationViewModel> {
                every { state } returns MutableStateFlow(matcherResult)
            }

            RoadNameLabelComponent(roadNameView, locationViewModel, style)
                .onAttached(mapboxNavigation)

            verify { roadNameView.isVisible = false }
        }

    @Test
    fun `onAttached should show roadNameView when road name data is available`() =
        coroutineRule.runBlockingTest {
            val roadNameView = mockk<MapboxRoadNameView>(relaxed = true)
            val matcherResult = mockk<LocationMatcherResult> {
                every { road } returns mockk {
                    every { components } returns listOf(mockk())
                }
            }
            val mapboxNavigation = mockk<MapboxNavigation>()
            val locationViewModel = mockk<LocationViewModel> {
                every { state } returns MutableStateFlow(matcherResult)
            }

            RoadNameLabelComponent(roadNameView, locationViewModel, style)
                .onAttached(mapboxNavigation)

            verify { roadNameView.isVisible = true }
        }

    @Test
    fun `onAttached renders location matcher results`() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
        val mockRoad = mockk<Road> {
            every { components } returns listOf(mockk())
        }
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { road } returns mockRoad
        }
        val roadNameView = mockk<MapboxRoadNameView>(relaxed = true)
        val mapboxNavigation = mockk<MapboxNavigation> {
            every { flowLocationMatcherResult() } returns flowOf(locationMatcherResult)
        }
        val locationViewModel = mockk<LocationViewModel> {
            every { state } returns MutableStateFlow(locationMatcherResult)
        }

        RoadNameLabelComponent(roadNameView, locationViewModel, style)
            .onAttached(mapboxNavigation)

        verify { roadNameView.renderRoadName(mockRoad) }
        unmockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
    }

    @Test
    fun `onAttached renders road shields`() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
        val routeProgress = mockk<RouteProgress>()
        val mockRoad = mockk<Road> {
            every { components } returns listOf(mockk())
        }
        val locationMatcherResult = mockk<LocationMatcherResult> {
            every { road } returns mockRoad
        }
        val mapboxNavigation = mockk<MapboxNavigation> {
            every { flowLocationMatcherResult() } returns flowOf(locationMatcherResult)
            every { flowRouteProgress() } returns flowOf(routeProgress)
            every { navigationOptions } returns mockk {
                every { accessToken } returns ""
            }
        }
        val shields: List<Expected<RouteShieldError, RouteShieldResult>> = listOf()
        val shieldsCallbackSlot = slot<RouteShieldCallback>()
        val roadNameView = mockk<MapboxRoadNameView>(relaxed = true)
        val routeShieldApi = mockk<MapboxRouteShieldApi> {
            every {
                getRouteShields(
                    mockRoad,
                    DirectionsCriteria.PROFILE_DEFAULT_USER,
                    NavigationStyles.NAVIGATION_DAY_STYLE_ID,
                    any<String>(),
                    capture(shieldsCallbackSlot)
                )
            } answers {
                shieldsCallbackSlot.captured.onRoadShields(shields)
            }
        }
        val locationViewModel = mockk<LocationViewModel> {
            every { state } returns MutableStateFlow(locationMatcherResult)
        }

        RoadNameLabelComponent(
            roadNameView,
            locationViewModel,
            style,
            routeShieldApi
        ).onAttached(mapboxNavigation)

        verify { roadNameView.renderRoadNameWith(shields) }
        unmockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
    }
}
