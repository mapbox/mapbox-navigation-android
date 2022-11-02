package com.mapbox.navigation.core

import com.mapbox.navigation.core.internal.LegacyMapboxNavigationInstanceHolder
import com.mapbox.navigation.core.internal.MapboxNavigationCreateObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyMapboxNavigationInstanceHolderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()
    private val observer = mockk<MapboxNavigationCreateObserver>(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    @Before
    fun setUp() {
        LegacyMapboxNavigationInstanceHolder.unregisterAllCreateObservers()
        LegacyMapboxNavigationInstanceHolder.onDestroyed()
    }

    @After
    fun tearDown() {
        LegacyMapboxNavigationInstanceHolder.unregisterAllCreateObservers()
        LegacyMapboxNavigationInstanceHolder.onDestroyed()
    }

    @Test
    fun `observer is invoked on registration if has mapboxNavigation`() {
        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)

        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)

        verify(exactly = 1) { observer.onCreated(mapboxNavigation) }
    }

    @Test
    fun `observer is not invoked on registration if has no mapboxNavigation`() {
        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)

        verify(exactly = 0) { observer.onCreated(mapboxNavigation) }
    }

    @Test
    fun `observer is not invoked on registration if has destroyed mapboxNavigation`() {
        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)
        LegacyMapboxNavigationInstanceHolder.onDestroyed()

        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)

        verify(exactly = 0) { observer.onCreated(mapboxNavigation) }
    }

    @Test
    fun `observer is invoked when mapboxNavigation is created`() {
        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)

        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)

        verify(exactly = 1) { observer.onCreated(mapboxNavigation) }
    }

    @Test
    fun `observer is invoked when new mapboxNavigation is created`() {
        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)
        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)
        clearMocks(observer, answers = false)

        val newNavigation = mockk<MapboxNavigation>(relaxed = true)
        LegacyMapboxNavigationInstanceHolder.onCreated(newNavigation)

        verify(exactly = 1) { observer.onCreated(newNavigation) }
    }

    @Test
    fun `removed observer is not invoked when mapboxNavigation is created`() {
        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)
        LegacyMapboxNavigationInstanceHolder.unregisterCreateObserver(observer)

        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)

        verify(exactly = 0) { observer.onCreated(any()) }
    }

    @Test
    fun `multiple observers are invoked when new mapboxNavigation is created`() {
        val secondObserver = mockk<MapboxNavigationCreateObserver>(relaxed = true)
        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)
        LegacyMapboxNavigationInstanceHolder.registerCreateObserver(secondObserver)

        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)

        verify(exactly = 1) {
            observer.onCreated(mapboxNavigation)
            secondObserver.onCreated(mapboxNavigation)
        }
    }

    @Test
    fun `peek with no instance`() {
        assertNull(LegacyMapboxNavigationInstanceHolder.peek())
    }

    @Test
    fun `peek with active instance`() {
        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)

        assertEquals(mapboxNavigation, LegacyMapboxNavigationInstanceHolder.peek())
    }

    @Test
    fun `peek with destroyed instance`() {
        LegacyMapboxNavigationInstanceHolder.onCreated(mapboxNavigation)
        LegacyMapboxNavigationInstanceHolder.onDestroyed()

        assertNull(LegacyMapboxNavigationInstanceHolder.peek())
    }
}
