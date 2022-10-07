package com.mapbox.navigation.dropin.speedlimit

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.internal.extensions.ReloadingComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.speedlimit.internal.SpeedLimitComponent
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpeedLimitViewBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: SpeedLimitViewBinder

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        navContext = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { TestStore() }
        )
        mapboxNavigation = mockk(relaxed = true)

        sut = SpeedLimitViewBinder(navContext)
    }

    @Test
    fun `bind should inflate speed limit layout`() {
        val rootLayout = FrameLayout(ctx)

        sut.bind(rootLayout)

        assertNotNull(
            "Expected MapboxSpeedLimitView",
            rootLayout.findViewById<MapboxSpeedLimitView>(R.id.speedLimitView)
        )
    }

    @Test
    fun `bind should return ReloadingComponent that attaches SpeedLimitComponent`() =
        runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            assertTrue(reloadComponent.childComponent is SpeedLimitComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on speedLimitStyle change`() =
        coroutineRule.runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            val firstComponent = reloadComponent.childComponent
            navContext.applyStyleCustomization {
                speedLimitStyle = com.mapbox.navigation.ui.speedlimit.R.style.MapboxStyleSpeedLimit
            }

            assertNotEquals(firstComponent, reloadComponent.childComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on speedLimitTextAppearance change`() =
        coroutineRule.runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            val firstComponent = reloadComponent.childComponent
            navContext.applyStyleCustomization {
                speedLimitTextAppearance = R.style.TextAppearance_AppCompat_Medium
            }

            assertNotEquals(firstComponent, reloadComponent.childComponent)
        }
}
