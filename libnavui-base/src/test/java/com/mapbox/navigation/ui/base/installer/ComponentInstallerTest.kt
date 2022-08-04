package com.mapbox.navigation.ui.base.installer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class ComponentInstallerTest {

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `installComponent should register ComponentInstaller lifecycle is CREATED`() {
        val testComponent = TestComponent()
        val lifecycleOwner = TestLifecycleOwner()
        MapboxNavigationApp.installComponents(lifecycleOwner) {
            component(testComponent)
        }

        lifecycleOwner.moveToState(Lifecycle.State.CREATED)

        val registeredObserver = slot<MapboxNavigationObserver>()
        verify { MapboxNavigationApp.registerObserver(capture(registeredObserver)) }
        assertTrue(registeredObserver.captured is ComponentInstaller)
    }

    @Test
    fun `installComponent should not register ComponentInstaller before creation`() {
        val testComponent = TestComponent()
        val lifecycleOwner = TestLifecycleOwner()
        MapboxNavigationApp.installComponents(lifecycleOwner) {
            component(testComponent)
        }

        verify(exactly = 0) { MapboxNavigationApp.registerObserver(any()) }
    }

    private class TestComponent : UIComponent() {
        var attachedTo: MapboxNavigation? = null

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)
            attachedTo = mapboxNavigation
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            super.onDetached(mapboxNavigation)
            attachedTo = null
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry

        fun moveToState(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }
    }
}
