package com.mapbox.navigation.dropin.speedlimit

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.EmptyBinder
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
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpeedLimitCoordinatorTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var store: TestStore
    private lateinit var context: NavigationViewContext
    private lateinit var coordinator: SpeedLimitCoordinator

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

        coordinator = SpeedLimitCoordinator(context, FrameLayout(ctx))
    }

    @Test
    fun `should return default binder`() = runBlockingTest {
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() is SpeedLimitViewBinder)
        }
    }

    @Test
    fun `should return custom binder`() = runBlockingTest {
        val customBinder = mockk<UIBinder>()
        context.applyBinderCustomization {
            speedLimitBinder = customBinder
        }
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().take(1).toList()
            assertTrue(binders.first() === customBinder)
        }
    }

    @Test
    fun `should return EmptyBinder when ViewOptionsCustomization showSpeedLimit is FALSE`() =
        runBlockingTest {
            context.applyOptionsCustomization {
                showSpeedLimit = false
            }

            val binders = coordinator.run {
                val mapboxNavigation = mockk<MapboxNavigation>()
                mapboxNavigation.flowViewBinders().take(1).toList()
            }

            assertTrue(binders.first() is EmptyBinder)
        }
}
