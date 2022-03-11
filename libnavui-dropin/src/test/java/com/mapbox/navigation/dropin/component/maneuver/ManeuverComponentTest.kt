package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowRouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.NavigationStyles
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
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManeuverComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
        every { mockNavigation.flowRouteProgress() } returns flowOf(routeProgress)
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
    }

    private val callbackSlot = slot<RouteShieldCallback>()
    private val maneuvers = listOf<Maneuver>()
    private val expectedManeuvers =
        ExpectedFactory.createValue<ManeuverError, List<Maneuver>>(maneuvers)
    private val mockManeuverApi = mockk<MapboxManeuverApi>(relaxed = true) {
        every { getManeuvers(any<RouteProgress>()) } returns expectedManeuvers
        every {
            getRoadShields(
                DirectionsCriteria.PROFILE_DEFAULT_USER,
                NavigationStyles.NAVIGATION_DAY_STYLE_ID,
                "token",
                maneuvers,
                capture(callbackSlot)
            )
        } returns Unit
    }
    private val maneuverView: MapboxManeuverView = mockk(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation>()
    private val routeProgress = mockk<RouteProgress>()

    @Test
    fun maneuversAreRendered() = coroutineRule.runBlockingTest {
        val maneuverComponent = ManeuverComponent(maneuverView, "token", mockManeuverApi)

        maneuverComponent.onAttached(mockNavigation)

        verify { maneuverView.renderManeuvers(expectedManeuvers) }
    }

    @Test
    fun roadShieldsAreRendered() {
        val expectedList: List<Expected<RouteShieldError, RouteShieldResult>> = listOf()
        val maneuverComponent = ManeuverComponent(maneuverView, "token", mockManeuverApi)

        maneuverComponent.onAttached(mockNavigation).also {
            callbackSlot.captured.onRoadShields(expectedList)
        }

        verify { maneuverView.renderManeuverWith(expectedList) }
    }
}
