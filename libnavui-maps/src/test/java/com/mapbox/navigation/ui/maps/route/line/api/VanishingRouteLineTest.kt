package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VanishingRouteLineTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
    private lateinit var testJobControl: JobControl

    @Before
    fun setUp() {
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)
        testJobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    }

    @After
    fun cleanUp() {
        unmockkObject(InternalJobControlFactory)
    }

    @Test
    fun updateVanishingPointState_when_LOCATION_TRACKING() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.TRACKING)
        }

        assertEquals(VanishingPointState.ENABLED, vanishingRouteLine.vanishingPointState)
    }

    @Test
    fun updateVanishingPointState_when_ROUTE_COMPLETE() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.COMPLETE)
        }

        assertEquals(
            VanishingPointState.ONLY_INCREASE_PROGRESS,
            vanishingRouteLine.vanishingPointState
        )
    }

    @Test
    fun updateVanishingPointState_when_other() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.OFF_ROUTE)
        }

        assertEquals(VanishingPointState.DISABLED, vanishingRouteLine.vanishingPointState)
    }
}
