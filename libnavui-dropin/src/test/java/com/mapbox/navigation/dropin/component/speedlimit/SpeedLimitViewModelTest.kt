package com.mapbox.navigation.dropin.component.speedlimit

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedLimitViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun consumeAction_UpdateNavigationState() = coroutineRule.runBlockingTest {
        val state = SpeedLimitAction.UpdateNavigationState(NavigationState.ActiveNavigation)
        val speedLimitApi = mockk<MapboxSpeedLimitApi>()
        val viewModel = SpeedLimitViewModel(speedLimitApi)
        val def = async {
            viewModel.speedLimitState().drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertTrue(viewModelResult.isVisible)
        assertEquals(NavigationState.ActiveNavigation, viewModelResult.navigationState)
        assertTrue(viewModelResult.speedLimit.isError)
    }

    @Test
    fun consumeAction_UpdateLocationMatcher() = coroutineRule.runBlockingTest {
        val mockSpeedLimit = SpeedLimit(
            55,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.VIENNA
        )
        val updateSpeedLimitValue = mockk<UpdateSpeedLimitValue> {
            every { speedKPH } returns 55
            every { speedUnit } returns SpeedLimitUnit.MILES_PER_HOUR
            every { signFormat } returns SpeedLimitSign.VIENNA
            every { speedLimitFormatter } returns mockk<SpeedLimitFormatter>()
        }
        val locationMatchResult = mockk<LocationMatcherResult> {
            every { speedLimit } returns mockSpeedLimit
        }
        val state = SpeedLimitAction.UpdateLocationMatcher(locationMatchResult)
        val speedLimitApi = mockk<MapboxSpeedLimitApi> {
            every {
                updateSpeedLimit(mockSpeedLimit)
            } returns ExpectedFactory.createValue(updateSpeedLimitValue)
        }
        val viewModel = SpeedLimitViewModel(speedLimitApi)
        val def = async {
            viewModel.speedLimitState().drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertEquals(55, viewModelResult.speedLimit.value!!.speedKPH)
        assertEquals(SpeedLimitSign.VIENNA, viewModelResult.speedLimit.value!!.signFormat)
        assertEquals(SpeedLimitUnit.MILES_PER_HOUR, viewModelResult.speedLimit.value!!.speedUnit)
    }
}
