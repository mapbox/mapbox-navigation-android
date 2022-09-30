package com.mapbox.navigation.dropin.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.android.gestures.Utils
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
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

    private lateinit var onKeyListener: View.OnKeyListener

    @Before
    fun setUp() {
        mockkStatic(Utils::class)
        testStore = spyk(TestStore())
        onKeyListener = mockk(relaxed = true)
        onKeyListenerComponent = OnKeyListenerComponent(testStore, view, onKeyListener)
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
    fun `back press during RoutePreview will set route preview to null`() {
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        onKeyListenerComponent.onAttached(mockk())
        slotOnKeyListener.captured.triggerBackPressed()

        verify {
            testStore.dispatch(
                RoutePreviewAction.Ready(emptyList())
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

    @Test
    fun `should call delegate onClickListener with onKey event`() {
        onKeyListenerComponent.onAttached(mockk())

        slotOnKeyListener.captured.triggerBackPressed()

        verify { onKeyListener.onKey(view, KeyEvent.KEYCODE_BACK, any()) }
    }

    @Test
    fun `should use delegate onClickListener to intercept onKey events`() {
        every { onKeyListener.onKey(any(), any(), any()) } returns true
        testStore.setState(State(navigation = NavigationState.DestinationPreview))
        onKeyListenerComponent.onAttached(mockk())

        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
        verify(exactly = 0) { testStore.dispatch(any()) }
    }

    private fun View.OnKeyListener.triggerBackPressed(): Boolean {
        return onKey(
            view,
            KeyEvent.KEYCODE_BACK,
            mockk { every { action } returns KeyEvent.ACTION_UP }
        )
    }
}
