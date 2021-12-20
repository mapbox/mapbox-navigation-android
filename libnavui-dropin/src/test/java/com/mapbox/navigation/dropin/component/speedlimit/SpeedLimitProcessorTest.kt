package com.mapbox.navigation.dropin.component.speedlimit

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedLimitProcessorTest {

    @Test
    fun processVisibility_navigationState_freeDrive() {
        val processVisibility =
            SpeedLimitProcessor.ProcessVisibility(NavigationState.FreeDrive).process()

        assertTrue(processVisibility.isVisible)
    }

    @Test
    fun processVisibility_navigationState_activeNavigation() {
        val processVisibility =
            SpeedLimitProcessor.ProcessVisibility(NavigationState.ActiveNavigation).process()

        assertTrue(processVisibility.isVisible)
    }

    @Test
    fun processVisibility_navigationState_arrival() {
        val processVisibility =
            SpeedLimitProcessor.ProcessVisibility(NavigationState.Arrival).process()

        assertTrue(processVisibility.isVisible)
    }

    @Test
    fun processVisibility_navigationState_empty() {
        val processVisibility =
            SpeedLimitProcessor.ProcessVisibility(NavigationState.Empty).process()

        assertFalse(processVisibility.isVisible)
    }

    @Test
    fun processVisibility_navigationState_routePreview() {
        val processVisibility =
            SpeedLimitProcessor.ProcessVisibility(NavigationState.RoutePreview).process()

        assertFalse(processVisibility.isVisible)
    }

    @Test
    fun processLocationMatcher() {
        val mockSpeedLimit = mockk<SpeedLimit>()
        val updateValue = mockk<UpdateSpeedLimitValue>()
        val locationMatcher = mockk<LocationMatcherResult> {
            every { speedLimit } returns mockSpeedLimit
        }
        val speedLimitApi = mockk<MapboxSpeedLimitApi> {
            every {
                updateSpeedLimit(mockSpeedLimit)
            } returns ExpectedFactory.createValue(updateValue)
        }
        val processLocationMatcher = SpeedLimitProcessor.ProcessLocationMatcher(
            locationMatcher,
            speedLimitApi
        )

        val result = processLocationMatcher.process().speedLimit.value

        assertEquals(updateValue, result)
    }
}
