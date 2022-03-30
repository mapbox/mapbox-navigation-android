package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun clear() = coroutineRule.runBlockingTest {
        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any())
        } answers {
            val routesCount =
                JSONObject(this.firstArg<String>())
                    .getJSONArray("routes")
                    .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeId } returns "$it"
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }

        val vanishingRouteLine = VanishingRouteLine().also {
            it.initWithRoute(loadRoute("short_route.json").toNavigationRoute())
        }
        assertNotNull(vanishingRouteLine.primaryRoutePoints)
        assertNotNull(vanishingRouteLine.primaryRouteLineGranularDistances)

        vanishingRouteLine.clear()

        assertNull(vanishingRouteLine.primaryRoutePoints)
        assertNull(vanishingRouteLine.primaryRouteLineGranularDistances)

        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun cancel() {
        val mockParentJob = mockk<CompletableJob>(relaxed = true)
        val mockJobControl = mockk<JobControl> {
            every { job } returns mockParentJob
        }
        every { InternalJobControlFactory.createDefaultScopeJobControl() } returns mockJobControl

        VanishingRouteLine().cancel()

        verify { mockParentJob.cancelChildren() }
    }
}
