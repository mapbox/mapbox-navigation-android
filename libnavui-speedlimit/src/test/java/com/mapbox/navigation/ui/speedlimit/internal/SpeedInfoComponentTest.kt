package com.mapbox.navigation.ui.speedlimit.internal

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedInfoView
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
        every { navigationOptions } returns mockk {
            every { accessToken } returns "token"
        }
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
            val locationMatcher = mockk<LocationMatcherResult> {
                every { speedLimit } returns SpeedLimit(
                    speedKmph = 100,
                    speedLimitUnit = SpeedLimitUnit.KILOMETRES_PER_HOUR,
                    speedLimitSign = SpeedLimitSign.MUTCD
                )
            }
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
                speedInfoApi = speedInfoApi
            )
            speedInfoComponent.onAttached(mockNavigation)

            verify { speedInfoView.render(any()) }
        }
}
