package com.mapbox.navigation.driver.notification

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class DriverNotificationManagerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val parentJob = SupervisorJob()
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    private lateinit var manager: DriverNotificationManager

    @Before
    fun setUp() {
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, coroutineRule.createTestScope(parentJob))
        manager = DriverNotificationManager()
    }

    @After
    fun tearDown() {
        parentJob.cancel()
        unmockkObject(InternalJobControlFactory)
    }

    @Test
    fun `provider receives onAttached when navigation is available`() =
        coroutineRule.runBlockingTest {
            val provider = TestProvider()
            manager.attachDriverNotificationProvider(provider)
            manager.onAttached(mapboxNavigation)

            assertTrue(provider.attached)
        }

    @Test
    fun `provider receives onDetached when navigation goes away`() =
        coroutineRule.runBlockingTest {
            val provider = TestProvider()
            manager.attachDriverNotificationProvider(provider)
            manager.onAttached(mapboxNavigation)
            manager.onDetached(mapboxNavigation)

            assertTrue(provider.detached)
        }

    @Test
    fun `notifications are forwarded from provider`() = coroutineRule.runBlockingTest {
        val notification = TestNotification()
        val provider = TestProvider(notifications = flowOf(notification))
        val collected = mutableListOf<DriverNotification>()
        val collectJob = launch {
            manager.observeDriverNotifications().toList(collected)
        }

        manager.attachDriverNotificationProvider(provider)
        manager.onAttached(mapboxNavigation)

        assertEquals(listOf(notification), collected)
        collectJob.cancel()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate provider type throws`() {
        manager.attachDriverNotificationProvider(TestProvider())
        manager.attachDriverNotificationProvider(TestProvider())
    }

    @Test
    fun `detach cancels provider and calls onDetached`() = coroutineRule.runBlockingTest {
        val provider = TestProvider(notifications = MutableSharedFlow())
        manager.attachDriverNotificationProvider(provider)
        manager.onAttached(mapboxNavigation)
        assertTrue(provider.attached)

        manager.detachDriverNotificationProvider(provider)
        assertTrue(provider.detached)
    }

    @Test
    fun `provider reattaches on new MapboxNavigation`() = coroutineRule.runBlockingTest {
        val provider = TestProvider()
        manager.attachDriverNotificationProvider(provider)

        manager.onAttached(mapboxNavigation)
        assertTrue(provider.attached)

        manager.onDetached(mapboxNavigation)
        assertTrue(provider.detached)

        provider.reset()
        val newNavigation = mockk<MapboxNavigation>(relaxed = true)
        manager.onAttached(newNavigation)
        assertTrue(provider.attached)
    }

    private class TestNotification : DriverNotification()

    private class TestProvider(
        private val notifications: Flow<DriverNotification> = emptyFlow(),
    ) : DriverNotificationProvider() {

        var attached = false
        var detached = false

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            attached = true
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            detached = true
        }

        override fun trackNotifications(): Flow<DriverNotification> = notifications

        fun reset() {
            attached = false
            detached = false
        }
    }
}
