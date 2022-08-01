package com.mapbox.navigation.ui.speedlimit.internal

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitError
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
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
@ExperimentalPreviewMapboxNavigationAPI
class SpeedLimitComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val speedLimitView = mockk<MapboxSpeedLimitView>(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation> {
        every { navigationOptions } returns mockk {
            every { accessToken } returns "token"
        }
    }

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationEx")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationEx")
    }

    @Test
    fun `speed limit is rendered when location matcher results are available`() =
        coroutineRule.runBlockingTest {
            val locationMatcher = mockk<LocationMatcherResult> {
                every { speedLimit } returns SpeedLimit(
                    speedKmph = 120,
                    speedLimitUnit = SpeedLimitUnit.MILES_PER_HOUR,
                    speedLimitSign = SpeedLimitSign.MUTCD
                )
            }
            every { mockNavigation.flowLocationMatcherResult() } returns flowOf(locationMatcher)
            every {
                mockNavigation.registerLocationObserver(any())
            } answers {
                firstArg<LocationObserver>().onNewLocationMatcherResult(locationMatcher)
            }
            val expected = ExpectedFactory
                .createValue<UpdateSpeedLimitError, UpdateSpeedLimitValue>(
                    UpdateSpeedLimitValue(
                        locationMatcher.speedLimit?.speedKmph!!,
                        locationMatcher.speedLimit?.speedLimitUnit!!,
                        locationMatcher.speedLimit?.speedLimitSign!!,
                        mockk()
                    )
                )
            val speedLimitApi = mockk<MapboxSpeedLimitApi>(relaxed = true) {
                every { updateSpeedLimit(locationMatcher.speedLimit) } returns expected
            }
            val speedLimitComponent = SpeedLimitComponent(
                style = 1,
                textAppearance = 1,
                speedLimitView = speedLimitView,
                speedLimitApi = speedLimitApi
            )
            speedLimitComponent.onAttached(mockNavigation)

            verify { speedLimitView.render(expected) }
        }

    @Test
    fun `speed limit is not rendered when speed is not available`() =
        coroutineRule.runBlockingTest {
            val locationMatcher = mockk<LocationMatcherResult> {
                every { speedLimit } returns null
            }
            every { mockNavigation.flowLocationMatcherResult() } returns flowOf(locationMatcher)
            every {
                mockNavigation.registerLocationObserver(any())
            } answers {
                firstArg<LocationObserver>().onNewLocationMatcherResult(locationMatcher)
            }
            val expected = ExpectedFactory
                .createError<UpdateSpeedLimitError, UpdateSpeedLimitValue>(
                    UpdateSpeedLimitError("Speed Limit data not available", null)
                )
            val speedLimitApi = mockk<MapboxSpeedLimitApi>(relaxed = true) {
                every { updateSpeedLimit(locationMatcher.speedLimit) } returns expected
            }
            val speedLimitComponent = SpeedLimitComponent(
                style = 1,
                textAppearance = 1,
                speedLimitView = speedLimitView,
                speedLimitApi = speedLimitApi
            )
            speedLimitComponent.onAttached(mockNavigation)

            verify { speedLimitView.render(expected) }
        }
}
