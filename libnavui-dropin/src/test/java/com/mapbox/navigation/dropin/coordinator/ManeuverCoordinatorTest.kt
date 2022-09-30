package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewBinder
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ManeuverCoordinatorTest {

    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val store = TestStore()
    private val maneuverFlow = MutableStateFlow<UIBinder?>(null)
    private val context = mockk<NavigationViewContext> {
        every { store } returns this@ManeuverCoordinatorTest.store
        every { uiBinders } returns mockk {
            every { maneuver } returns maneuverFlow
        }
        every { mapStyleLoader } returns mockk(relaxed = true)
        every { options } returns mockk(relaxed = true)
        every { styles } returns mockk(relaxed = true)
    }
    private val coordinator = ManeuverCoordinator(context, mockk())

    @Test
    fun `should create empty binder fore FreeDrive`() = runBlockingTest {
        store.setState(State(navigation = NavigationState.FreeDrive))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is EmptyBinder)
        }
    }

    @Test
    fun `should create empty binder fore FreeDrive even with custom binder`() = runBlockingTest {
        val customBinder = mockk<UIBinder>()
        maneuverFlow.value = customBinder
        store.setState(State(navigation = NavigationState.FreeDrive))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is EmptyBinder)
        }
    }

    @Test
    fun `should create empty binder fore DestinationPreview`() = runBlockingTest {
        store.setState(State(navigation = NavigationState.DestinationPreview))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is EmptyBinder)
        }
    }

    @Test
    fun `should create empty binder fore RoutePreview`() = runBlockingTest {
        store.setState(State(navigation = NavigationState.RoutePreview))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is EmptyBinder)
        }
    }

    @Test
    fun `should create empty binder fore Arrival`() = runBlockingTest {
        store.setState(State(navigation = NavigationState.Arrival))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is EmptyBinder)
        }
    }

    @Test
    fun `should create default binder for ActiveGuidance`() = runBlockingTest {
        store.setState(State(navigation = NavigationState.ActiveNavigation))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is ManeuverViewBinder)
        }
    }

    @Test
    fun `should use custom binder for ActiveGuidance`() = runBlockingTest {
        val customBinder = mockk<UIBinder>()
        maneuverFlow.value = customBinder
        store.setState(State(navigation = NavigationState.ActiveNavigation))
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() === customBinder)
        }
    }


    @Test
    fun `should create new binder when navigation state changes`() = runBlockingTest {
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(2).toList(collectedBinders)
            }
            store.setState(State(navigation = NavigationState.ActiveNavigation))
            job.join()
            assertEquals(2, collectedBinders.size)
            assertTrue(collectedBinders[0] is EmptyBinder)
            assertTrue(collectedBinders[1] is ManeuverViewBinder)
        }
    }

    @Test
    fun `should create new binder when custom binders change`() = runBlockingTest {
        val customBinder1 = mockk<UIBinder>()
        val customBinder2 = mockk<UIBinder>()
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        store.setState(State(navigation = NavigationState.ActiveNavigation))
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(4).toList(collectedBinders)
            }
            maneuverFlow.value = customBinder1
            maneuverFlow.value = null
            maneuverFlow.value = customBinder2
            job.join()
            assertEquals(4, collectedBinders.size)
            assertTrue(collectedBinders[0] is ManeuverViewBinder)
            assertTrue(collectedBinders[1] === customBinder1)
            assertTrue(collectedBinders[2] is ManeuverViewBinder)
            assertTrue(collectedBinders[3] === customBinder2)
        }
    }

    @Test
    fun `should use different instances of ManeuverViewBinder`() = runBlockingTest {
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        store.setState(State(navigation = NavigationState.ActiveNavigation))
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(3).toList(collectedBinders)
            }
            maneuverFlow.value = mockk()
            maneuverFlow.value = null
            job.join()
            assertTrue(collectedBinders[0] is ManeuverViewBinder)
            assertTrue(collectedBinders[2] is ManeuverViewBinder)
            assertFalse(collectedBinders[0] === collectedBinders[2])
        }
    }

    @Test
    fun `should use different instances of EmptyBinder`() = runBlockingTest {
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(3).toList(collectedBinders)
            }
            store.setState(State(navigation = NavigationState.ActiveNavigation))
            store.setState(State(navigation = NavigationState.FreeDrive))
            job.join()
            assertTrue(collectedBinders[0] is EmptyBinder)
            assertTrue(collectedBinders[2] is EmptyBinder)
            assertFalse(collectedBinders[0] === collectedBinders[2])
        }
    }
}
