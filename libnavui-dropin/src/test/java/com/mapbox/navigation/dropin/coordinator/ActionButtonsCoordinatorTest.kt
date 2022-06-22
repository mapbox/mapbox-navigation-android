package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.ActionButtonDescription
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.ViewBinder
import com.mapbox.navigation.dropin.ViewBinderCustomization
import com.mapbox.navigation.dropin.binder.ActionButtonBinder
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class ActionButtonsCoordinatorTest {

    private lateinit var sut: ActionButtonsCoordinator
    private lateinit var viewBinders: ViewBinder

    @Before
    fun setUp() {
        viewBinders = ViewBinder()

        val navContext = mockk<NavigationViewContext> {
            every { uiBinders } returns viewBinders
        }
        sut = ActionButtonsCoordinator(navContext, mockk())
    }

    @Test
    fun `should return default actionButtonsBinder`() = runBlockingTest {
        sut.apply {
            val mapboxNavigation = mockk<MapboxNavigation>()
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.firstOrNull() is ActionButtonBinder)
        }
    }

    @Test
    fun `should return custom actionButtonsBinder`() = runBlockingTest {
        val customization = ViewBinderCustomization().apply {
            actionButtonsBinder = EmptyBinder()
        }
        viewBinders.applyCustomization(customization)

        val binders = sut.run {
            val mapboxNavigation = mockk<MapboxNavigation>()
            mapboxNavigation.flowViewBinders().take(1).toList()
        }

        assertEquals(customization.actionButtonsBinder, binders.first())
    }

    @Test
    fun `should reload default binder on ViewBinderCustomization#customActionButtons change`() =
        runBlockingTest {
            val customization = ViewBinderCustomization().apply {
                customActionButtons = listOf(ActionButtonDescription(mockk()))
            }

            val binders = mutableListOf<UIBinder>()
            sut.apply {
                val mapboxNavigation = mockk<MapboxNavigation>()
                val job = launch {
                    mapboxNavigation.flowViewBinders().take(2).toList(binders)
                }
                viewBinders.applyCustomization(customization)
                job.join()
            }

            assertNotEquals(binders[0], binders[1])
            assertTrue(binders[1] is ActionButtonBinder)
        }

    @Test
    fun `should NOT reload default binder on when custom actionButtonsBinder is set`() =
        runBlockingTest {
            val customization = ViewBinderCustomization().apply {
                actionButtonsBinder = EmptyBinder()
            }
            viewBinders.applyCustomization(customization)

            val binders = mutableListOf<UIBinder>()
            sut.apply {
                val mapboxNavigation = mockk<MapboxNavigation>()
                val job = launch {
                    mapboxNavigation.flowViewBinders().take(2).toList(binders)
                }
                viewBinders.applyCustomization(
                    ViewBinderCustomization().apply {
                        customActionButtons = listOf(ActionButtonDescription(mockk()))
                    }
                )
                job.join()
            }

            assertEquals(binders[0], binders[1])
            assertEquals(customization.actionButtonsBinder, binders[1])
        }
}
