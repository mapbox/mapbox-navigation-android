package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.internal.factory.EHorizonFactory
import com.mapbox.navigation.base.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManagerImpl
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.RoadObjectDistance
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectsStoreObserver
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private typealias SDKRoadObjectDistanceInfo =
    com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo

private typealias SDKRoadObjectEnterExitInfo =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEnterExitInfo

@ExperimentalCoroutinesApi
class EHorizonSubscriptionManagerTest {
    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val roadObjectEnterExitInfo: RoadObjectEnterExitInfo = mockk(relaxed = true)
    private val eHorizonObjectEnterExitInfo: SDKRoadObjectEnterExitInfo = mockk(relaxed = true)
    private val electronicHorizonPosition: ElectronicHorizonPosition = mockk(relaxed = true)
    private val eHorizonPosition: EHorizonPosition = mockk(relaxed = true)
    private val roadObjectDistance: RoadObjectDistance = mockk(relaxed = true)
    private val eHorizonObjectDistance: SDKRoadObjectDistanceInfo = mockk(relaxed = true)
    private val subscriptionManager: EHorizonSubscriptionManager =
        EHorizonSubscriptionManagerImpl(
            navigator,
            ThreadController(),
        )

    @Before
    fun setUp() {
        mockkObject(EHorizonFactory)
        every {
            EHorizonFactory.buildRoadObjectDistance(any())
        } returns eHorizonObjectDistance
        every {
            EHorizonFactory.buildEHorizonPosition(any())
        } coAnswers { eHorizonPosition }
        every {
            EHorizonFactory.buildRoadObjectEnterExitInfo(any())
        } coAnswers { eHorizonObjectEnterExitInfo }
    }

    @After
    fun cleanUp() {
        unmockkObject(EHorizonFactory)
        subscriptionManager.unregisterAllObservers()
    }

    @Test
    fun `when register EHorizonObserver observers added to navigator`() {
        subscriptionManager.registerObserver(mockk())

        verify(exactly = 1) { navigator.setElectronicHorizonObserver(any()) }
        verify(exactly = 1) { navigator.addRoadObjectsStoreObserver(any()) }
    }

    @Test
    fun `when register multiple EHorizonObservers observers added to navigator just once`() {
        subscriptionManager.registerObserver(mockk())
        subscriptionManager.registerObserver(mockk())

        verify(exactly = 1) { navigator.setElectronicHorizonObserver(any()) }
        verify(exactly = 1) { navigator.addRoadObjectsStoreObserver(any()) }
    }

    @Test
    fun `when unregister all EHorizonObservers nulls passed to navigator`() {
        subscriptionManager.unregisterAllObservers()

        verify(exactly = 1) { navigator.setElectronicHorizonObserver(null) }
        verify(exactly = 1) { navigator.removeRoadObjectsStoreObserver(any()) }
    }

    @Test
    fun `when register and unregister EHorizonObserver navigator method called`() {
        val eHorizonObserverSlot = CapturingSlot<ElectronicHorizonObserver>()
        val roadObjectsStoreObserverSlot = CapturingSlot<RoadObjectsStoreObserver>()
        every { navigator.setElectronicHorizonObserver(capture(eHorizonObserverSlot)) } just Runs
        every { navigator.addRoadObjectsStoreObserver(capture(roadObjectsStoreObserverSlot)) } just
            Runs
        val observer: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(observer)
        subscriptionManager.unregisterObserver(observer)

        verify(exactly = 1) {
            navigator.setElectronicHorizonObserver(eHorizonObserverSlot.captured)
        }
        verify(exactly = 1) {
            navigator.addRoadObjectsStoreObserver(roadObjectsStoreObserverSlot.captured)
        }
        verify(exactly = 1) { navigator.setElectronicHorizonObserver(null) }
        verify(exactly = 1) {
            navigator.removeRoadObjectsStoreObserver(roadObjectsStoreObserverSlot.captured)
        }
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

        val list = mutableListOf<RoadObjectDistance>()
        list.add(roadObjectDistance)
        val expectedList = mutableListOf<SDKRoadObjectDistanceInfo>()
        expectedList.add(eHorizonObjectDistance)

        eHorizonObserverSlot.captured.onPositionUpdated(electronicHorizonPosition, list)

        verify(exactly = 1) { firstObserver.onPositionUpdated(eHorizonPosition, expectedList) }
        verify(exactly = 1) { secondObserver.onPositionUpdated(eHorizonPosition, expectedList) }
        verify(exactly = 1) { thirdObserver.onPositionUpdated(eHorizonPosition, expectedList) }
    }

    @Test
    fun `onRoadObjectAdded is called for all observers`() = runBlockingTest {
        val roadObjectsStoreObserver = CapturingSlot<RoadObjectsStoreObserver>()
        every { navigator.addRoadObjectsStoreObserver(capture(roadObjectsStoreObserver)) } just Runs
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
        every { navigator.addRoadObjectsStoreObserver(capture(roadObjectsStoreObserver)) } just Runs
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
        every { navigator.addRoadObjectsStoreObserver(capture(roadObjectsStoreObserver)) } just Runs
        val firstObserver: EHorizonObserver = mockk(relaxed = true)
        val secondObserver: EHorizonObserver = mockk(relaxed = true)

        subscriptionManager.registerObserver(firstObserver)
        subscriptionManager.registerObserver(secondObserver)

        roadObjectsStoreObserver.captured.onRoadObjectRemoved(ROAD_OBJECT_ID)

        verify(exactly = 1) { firstObserver.onRoadObjectRemoved(ROAD_OBJECT_ID) }
        verify(exactly = 1) { secondObserver.onRoadObjectRemoved(ROAD_OBJECT_ID) }
    }

    private companion object {
        private const val ROAD_OBJECT_ID = "road_object_id"
    }
}
