package com.mapbox.navigation.dropin.actionbutton

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.ViewBinderCustomization
import com.mapbox.navigation.dropin.ViewOptionsCustomization
import com.mapbox.navigation.dropin.navigationview.NavigationViewBinder
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ActionButtonsCoordinatorTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var sut: ActionButtonsCoordinator
    private lateinit var navContext: NavigationViewContext
    private lateinit var viewBinders: NavigationViewBinder

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()

        navContext = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { TestStore() }
        )
        viewBinders = navContext.uiBinders
        sut = ActionButtonsCoordinator(navContext, mockk())
    }

    @Test
    fun `should return default actionButtonsBinder`() = runBlockingTest {
        sut.apply {
            val mapboxNavigation = mockk<MapboxNavigation>()
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.firstOrNull() is ActionButtonsBinder)
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
            assertTrue(binders[1] is ActionButtonsBinder)
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

    @Test
    fun `should return EmptyBinder when ViewOptionsCustomization showActionButtons is FALSE`() =
        runBlockingTest {
            navContext.options.applyCustomization(
                ViewOptionsCustomization().apply {
                    showActionButtons = false
                }
            )

            val binders = sut.run {
                val mapboxNavigation = mockk<MapboxNavigation>()
                mapboxNavigation.flowViewBinders().take(1).toList()
            }

            assertTrue(binders.first() is EmptyBinder)
        }
}
