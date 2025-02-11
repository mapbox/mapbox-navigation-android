package com.mapbox.navigation.ui.base.lifecycle

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UICoordinatorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `should bind ViewGroup when onAttached`() = runBlockingTest {
        val viewGroup = mockk<ViewGroup>(relaxed = true)
        val coordinator = TestUICoordinator(viewGroup)
        val mapboxNavigation = mockk<MapboxNavigation>()
        val mapboxNavigationObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val binder = mockk<UIBinder>(relaxed = true) {
            every { bind(any()) } returns mapboxNavigationObserver
        }

        coordinator.onAttached(mapboxNavigation)
        coordinator.flow.emit(binder)

        verify { binder.bind(viewGroup) }
    }

    @Test
    fun `should attach the binder MapboxNavigationObserver`() = runBlockingTest {
        val viewGroup = mockk<ViewGroup>(relaxed = true)
        val coordinator = TestUICoordinator(viewGroup)
        val mapboxNavigation = mockk<MapboxNavigation>()
        val mapboxNavigationObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val binder = mockk<UIBinder>(relaxed = true) {
            every { bind(any()) } returns mapboxNavigationObserver
        }

        coordinator.onAttached(mapboxNavigation)
        coordinator.flow.emit(binder)

        verify(exactly = 1) { mapboxNavigationObserver.onAttached(mapboxNavigation) }
        verify(exactly = 0) { mapboxNavigationObserver.onDetached(mapboxNavigation) }
    }

    @Test
    fun `should detach the binder with coordinator is detached`() = runBlockingTest {
        val viewGroup = mockk<ViewGroup>(relaxed = true)
        val coordinator = TestUICoordinator(viewGroup)
        val mapboxNavigation = mockk<MapboxNavigation>()
        val mapboxNavigationObserver = mockk<MapboxNavigationObserver>(relaxed = true)
        val binder = mockk<UIBinder>(relaxed = true) {
            every { bind(any()) } returns mapboxNavigationObserver
        }

        coordinator.onAttached(mapboxNavigation)
        coordinator.flow.emit(binder)
        coordinator.onDetached(mapboxNavigation)

        verify(exactly = 1) { mapboxNavigationObserver.onAttached(mapboxNavigation) }
        verify(exactly = 1) { mapboxNavigationObserver.onDetached(mapboxNavigation) }
    }

    @Test
    fun `verify transition when binder is updated`() = runBlockingTest {
        val viewGroup = mockk<ViewGroup>(relaxed = true)
        val coordinator = TestUICoordinator(viewGroup)
        val mapboxNavigation = mockk<MapboxNavigation>()
        val mapboxNavigationObserverFirst = mockk<MapboxNavigationObserver>(relaxed = true)
        val binderFirst = mockk<UIBinder>(relaxed = true) {
            every { bind(any()) } returns mapboxNavigationObserverFirst
        }
        val mapboxNavigationObserverSecond = mockk<MapboxNavigationObserver>(relaxed = true)
        val binderSecond = mockk<UIBinder>(relaxed = true) {
            every { bind(any()) } returns mapboxNavigationObserverSecond
        }

        coordinator.onAttached(mapboxNavigation)
        coordinator.flow.emit(binderFirst)
        coordinator.flow.emit(binderSecond)

        verifyOrder {
            // First binder is attached
            binderFirst.bind(viewGroup)
            mapboxNavigationObserverFirst.onAttached(mapboxNavigation)

            // Second binder is attached, the first observer is detached.
            mapboxNavigationObserverFirst.onDetached(mapboxNavigation)
            binderSecond.bind(viewGroup)
            mapboxNavigationObserverSecond.onAttached(mapboxNavigation)
        }
    }
}

private class TestUICoordinator(viewGroup: ViewGroup) : UICoordinator<ViewGroup>(viewGroup) {
    val flow = MutableSharedFlow<UIBinder>()

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return flow
    }
}
