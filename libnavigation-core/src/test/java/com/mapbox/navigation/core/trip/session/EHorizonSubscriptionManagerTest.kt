package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectEnterExitInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectEnterExitInfo
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonPosition
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.RoadObjectDistanceInfo
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectsStoreObserver
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.HashMap

@ExperimentalCoroutinesApi
class EHorizonSubscriptionManagerTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val roadObjectEnterExitInfo: RoadObjectEnterExitInfo = mockk(relaxed = true)
    private val eHorizonObjectEnterExitInfo: EHorizonObjectEnterExitInfo = mockk(relaxed = true)
    private val electronicHorizonPosition: ElectronicHorizonPosition = mockk(relaxed = true)
    private val eHorizonPosition: EHorizonPosition = mockk(relaxed = true)
    private val roadObjectDistanceInfo: RoadObjectDistanceInfo = mockk(relaxed = true)
    private val eHorizonObjectDistanceInfo: EHorizonObjectDistanceInfo = mockk(relaxed = true)
    private val subscriptionManager: EHorizonSubscriptionManager =
        EHorizonSubscriptionManagerImpl(navigator)

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        mockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapper")
        mockkStatic("com.mapbox.navigation.core.trip.model.eh.EHorizonMapper")
        coEvery { roadObjectEnterExitInfo.mapToEHorizonObjectEnterExitInfo() } coAnswers
            { eHorizonObjectEnterExitInfo }
        coEvery { electronicHorizonPosition.mapToEHorizonPosition() } coAnswers
            { eHorizonPosition }
        coEvery { roadObjectDistanceInfo.mapToEHorizonObjectDistanceInfo() } coAnswers
            { eHorizonObjectDistanceInfo }
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
        unmockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapper")
        unmockkStatic("com.mapbox.navigation.core.trip.model.eh.EHorizonMapper")
        subscriptionManager.unregisterAllObservers()
    }

    @Test
    fun `when register EHorizonObserver observers added to navigator`() {
        subscriptionManager.registerObserver(mockk())

        verify(exactly = 1) { navigator.setElectronicHorizonObserver(any()) }
        verify(exactly = 1) { navigator.setRoadObjectsStoreObserver(any()) }
    }

    @Test
    fun `when register multiple EHorizonObservers observers added to navigator just once`() {
        subscriptionManager.registerObserver(mockk())
        subscriptionManager.registerObserver(mockk())

        verify(exactly = 1) { navigator.setElectronicHorizonObserver(any()) }
        verify(exactly = 1) { navigator.setRoadObjectsStoreObserver(any()) }
    }

    @Test
    fun `when unregister all EHorizonObservers nulls passed to navigator`() {
        subscriptionManager.unregisterAllObservers()

        verify(exactly = 1) { navigator.setElectronicHorizonObserver(null) }
        verify(exactly = 1) { navigator.setRoadObjectsStoreObserver(null) }
    }

    @Test
    fun `when register and unregister EHorizonObserver navigator method called`() {
        val eHorizonObserverSlot = CapturingSlot<ElectronicHorizonObserver>()
        val roadObjectsStoreObserverSlot = CapturingSlot<RoadObjectsStoreObserver>()
        every { navigator.setElectronicHorizonObserver(capture(eHorizonObserverSlot)) } just Runs
        every { navigator.setRoadObjectsStoreObserver(capture(roadObjectsStoreObserverSlot)) } just
            Runs
        val observer: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(observer)
        subscriptionManager.unregisterObserver(observer)

        verify(exactly = 1) {
            navigator.setElectronicHorizonObserver(eHorizonObserverSlot.captured)
        }
        verify(exactly = 1) {
            navigator.setRoadObjectsStoreObserver(roadObjectsStoreObserverSlot.captured)
        }
        verify(exactly = 1) { navigator.setElectronicHorizonObserver(null) }
        verify(exactly = 1) { navigator.setRoadObjectsStoreObserver(null) }
    }

    @Test
    fun `onRoadObjectEnter is called for all observers`() = runBlockingTest {
        val eHorizonObserverSlot = CapturingSlot<ElectronicHorizonObserver>()
        every { navigator.setElectronicHorizonObserver(capture(eHorizonObserverSlot)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)

        eHorizonObserverSlot.captured.onRoadObjectEnter(roadObjectEnterExitInfo)

        verify(exactly = 1) { firstObserver.onRoadObjectEnter(eHorizonObjectEnterExitInfo) }
        verify(exactly = 1) { secondObserver.onRoadObjectEnter(eHorizonObjectEnterExitInfo) }
    }

    @Test
    fun `onRoadObjectExit is called for all observers`() = runBlockingTest {
        val eHorizonObserverSlot = CapturingSlot<ElectronicHorizonObserver>()
        every { navigator.setElectronicHorizonObserver(capture(eHorizonObserverSlot)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)
        val thirdObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)
        subscriptionManager.registerObserver(thirdObserver)

        eHorizonObserverSlot.captured.onRoadObjectExit(roadObjectEnterExitInfo)

        verify(exactly = 1) { firstObserver.onRoadObjectExit(eHorizonObjectEnterExitInfo) }
        verify(exactly = 1) { secondObserver.onRoadObjectExit(eHorizonObjectEnterExitInfo) }
        verify(exactly = 1) { thirdObserver.onRoadObjectExit(eHorizonObjectEnterExitInfo) }
    }

    @Test
    fun `onPositionUpdated is called for all observers`() = runBlockingTest {
        val eHorizonObserverSlot = CapturingSlot<ElectronicHorizonObserver>()
        every { navigator.setElectronicHorizonObserver(capture(eHorizonObserverSlot)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)
        val thirdObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)
        subscriptionManager.registerObserver(thirdObserver)

        val map = HashMap<String, RoadObjectDistanceInfo>()
        map[POSITION_DISTANCE] = roadObjectDistanceInfo
        val expectedMap = HashMap<String, EHorizonObjectDistanceInfo>()
        expectedMap[POSITION_DISTANCE] = eHorizonObjectDistanceInfo

        eHorizonObserverSlot.captured.onPositionUpdated(electronicHorizonPosition, map)

        verify(exactly = 1) { firstObserver.onPositionUpdated(eHorizonPosition, expectedMap) }
        verify(exactly = 1) { secondObserver.onPositionUpdated(eHorizonPosition, expectedMap) }
        verify(exactly = 1) { thirdObserver.onPositionUpdated(eHorizonPosition, expectedMap) }
    }

    @Test
    fun `onRoadObjectAdded is called for all observers`() = runBlockingTest {
        val roadObjectsStoreObserver = CapturingSlot<RoadObjectsStoreObserver>()
        every { navigator.setRoadObjectsStoreObserver(capture(roadObjectsStoreObserver)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)

        roadObjectsStoreObserver.captured.onRoadObjectAdded(ROAD_OBJECT_ID)

        verify(exactly = 1) { firstObserver.onRoadObjectAdded(ROAD_OBJECT_ID) }
        verify(exactly = 1) { secondObserver.onRoadObjectAdded(ROAD_OBJECT_ID) }
    }

    @Test
    fun `onRoadObjectUpdated is called for all observers`() = runBlockingTest {
        val roadObjectsStoreObserver = CapturingSlot<RoadObjectsStoreObserver>()
        every { navigator.setRoadObjectsStoreObserver(capture(roadObjectsStoreObserver)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)

        roadObjectsStoreObserver.captured.onRoadObjectUpdated(ROAD_OBJECT_ID)

        verify(exactly = 1) { firstObserver.onRoadObjectUpdated(ROAD_OBJECT_ID) }
        verify(exactly = 1) { secondObserver.onRoadObjectUpdated(ROAD_OBJECT_ID) }
    }

    @Test
    fun `onRoadObjectRemoved is called for all observers`() = runBlockingTest {
        val roadObjectsStoreObserver = CapturingSlot<RoadObjectsStoreObserver>()
        every { navigator.setRoadObjectsStoreObserver(capture(roadObjectsStoreObserver)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)

        roadObjectsStoreObserver.captured.onRoadObjectRemoved(ROAD_OBJECT_ID)

        verify(exactly = 1) { firstObserver.onRoadObjectRemoved(ROAD_OBJECT_ID) }
        verify(exactly = 1) { secondObserver.onRoadObjectRemoved(ROAD_OBJECT_ID) }
    }

    private companion object {
        private const val POSITION_DISTANCE = "position_distance"
        private const val ROAD_OBJECT_ID = "road_object_id"
    }
}
