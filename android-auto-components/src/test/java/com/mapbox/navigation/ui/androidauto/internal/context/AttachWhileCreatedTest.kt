package com.mapbox.navigation.ui.androidauto.internal.context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AttachWhileCreatedTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val lifecycleRegistry = LifecycleRegistry.createUnsafe(mockk<LifecycleOwner>())
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { lifecycle } returns lifecycleRegistry
    }
    private val mapboxNavigation: MapboxNavigation = mockk()
    private val observer: MapboxNavigationObserver = mockk(relaxed = true)

    @Before
    fun setup() {
        carAppTestRule.onAttached(mapboxNavigation)
    }

    @Test
    fun `returns the observer itself without attaching it before the lifecycle is created`() {
        val attached = mapboxCarContext.attachWhileCreated(observer)

        assertSame(observer, attached)
        verify(exactly = 0) { observer.onAttached(any()) }
    }

    @Test
    fun `attaches on create and detaches on destroy, keeping the same instance`() {
        val attached = mapboxCarContext.attachWhileCreated(observer)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        verify(exactly = 1) { observer.onAttached(mapboxNavigation) }
        verify(exactly = 0) { observer.onDetached(any()) }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        verify(exactly = 1) { observer.onDetached(mapboxNavigation) }
        assertSame(observer, attached)
    }
}
