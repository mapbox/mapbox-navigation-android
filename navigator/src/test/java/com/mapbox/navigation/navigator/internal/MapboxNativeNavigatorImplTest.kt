package com.mapbox.navigation.navigator.internal

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigator.AdasisFacadeHandleInterface
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.EventsMetadataInterface
import com.mapbox.navigator.InputsServiceHandle
import com.mapbox.navigator.NavigatorInterface
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RoadObjectMatcherConfig
import com.mapbox.navigator.RoadObjectsStoreInterface
import com.mapbox.navigator.TilesConfig
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(MapboxExperimental::class)
@RunWith(RobolectricTestRunner::class)
class MapboxNativeNavigatorImplTest {

    private val mockNavigator = mockk<NavigatorInterface>(relaxed = true)
    private val mockCacheHandle = mockk<CacheHandle>(relaxed = true)
    private val mockInputsService = mockk<InputsServiceHandle>(relaxed = true)
    private val mockAdasisFacade = mockk<AdasisFacadeHandleInterface>(relaxed = true)
    private val mockRoadObjectStore = mockk<RoadObjectsStoreInterface>(relaxed = true)

    private val loggingFrontend = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerFrontendTestRule = LoggingFrontendTestRule(loggingFrontend)

    private lateinit var navigatorImpl: MapboxNativeNavigatorImpl

    @Before
    fun setup() {
        every { mockNavigator.roadObjectsStore() } returns mockRoadObjectStore

        mockkObject(NavigatorLoader)

        every { NavigatorLoader.createInputService(any(), any()) } returns mockInputsService
        every { NavigatorLoader.createCacheHandle(any(), any(), any()) } returns mockCacheHandle
        every { NavigatorLoader.createAdasisFacade(any(), any(), any()) } returns mockAdasisFacade
        every { NavigatorLoader.createGraphAccessor(any()) } returns mockk(relaxed = true)
        every {
            NavigatorLoader.createRoadObjectMatcher(
                any(),
                any(),
            )
        } returns mockk(relaxed = true)
        every {
            NavigatorLoader.createNavigator(any(), any(), any(), any(), any(), any())
        } returns mockNavigator

        navigatorImpl = MapboxNativeNavigatorImpl(
            tilesConfig = mockk<TilesConfig>(relaxed = true),
            historyRecorderComposite = null,
            offlineCacheHandle = null,
            roadObjectMatcherConfig = mockk<RoadObjectMatcherConfig>(relaxed = true),
            config = mockk<ConfigHandle>(relaxed = true),
            eventsMetadataProvider = mockk<EventsMetadataInterface>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        unmockkObject(NavigatorLoader)
    }

    @Test
    fun `shutdown cleans up resources and calls shutdown`() {
        val navigatorObserver = mockk<NavigatorObserver>(relaxed = true)

        navigatorImpl.addNavigatorObserver(navigatorObserver)
        navigatorImpl.shutdown()

        verifyOrder {
            mockNavigator.reset(null)
            mockNavigator.setElectronicHorizonObserver(null)
            mockNavigator.setFallbackVersionsObserver(null)
            mockNavigator.removeObserver(navigatorObserver)
            mockRoadObjectStore.removeAllCustomRoadObjects()
            mockAdasisFacade.resetAdasisMessageCallback()
            mockNavigator.shutdown()
        }
    }

    @Test
    fun `shutdown called twice logs warning and does not call native shutdown again`() {
        navigatorImpl.shutdown()
        navigatorImpl.shutdown()

        verify(exactly = 1) { mockNavigator.shutdown() }
        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
    }

    @Test
    fun `when navigator is not shut down, startNavigationSession does not log warning`() {
        navigatorImpl.startNavigationSession()

        verify(exactly = 0) { loggingFrontend.logW(any(), any()) }
        verify(exactly = 1) { mockNavigator.startNavigationSession() }
    }

    @Test
    fun `when navigator is shut down, startNavigationSession logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.startNavigationSession()

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.startNavigationSession() }
    }

    @Test
    fun `when navigator is shut down, stopNavigationSession logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.stopNavigationSession()

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.stopNavigationSession() }
    }

    @Test
    fun `when navigator is shut down, pause logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.pause()

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.pause() }
    }

    @Test
    fun `when navigator is shut down, resume logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.resume()

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.resume() }
    }

    @Test
    fun `when navigator is shut down, onEVDataUpdated logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.onEVDataUpdated(emptyMap())

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.onEvDataUpdated(any()) }
    }

    @Test
    fun `when navigator is shut down, setElectronicHorizonObserver logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.setElectronicHorizonObserver(null)

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.setElectronicHorizonObserver(any()) }
    }

    @Test
    fun `when navigator is shut down, setFallbackVersionsObserver logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.setFallbackVersionsObserver(null)

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.setFallbackVersionsObserver(any()) }
    }

    @Test
    fun `when navigator is shut down, addNavigatorObserver logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.addNavigatorObserver(mockk(relaxed = true))

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.addObserver(any()) }
    }

    @Test
    fun `when navigator is shut down, removeNavigatorObserver logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.removeNavigatorObserver(mockk(relaxed = true))

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.removeObserver(any()) }
    }

    @Test
    fun `when navigator is shut down, addRerouteObserver logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.addRerouteObserver(mockk(relaxed = true))

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.addRerouteObserver(any()) }
    }

    @Test
    fun `when navigator is shut down, removeRerouteObserver logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        navigatorImpl.removeRerouteObserver(mockk(relaxed = true))

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.removeRerouteObserver(any()) }
    }

    @Test
    fun `when navigator is shut down, setAdasisMessageCallback logs warning and skips native call`() {
        navigatorImpl.shutdown()

        navigatorImpl.setAdasisMessageCallback(mockk(relaxed = true), mockk(relaxed = true))

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockAdasisFacade.setAdasisMessageCallback(any(), any()) }
    }

    @Test
    fun `when navigator is shut down, resetAdasisMessageCallback logs warning and skips native call`() {
        navigatorImpl.shutdown()
        clearMocks(mockAdasisFacade)

        navigatorImpl.resetAdasisMessageCallback()

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockAdasisFacade.resetAdasisMessageCallback() }
    }

    @Test
    fun `when navigator is shut down, createNavigationPredictiveCacheController logs warning and returns empty list`() {
        navigatorImpl.shutdown()
        clearMocks(mockNavigator, answers = false)

        val result = navigatorImpl.createNavigationPredictiveCacheController(mockk(relaxed = true))

        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
        verify(exactly = 0) { mockNavigator.createPredictiveCacheController(any(), any()) }
        assertTrue(result.isEmpty())
    }

    @Test
    fun `when navigator is shut down, getRerouteDetector returns null`() {
        navigatorImpl.shutdown()

        val result = navigatorImpl.getRerouteDetector()

        assertNull(result)
        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
    }

    @Test
    fun `when navigator is shut down, getRerouteController returns null`() {
        navigatorImpl.shutdown()

        val result = navigatorImpl.getRerouteController()

        assertNull(result)
        verify(exactly = 1) { loggingFrontend.logW(any(), LOG_CATEGORY) }
    }

    private companion object {
        const val LOG_CATEGORY = "MapboxNativeNavigatorImpl"
    }
}
