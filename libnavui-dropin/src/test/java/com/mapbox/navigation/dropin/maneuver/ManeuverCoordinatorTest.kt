package com.mapbox.navigation.dropin.maneuver

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ManeuverCoordinatorTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var store: TestStore
    private lateinit var context: NavigationViewContext
    private lateinit var coordinator: ManeuverCoordinator

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        store = TestStore()
        context = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { store }
        )
        mapboxNavigation = mockk(relaxed = true)

        coordinator = ManeuverCoordinator(context, FrameLayout(ctx))
    }

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
        context.applyBinderCustomization {
            maneuverBinder = customBinder
        }
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
        context.applyBinderCustomization {
            maneuverBinder = customBinder
        }
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
    fun `should return EmptyBinder when ViewOptionsCustomization showManeuver is FALSE`() =
        runBlockingTest {
            context.applyOptionsCustomization {
                showManeuver = false
            }

            val binders = coordinator.run {
                val mapboxNavigation = mockk<MapboxNavigation>()
                mapboxNavigation.flowViewBinders().take(1).toList()
            }

            assertTrue(binders.first() is EmptyBinder)
        }
}
