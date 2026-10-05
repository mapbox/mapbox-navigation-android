package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.Screen
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxJunctionApi
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Observe [MapboxCarMapSurface] and [MapboxNavigation] properties that create the
 * [NavigationTemplate.NavigationInfo].
 */
class CarNavigationInfoProviderTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val carNavigationEtaMapper: CarNavigationEtaMapper = mockk(relaxed = true)
    private val carNavigationInfoMapper: CarNavigationInfoMapper = mockk(relaxed = true)
    private val maneuverApi: MapboxManeuverApi = mockk(relaxed = true)
    private val junctionApi: MapboxJunctionApi = mockk(relaxed = true)
    private val serviceProvider: CarNavigationInfoServices = mockk {
        every { carNavigationEtaMapper(any()) } returns carNavigationEtaMapper
        every { carNavigationInfoMapper(any(), any()) } returns carNavigationInfoMapper
        every { maneuverApi(any()) } returns maneuverApi
        every { mapUserStyleObserver() } returns mockk(relaxed = true)
        every { junctionApi() } returns junctionApi
    }

    private val sut = CarNavigationInfoProvider(serviceProvider)

    @Test
    fun `navigationInfo is null by default`() {
        assertNull(sut.carNavigationInfo.value.navigationInfo)
    }

    @Test
    fun `travelEstimate is null by default`() {
        assertNull(sut.carNavigationInfo.value.destinationTravelEstimate)
    }

    @Test
    fun `navigationInfo is available after route progress`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
            every { registerBannerInstructionsObserver(any()) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))

        assertNotNull(sut.carNavigationInfo.value.navigationInfo)
    }

    @Test
    fun `navigationInfo is null when mapbox navigation is detached`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))
        carAppTestRule.onDetached(mapboxNavigation)

        assertNull(sut.carNavigationInfo.value.navigationInfo)
    }

    @Test
    fun `junctionView is available before route progress`() = runBlockingTest {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val junctionValue = mockk<JunctionValue>(relaxed = true)
        val progressObserver = slot<RouteProgressObserver>()
        val instrObserver = slot<BannerInstructionsObserver>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
            every { registerBannerInstructionsObserver(capture(instrObserver)) } just runs
        }
        every { junctionApi.generateJunction(any(), any()) } answers {
            secondArg<MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>>()
                .accept(ExpectedFactory.createValue(junctionValue))
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        instrObserver.captured.onNewBannerInstructions(mockk(relaxed = true))
        progressObserver.captured.onRouteProgressChanged(routeProgress)

        verify {
            carNavigationInfoMapper.mapNavigationInfo(any(), any(), routeProgress, junctionValue)
        }
        assertNotNull(sut.carNavigationInfo.value.navigationInfo)
    }

    @Test
    fun `late road shields render the latest route progress`() {
        val observerSlot = slot<RouteProgressObserver>()
        val shieldCallbacks = mutableListOf<RouteShieldCallback>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
            every { registerBannerInstructionsObserver(any()) } just runs
        }
        every { maneuverApi.getManeuvers(any<RouteProgress>()) } returns
            ExpectedFactory.createValue(listOf(mockk(relaxed = true)))
        every {
            maneuverApi.getRoadShields(any(), any(), any(), capture(shieldCallbacks))
        } just runs
        val firstProgress = mockk<RouteProgress>(relaxed = true)
        val secondProgress = mockk<RouteProgress>(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mockk(relaxed = true))
        observerSlot.captured.onRouteProgressChanged(firstProgress)
        observerSlot.captured.onRouteProgressChanged(secondProgress)
        shieldCallbacks.first().onRoadShields(listOf(shieldResult()))

        verify(exactly = 2) {
            carNavigationInfoMapper.mapNavigationInfo(any(), any(), secondProgress, any())
        }
        verify(exactly = 1) {
            carNavigationInfoMapper.mapNavigationInfo(any(), any(), firstProgress, any())
        }
    }

    @Test
    fun `road shields requested before detaching do not render the old route progress`() {
        val observerSlot = slot<RouteProgressObserver>()
        val shieldCallbacks = mutableListOf<RouteShieldCallback>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        every { maneuverApi.getManeuvers(any<RouteProgress>()) } returns
            ExpectedFactory.createValue(listOf(mockk(relaxed = true)))
        every {
            maneuverApi.getRoadShields(any(), any(), any(), capture(shieldCallbacks))
        } just runs
        val progressBeforeDetach = mockk<RouteProgress>(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mockk(relaxed = true))
        observerSlot.captured.onRouteProgressChanged(progressBeforeDetach)
        carAppTestRule.onDetached(mapboxNavigation)
        carAppTestRule.onAttached(mapboxNavigation)
        // The request made before detaching completes once navigation is attached again.
        shieldCallbacks.first().onRoadShields(listOf(shieldResult()))

        verify(exactly = 1) {
            carNavigationInfoMapper.mapNavigationInfo(any(), any(), progressBeforeDetach, any())
        }
        assertNull(sut.carNavigationInfo.value.navigationInfo)
    }

    @Test
    fun `road shields are cleared when navigation is detached`() {
        val observerSlot = slot<RouteProgressObserver>()
        val shieldCallback = slot<RouteShieldCallback>()
        val shield = mockk<RouteShield>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        every { maneuverApi.getManeuvers(any<RouteProgress>()) } returns
            ExpectedFactory.createValue(listOf(mockk(relaxed = true)))
        every {
            maneuverApi.getRoadShields(any(), any(), any(), capture(shieldCallback))
        } just runs

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mockk(relaxed = true))
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))
        shieldCallback.captured.onRoadShields(listOf(shieldResult(shield)))
        carAppTestRule.onDetached(mapboxNavigation)
        carAppTestRule.onAttached(mapboxNavigation)
        val progressAfterReattach = mockk<RouteProgress>(relaxed = true)
        observerSlot.captured.onRouteProgressChanged(progressAfterReattach)

        verify {
            carNavigationInfoMapper.mapNavigationInfo(
                any(),
                emptyList(),
                progressAfterReattach,
                any(),
            )
        }
    }

    private fun shieldResult(
        shield: RouteShield = mockk(),
    ): Expected<RouteShieldError, RouteShieldResult> =
        ExpectedFactory.createValue(mockk { every { this@mockk.shield } returns shield })

    @Test
    fun `travelEstimate is available after route progress`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
            every { registerBannerInstructionsObserver(any()) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))

        assertNotNull(sut.carNavigationInfo.value.destinationTravelEstimate)
    }

    @Test
    fun `travelEstimate is null when mapbox navigation is detached`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))
        carAppTestRule.onDetached(mapboxNavigation)

        assertNull(sut.carNavigationInfo.value.destinationTravelEstimate)
    }

    @Test
    fun `invalidateOnChange will invalidate screen when data changes`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)
        val testLifecycleOwner = TestLifecycleOwner()
        val screen: Screen = mockk {
            every { invalidate() } just runs
            every { lifecycle } returns testLifecycleOwner.lifecycle
        }

        sut.invalidateOnChange(screen)
        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))

        verify { screen.invalidate() }
    }

    @Test
    fun `invalidateOnChange will not invalidate screen during initialization`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)
        val testLifecycleOwner = TestLifecycleOwner()
        val screen: Screen = mockk {
            every { invalidate() } just runs
            every { lifecycle } returns testLifecycleOwner.lifecycle
        }

        sut.invalidateOnChange(screen)
        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)

        verify(exactly = 0) { screen.invalidate() }
    }

    @Test
    fun `setNavigationInfo will update the builder with navigation info`() {
        val observerSlot = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(observerSlot)) } just runs
        }
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)
        val navigationTemplateBuilder: NavigationTemplate.Builder = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        observerSlot.captured.onRouteProgressChanged(mockk(relaxed = true))
        sut.setNavigationInfo(navigationTemplateBuilder)

        verify { navigationTemplateBuilder.setNavigationInfo(any()) }
        verify { navigationTemplateBuilder.setDestinationTravelEstimate(any()) }
    }

    @Test
    fun `maneuverApi is canceled when navigation is detached`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        carAppTestRule.onDetached(mapboxNavigation)

        verify { maneuverApi.cancel() }
    }

    @Test
    fun `services are available when navigation is attached`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)

        assertNotNull(sut.navigationInfoMapper)
        assertNotNull(sut.navigationEtaMapper)
        assertNotNull(sut.maneuverApi)
    }

    @Test
    fun `services are null when navigation is detached`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        carAppTestRule.onDetached(mapboxNavigation)

        assertNull(sut.navigationInfoMapper)
        assertNull(sut.navigationEtaMapper)
        assertNull(sut.maneuverApi)
    }

    @Test
    fun `carContext is available when map is attached`() {
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        sut.onAttached(mapboxCarMapSurface)

        assertNotNull(sut.carContext)
    }

    @Test
    fun `carContext is not available when map is detached`() {
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        sut.onAttached(mapboxCarMapSurface)
        sut.onDetached(mapboxCarMapSurface)

        assertNull(sut.carContext)
    }
}
