package com.mapbox.navigation.dropin.backpress

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackPressedComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val slotOnBackPressedCallback = slot<OnBackPressedCallback>()
    private val onBackPressedDispatcher = mockk<OnBackPressedDispatcher> {
        every { addCallback(capture(slotOnBackPressedCallback)) } just runs
    }
    private val testStore = TestStore()
    private val lifecycleOwner = TestLifecycleOwner()
    private val backPressedComponent =
        BackPressedComponent(onBackPressedDispatcher, testStore, lifecycleOwner)

    @Test
    fun `init will add onBackPressedCallback`() {
        verify { onBackPressedDispatcher.addCallback(any()) }
    }

    @Test
    fun `onStop will disable onBackPressedCallback`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        backPressedComponent.onAttached(mockk())
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED

        assertFalse(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `onDetached will disable onBackPressedCallback`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        backPressedComponent.onAttached(mockk())
        backPressedComponent.onDetached(mockk())

        assertFalse(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `back press during FreeDrive is not handled`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.FreeDrive))

        backPressedComponent.onAttached(mockk())

        assertFalse(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `back press during DestinationPreview is handled`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        backPressedComponent.onAttached(mockk())

        assertTrue(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `back press during DestinationPreview moves to FreeDrive`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        backPressedComponent.onAttached(mockk())
        slotOnBackPressedCallback.captured.handleOnBackPressed()

        assertTrue(NavigationStateAction.Update(NavigationState.FreeDrive) in testStore.actions)
    }

    @Test
    fun `back press during DestinationPreview will SetDestination to null`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.DestinationPreview))

        backPressedComponent.onAttached(mockk())
        slotOnBackPressedCallback.captured.handleOnBackPressed()

        assertTrue(DestinationAction.SetDestination(destination = null) in testStore.actions)
    }

    @Test
    fun `back press during RoutePreview is handled`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        backPressedComponent.onAttached(mockk())

        assertTrue(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `back press during RoutePreview moves to DestinationPreview`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        backPressedComponent.onAttached(mockk())
        slotOnBackPressedCallback.captured.handleOnBackPressed()

        val expectedAction = NavigationStateAction.Update(NavigationState.DestinationPreview)
        assertTrue(expectedAction in testStore.actions)
    }

    @Test
    fun `back press during RoutePreview will set route preview to null`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.RoutePreview))

        backPressedComponent.onAttached(mockk())
        slotOnBackPressedCallback.captured.handleOnBackPressed()

        assertTrue(RoutePreviewAction.Ready(emptyList()) in testStore.actions)
    }

    @Test
    fun `back press during ActiveNavigation is handled`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.ActiveNavigation))

        backPressedComponent.onAttached(mockk())

        assertTrue(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `back press during ActiveNavigation moves to RoutePreview`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.ActiveNavigation))

        backPressedComponent.onAttached(mockk())
        slotOnBackPressedCallback.captured.handleOnBackPressed()

        assertTrue(NavigationStateAction.Update(NavigationState.RoutePreview) in testStore.actions)
    }

    @Test
    fun `back press during Arrival is handled`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.Arrival))

        backPressedComponent.onAttached(mockk())

        assertTrue(slotOnBackPressedCallback.captured.isEnabled)
    }

    @Test
    fun `back press during Arrival moves to FreeDrive`() {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        testStore.setState(State(navigation = NavigationState.Arrival))

        backPressedComponent.onAttached(mockk())
        slotOnBackPressedCallback.captured.handleOnBackPressed()

        assertTrue(NavigationStateAction.Update(NavigationState.FreeDrive) in testStore.actions)
    }
}
