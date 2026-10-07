package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.Screen
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.TravelEstimate
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
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import java.util.TimeZone

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
    private val maneuverApi: MapboxManeuverApi = mockk(relaxed = true) {
        every { getManeuvers(any<RouteProgress>()) } returns
            ExpectedFactory.createValue(emptyList())
    }
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

    @Test
    fun `route progress without a visible change does not publish new navigation info`() {
        val progressObserver = attachForDisplayChecks()

        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        // 590 s still shows as 10 min, and the arrival time is in the same minute.
        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 590, arrivalMs = 10_000)

        assertSame(published, sut.carNavigationInfo.value)
    }

    @Test
    fun `a new maneuver distance publishes new navigation info`() {
        val progressObserver = attachForDisplayChecks()

        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        displayTick(progressObserver, stepMeters = 50.0, remainingSeconds = 600, arrivalMs = 0)

        assertNotSame(published, sut.carNavigationInfo.value)
    }

    @Test
    fun `a new remaining minute publishes new navigation info`() {
        val progressObserver = attachForDisplayChecks()

        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 540, arrivalMs = 0)

        assertNotSame(published, sut.carNavigationInfo.value)
    }

    @Test
    fun `a new arrival minute publishes new navigation info`() {
        val progressObserver = attachForDisplayChecks()

        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 60_000)

        assertNotSame(published, sut.carNavigationInfo.value)
    }

    @Test
    fun `a new step publishes new navigation info`() {
        val progressObserver = attachForDisplayChecks()

        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        displayTick(
            progressObserver,
            stepMeters = 100.0,
            remainingSeconds = 600,
            arrivalMs = 0,
            stepIndex = 1,
        )

        assertNotSame(published, sut.carNavigationInfo.value)
    }

    @Test
    fun `navigation info is published again after navigation is attached again`() {
        val progressObserver = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
        }
        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mockk(relaxed = true))

        displayTick(progressObserver.captured, 100.0, remainingSeconds = 600, arrivalMs = 0)
        carAppTestRule.onDetached(mapboxNavigation)
        carAppTestRule.onAttached(mapboxNavigation)
        displayTick(progressObserver.captured, 100.0, remainingSeconds = 600, arrivalMs = 0)

        assertNotNull(sut.carNavigationInfo.value.navigationInfo)
    }

    @Test
    fun `invalidateOnChange does not invalidate the screen without a visible change`() {
        val testLifecycleOwner = TestLifecycleOwner()
        val screen: Screen = mockk {
            every { invalidate() } just runs
            every { lifecycle } returns testLifecycleOwner.lifecycle
        }
        sut.invalidateOnChange(screen)
        val progressObserver = attachForDisplayChecks()

        repeat(10) { second ->
            displayTick(
                progressObserver,
                stepMeters = 100.0,
                remainingSeconds = 600L - second,
                arrivalMs = second * 1_000L,
            )
        }

        verify(exactly = 1) { screen.invalidate() }
    }

    @Test
    fun `a new junction view publishes new navigation info`() {
        val progressObserver = slot<RouteProgressObserver>()
        val bannerObserver = slot<BannerInstructionsObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
            every { registerBannerInstructionsObserver(capture(bannerObserver)) } just runs
        }
        every { junctionApi.generateJunction(any(), any()) } answers {
            secondArg<MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>>()
                .accept(ExpectedFactory.createValue(mockk(relaxed = true)))
        }
        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mockk(relaxed = true))

        displayTick(progressObserver.captured, 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        bannerObserver.captured.onNewBannerInstructions(mockk(relaxed = true))
        displayTick(progressObserver.captured, 100.0, remainingSeconds = 600, arrivalMs = 0)

        assertNotSame(published, sut.carNavigationInfo.value)
    }

    @Test
    fun `new road shields publish new navigation info`() {
        val shieldCallback = slot<RouteShieldCallback>()
        every { maneuverApi.getManeuvers(any<RouteProgress>()) } returns
            ExpectedFactory.createValue(listOf(mockk(relaxed = true)))
        every {
            maneuverApi.getRoadShields(any(), any(), any(), capture(shieldCallback))
        } just runs
        val progressObserver = attachForDisplayChecks()

        displayTick(progressObserver, stepMeters = 100.0, remainingSeconds = 600, arrivalMs = 0)
        val published = sut.carNavigationInfo.value
        shieldCallback.captured.onRoadShields(listOf(shieldResult()))

        assertNotSame(published, sut.carNavigationInfo.value)
    }

    private fun attachForDisplayChecks(): RouteProgressObserver {
        val progressObserver = slot<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
        }
        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mockk(relaxed = true))
        return progressObserver.captured
    }

    private fun displayTick(
        progressObserver: RouteProgressObserver,
        stepMeters: Double,
        remainingSeconds: Long,
        arrivalMs: Long,
        stepIndex: Int = 0,
    ) {
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns mockk(relaxed = true) {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk(relaxed = true) {
                    every { this@mockk.stepIndex } returns stepIndex
                }
            }
        }
        every {
            carNavigationInfoMapper.mapNavigationInfo(any(), any(), routeProgress, any())
        } returns mockk<RoutingInfo> {
            every { currentDistance } returns Distance.create(stepMeters, Distance.UNIT_METERS)
        }
        every { carNavigationEtaMapper.getDestinationTravelEstimate(routeProgress) } returns
            TravelEstimate.Builder(
                Distance.create(10.0, Distance.UNIT_KILOMETERS),
                DateTimeWithZone.create(arrivalMs, TimeZone.getTimeZone("UTC")),
            ).setRemainingTimeSeconds(remainingSeconds).build()
        progressObserver.onRouteProgressChanged(routeProgress)
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
