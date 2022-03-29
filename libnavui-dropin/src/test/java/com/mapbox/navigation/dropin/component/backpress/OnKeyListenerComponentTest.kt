package com.mapbox.navigation.dropin.component.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.android.gestures.Utils
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
class OnKeyListenerComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val slotOnKeyListener = slot<View.OnKeyListener>()
    private val view: View = mockk(relaxed = true) {
        every { setOnKeyListener(capture(slotOnKeyListener)) } just Runs
    }

    private lateinit var testStore: TestStore

    private lateinit var onKeyListenerComponent: OnKeyListenerComponent

    @Before
    fun setUp() {
        mockkStatic(Utils::class)
        testStore = spyk(TestStore())

        onKeyListenerComponent = OnKeyListenerComponent(testStore, view)
    }

    @Test
    fun `onAttached with set focus and setOnKeyListener`() {
        onKeyListenerComponent.onAttached(mockk())

        verify { view.requestFocus() }
        verify { view.isFocusableInTouchMode = true }
        verify { view.setOnKeyListener(any()) }
    }

    @Test
    fun `onDetached with reset focus and setOnKeyListener`() {
        val mockMapboxNavigation: MapboxNavigation = mockk()
        onKeyListenerComponent.onAttached(mockMapboxNavigation)
        onKeyListenerComponent.onDetached(mockMapboxNavigation)

        verify { view.isFocusableInTouchMode = false }
        verify { view.setOnKeyListener(null) }
    }

    @Test
    fun `back press during FreeDrive is mot handled`() {
        testStore.setState(State(navigation = NavigationState.FreeDrive))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertFalse(result)
    }

    @Test
    fun `back press during DestinationPreview is handled`() {
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during DestinationPreview moves to FreeDrive`() {
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.FreeDrive)
            )
        }
        assertTrue(result)
    }

    @Test
    fun `back press during DestinationPreview will SetDestination to null`() {
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        onKeyListenerComponent.onAttached(mockk())
        slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                DestinationAction.SetDestination(null)
            )
        }
    }

    @Test
    fun `back press during RoutePreview is handled`() {
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during RoutePreview moves to DestinationPreview`() {
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.DestinationPreview)
            )
        }
        assertTrue(result)
    }

    @Test
    fun `back press during RoutePreview will SetRoutes to null`() {
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        onKeyListenerComponent.onAttached(mockk())
        slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                RoutesAction.SetRoutes(emptyList())
            )
        }
    }

    @Test
    fun `back press during ActiveNavigation is handled`() {
        testStore.setState(State(navigation = NavigationState.ActiveNavigation))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during ActiveNavigation moves to RoutePreview`() {
        testStore.setState(State(navigation = NavigationState.ActiveNavigation))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.RoutePreview)
            )
        }
        assertTrue(result)
    }

    @Test
    fun `back press during Arrival is handled`() {
        testStore.setState(State(navigation = NavigationState.Arrival))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during Arrival moves to FreeDrive`() {
        testStore.setState(State(navigation = NavigationState.Arrival))

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.FreeDrive)
            )
        }
        assertTrue(result)
    }

    private fun View.OnKeyListener.triggerBackPressed(): Boolean {
        return onKey(
            view,
            KeyEvent.KEYCODE_BACK,
            mockk { every { action } returns KeyEvent.ACTION_UP }
        )
    }
}
