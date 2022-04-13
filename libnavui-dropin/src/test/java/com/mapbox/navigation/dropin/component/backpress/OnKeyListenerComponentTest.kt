package com.mapbox.navigation.dropin.component.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class OnKeyListenerComponentTest {

    private val mockNavigationStateViewModel: NavigationStateViewModel = mockk(relaxed = true)
    private val mockDestinationViewModel: DestinationViewModel = mockk(relaxed = true)
    private val mockRoutesViewModel: RoutesViewModel = mockk(relaxed = true)
    private val slotOnKeyListener = slot<View.OnKeyListener>()
    private val view: View = mockk(relaxed = true) {
        every { setOnKeyListener(capture(slotOnKeyListener)) } just Runs
    }

    private val onKeyListenerComponent = OnKeyListenerComponent(
        mockNavigationStateViewModel,
        mockDestinationViewModel,
        mockRoutesViewModel,
        view,
    )

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
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.FreeDrive
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertFalse(result)
    }

    @Test
    fun `back press during DestinationPreview is handled`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.DestinationPreview
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during DestinationPreview moves to FreeDrive`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.DestinationPreview
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            mockNavigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.FreeDrive)
            )
        }
        assertTrue(result)
    }

    @Test
    fun `back press during DestinationPreview will SetDestination to null`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.DestinationPreview
        )

        onKeyListenerComponent.onAttached(mockk())
        slotOnKeyListener.captured.triggerBackPressed()

        verify {
            mockDestinationViewModel.invoke(
                DestinationAction.SetDestination(null)
            )
        }
    }

    @Test
    fun `back press during RoutePreview is handled`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.RoutePreview
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during RoutePreview moves to DestinationPreview`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.RoutePreview
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            mockNavigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.DestinationPreview)
            )
        }
        assertTrue(result)
    }

    @Test
    fun `back press during RoutePreview will SetRoutes to null`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.RoutePreview
        )

        onKeyListenerComponent.onAttached(mockk())
        slotOnKeyListener.captured.triggerBackPressed()

        verify {
            mockRoutesViewModel.invoke(
                RoutesAction.SetRoutes(emptyList())
            )
        }
    }

    @Test
    fun `back press during ActiveNavigation is handled`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during ActiveNavigation moves to RoutePreview`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            mockNavigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.RoutePreview)
            )
        }
        assertTrue(result)
    }

    @Test
    fun `back press during Arrival is handled`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.Arrival
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        assertTrue(result)
    }

    @Test
    fun `back press during Arrival moves to FreeDrive`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.Arrival
        )

        onKeyListenerComponent.onAttached(mockk())
        val result = slotOnKeyListener.captured.triggerBackPressed()

        verify {
            mockNavigationStateViewModel.invoke(
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
