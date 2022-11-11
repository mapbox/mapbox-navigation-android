package com.mapbox.navigation.dropin.infopanel

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.internal.extensions.headerContentBinder
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class InfoPanelHeaderBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var store: TestStore
    private lateinit var navContext: NavigationViewContext
    private lateinit var sut: InfoPanelHeaderBinder

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        store = TestStore()
        navContext = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { store }
        )
        sut = InfoPanelHeaderBinder(navContext)
    }

    @Test
    fun `headerContentBinder() should map NavigationState to correct default binders`() =
        runBlockingTest {
            store.updateState { it.copy(navigation = NavigationState.FreeDrive) }
            val emittedBinders = mutableListOf<UIBinder>()
            val job = launch {
                navContext.headerContentBinder().take(5).toList(emittedBinders)
                yield()
            }

            store.updateState { it.copy(navigation = NavigationState.DestinationPreview) }
            store.updateState { it.copy(navigation = NavigationState.RoutePreview) }
            store.updateState { it.copy(navigation = NavigationState.ActiveNavigation) }
            store.updateState { it.copy(navigation = NavigationState.Arrival) }
            job.join()

            assertTrue(emittedBinders[1] is InfoPanelHeaderDestinationPreviewBinder)
            assertTrue(emittedBinders[2] is InfoPanelHeaderRoutesPreviewBinder)
            assertTrue(emittedBinders[3] is InfoPanelHeaderActiveGuidanceBinder)
            assertTrue(emittedBinders[4] is InfoPanelHeaderArrivalBinder)
        }

    @Test
    fun `headerContentBinder() should map NavigationState to correct custom binders`() =
        runBlockingTest {
            val custom = object {
                val infoPanelHeaderFreeDriveBinder = UIBinder { UIComponent() }
                val infoPanelHeaderDestinationPreviewBinder = UIBinder { UIComponent() }
                val infoPanelHeaderRoutesPreviewBinder = UIBinder { UIComponent() }
                val infoPanelHeaderActiveGuidanceBinder = UIBinder { UIComponent() }
                val infoPanelHeaderArrivalBinder = UIBinder { UIComponent() }
            }
            navContext.applyBinderCustomization {
                infoPanelHeaderFreeDriveBinder = custom.infoPanelHeaderFreeDriveBinder
                infoPanelHeaderDestinationPreviewBinder =
                    custom.infoPanelHeaderDestinationPreviewBinder
                infoPanelHeaderRoutesPreviewBinder = custom.infoPanelHeaderRoutesPreviewBinder
                infoPanelHeaderActiveGuidanceBinder = custom.infoPanelHeaderActiveGuidanceBinder
                infoPanelHeaderArrivalBinder = custom.infoPanelHeaderArrivalBinder
            }
            store.updateState { it.copy(navigation = NavigationState.FreeDrive) }
            val emittedBinders = mutableListOf<UIBinder>()
            val job = launch {
                navContext.headerContentBinder().take(5).toList(emittedBinders)
                yield()
            }

            store.updateState { it.copy(navigation = NavigationState.DestinationPreview) }
            store.updateState { it.copy(navigation = NavigationState.RoutePreview) }
            store.updateState { it.copy(navigation = NavigationState.ActiveNavigation) }
            store.updateState { it.copy(navigation = NavigationState.Arrival) }
            job.join()

            assertEquals(custom.infoPanelHeaderFreeDriveBinder, emittedBinders[0])
            assertEquals(custom.infoPanelHeaderDestinationPreviewBinder, emittedBinders[1])
            assertEquals(custom.infoPanelHeaderRoutesPreviewBinder, emittedBinders[2])
            assertEquals(custom.infoPanelHeaderActiveGuidanceBinder, emittedBinders[3])
            assertEquals(custom.infoPanelHeaderArrivalBinder, emittedBinders[4])
        }

    @Test
    fun `bind() should BIND a binder returned by headerContentBinder`() = runBlockingTest {
        val testBinder = spyk(TestBinder())
        val sut = InfoPanelHeaderBinder(flowOf(testBinder))

        sut.bind(FrameLayout(ctx)).onAttached(mockk(relaxed = true))

        verify { testBinder.bind(any()) }
    }

    private class TestBinder : UIBinder {
        override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
            return UIComponent()
        }
    }
}
