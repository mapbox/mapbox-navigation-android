package com.mapbox.navigation.dropin.speedlimit

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.internal.extensions.ReloadingComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.speedlimit.internal.SpeedInfoComponent
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoStyle
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedInfoView
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpeedInfoViewBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: SpeedInfoViewBinder

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

        sut = SpeedInfoViewBinder(navContext)
    }

    @Test
    fun `bind should inflate speed limit layout`() {
        val rootLayout = FrameLayout(ctx)

        sut.bind(rootLayout)

        assertNotNull(
            "Expected MapboxSpeedInfoView",
            rootLayout.findViewById<MapboxSpeedInfoView>(R.id.speedInfoView)
        )
    }

    @Test
    fun `bind should return ReloadingComponent that attaches SpeedInfoComponent`() =
        runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            assertTrue(reloadComponent.childComponent is SpeedInfoComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on style change`() =
        coroutineRule.runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            val firstComponent = reloadComponent.childComponent
            navContext.applyStyleCustomization {
                speedInfoOptions = MapboxSpeedInfoOptions
                    .Builder()
                    .speedInfoStyle(
                        SpeedInfoStyle().apply {
                            mutcdLayoutBackground = android.R.drawable.spinner_background
                        }
                    )
                    .build()
            }

            assertNotEquals(firstComponent, reloadComponent.childComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on distanceFormatter change`() =
        coroutineRule.runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            val firstComponent = reloadComponent.childComponent
            navContext.applyOptionsCustomization {
                distanceFormatterOptions = DistanceFormatterOptions
                    .Builder(ctx)
                    .roundingIncrement(25)
                    .build()
            }

            assertNotEquals(firstComponent, reloadComponent.childComponent)
        }
}
