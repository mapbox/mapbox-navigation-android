package com.mapbox.navigation.dropin.component.tripprogress

import android.os.Build
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.internal.extensions.flowRouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
@ExperimentalPreviewMapboxNavigationAPI
@ExperimentalCoroutinesApi
class TripProgressComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `onAttached renders location matcher results`() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.navigation.dropin.internal.extensions.MapboxNavigationEx")
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 600.0
            every { distanceRemaining } returns 100f
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 2.0
            }
            every { distanceTraveled } returns 50f
            every { route } returns mockk {
                every { currentState } returns RouteProgressState.TRACKING
            }
        }
        val tripProgressView = mockk<MapboxTripProgressView>(relaxed = true)
        val mapboxNavigation = mockk<MapboxNavigation> {
            every { flowRouteProgress() } returns flowOf(routeProgress)
        }

        TripProgressComponent(tripProgressView).onAttached(mapboxNavigation)

        verify { tripProgressView.render(any()) }
        unmockkStatic("com.mapbox.navigation.dropin.internal.extensions.MapboxNavigationEx")
    }
}
