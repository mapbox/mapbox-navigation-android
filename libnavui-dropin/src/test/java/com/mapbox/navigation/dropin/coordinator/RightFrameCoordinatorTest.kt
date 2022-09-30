package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
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
class RightFrameCoordinatorTest {

    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val rightFrameContentBinderFlow = MutableStateFlow<UIBinder?>(null)
    private val context = mockk<NavigationViewContext> {
        every { uiBinders } returns mockk {
            every { rightFrameContentBinder } returns rightFrameContentBinderFlow
        }
    }
    private val coordinator = RightFrameCoordinator(context, mockk())

    @Test
    fun `should return empty binder`() = runBlockingTest {
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is EmptyBinder)
        }
    }

    @Test
    fun `should return custom binder`() = runBlockingTest {
        val customBinder = mockk<UIBinder>()
        coordinator.apply {
            rightFrameContentBinderFlow.value = customBinder
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() === customBinder)
        }
    }

    @Test
    fun `should reload binder when rightFrameContentBinder changes`() = runBlockingTest {
        val customBinder1 = mockk<UIBinder>()
        val customBinder2 = mockk<UIBinder>()
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(4).toList(collectedBinders)
            }
            rightFrameContentBinderFlow.value = customBinder1
            rightFrameContentBinderFlow.value = null
            rightFrameContentBinderFlow.value = customBinder2
            job.join()
            assertEquals(4, collectedBinders.size)
            assertTrue(collectedBinders[0] is EmptyBinder)
            assertTrue(collectedBinders[1] === customBinder1)
            assertTrue(collectedBinders[2] is EmptyBinder)
            assertTrue(collectedBinders[3] === customBinder2)
        }
    }

    @Test
    fun `should use different EmptyBinder instances`() = runBlockingTest {
        val collectedBinders = mutableListOf<Binder<ViewGroup>>()
        coordinator.apply {
            val job = launch {
                mapboxNavigation.flowViewBinders().take(3).toList(collectedBinders)
            }
            rightFrameContentBinderFlow.value = mockk()
            rightFrameContentBinderFlow.value = null
            job.join()
            assertTrue(collectedBinders[0] is EmptyBinder)
            assertTrue(collectedBinders[2] is EmptyBinder)
            assertFalse(collectedBinders[0] === collectedBinders[2])
        }
    }
}
