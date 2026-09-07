package com.mapbox.navigation.ui.components.speedlimit.internal

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.components.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.components.speedlimit.view.MapboxSpeedInfoView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SpeedInfoComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val speedInfoView = mockk<MapboxSpeedInfoView>(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation> {
        every { navigationOptions } returns mockk()
    }

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
    }

    @Test
    fun `speed limit is rendered when location matcher results are available`() =
        coroutineRule.runBlockingTest {
            val locationMatcher = mockk<LocationMatcherResult>()
            every { mockNavigation.flowLocationMatcherResult() } returns flowOf(locationMatcher)
            every {
                mockNavigation.registerLocationObserver(any())
            } answers {
                firstArg<LocationObserver>().onNewLocationMatcherResult(locationMatcher)
            }
            val distanceFormatterOptions = mockk<DistanceFormatterOptions>(relaxed = true)
            val speedInfoApi = mockk<MapboxSpeedInfoApi>(relaxed = true) {
                every {
                    updatePostedAndCurrentSpeed(locationMatcher, distanceFormatterOptions)
                } returns mockk()
            }
            val speedInfoComponent = SpeedInfoComponent(
                speedInfoOptions = MapboxSpeedInfoOptions.Builder().build(),
                speedInfoView = speedInfoView,
                distanceFormatterOptions = distanceFormatterOptions,
                speedInfoApi = speedInfoApi,
            )
            speedInfoComponent.onAttached(mockNavigation)

            verify { speedInfoView.render(any()) }
        }

    @Test
    fun `speed limit is not rendered when value is null`() =
        coroutineRule.runBlockingTest {
            val locationMatcher = mockk<LocationMatcherResult>()
            every { mockNavigation.flowLocationMatcherResult() } returns flowOf(locationMatcher)
            every {
                mockNavigation.registerLocationObserver(any())
            } answers {
                firstArg<LocationObserver>().onNewLocationMatcherResult(locationMatcher)
            }
            val distanceFormatterOptions = mockk<DistanceFormatterOptions>(relaxed = true)
            val speedInfoApi = mockk<MapboxSpeedInfoApi>(relaxed = true) {
                every { updatePostedAndCurrentSpeed(any(), any(), any()) } returns null
            }
            val speedInfoComponent = SpeedInfoComponent(
                speedInfoOptions = MapboxSpeedInfoOptions.Builder().build(),
                speedInfoView = speedInfoView,
                distanceFormatterOptions = distanceFormatterOptions,
                speedInfoApi = speedInfoApi,
            )
            speedInfoComponent.onAttached(mockNavigation)

            verify(exactly = 0) { speedInfoView.render(any()) }
        }
}
