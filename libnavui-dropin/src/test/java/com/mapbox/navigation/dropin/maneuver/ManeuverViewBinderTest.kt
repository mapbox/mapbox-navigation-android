package com.mapbox.navigation.dropin.maneuver

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.Style
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
import com.mapbox.navigation.ui.maneuver.internal.ManeuverComponent
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ManeuverViewBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var loadedMapStyleFlow: MutableStateFlow<Style?>
    private lateinit var sut: ManeuverViewBinder

    @Suppress("PrivatePropertyName")
    private var MAP_STYLE = mockk<Style> {
        every { styleURI } returns Style.DARK
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        navContext = spyk(
            NavigationViewContext(
                context = ctx,
                lifecycleOwner = TestLifecycleOwner(),
                viewModel = NavigationViewModel(),
                storeProvider = { TestStore() }
            )
        )
        loadedMapStyleFlow = MutableStateFlow(MAP_STYLE)
        every { navContext.mapStyleLoader } returns mockk {
            every { loadedMapStyle } returns loadedMapStyleFlow
        }
        mapboxNavigation = mockk(relaxed = true)

        sut = ManeuverViewBinder(navContext)
    }

    @Test
    fun `bind should inflate maneuver view layout`() {
        val rootLayout = FrameLayout(ctx)

        sut.bind(rootLayout)

        assertNotNull(
            "Expected MapboxManeuverView",
            rootLayout.findViewById<MapboxManeuverView>(R.id.maneuverView)
        )
    }

    @Test
    fun `bind should return ReloadingComponent that attaches ManeuverComponent`() =
        runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            assertTrue(reloadComponent.childComponent is ManeuverComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on Map Style change`() =
        coroutineRule.runBlockingTest {
            loadedMapStyleFlow.value = null
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)
            assertNull(reloadComponent.childComponent)

            loadedMapStyleFlow.value = MAP_STYLE

            assertNotNull(reloadComponent.childComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on maneuverViewOptions change`() =
        coroutineRule.runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            val firstComponent = reloadComponent.childComponent
            navContext.applyStyleCustomization {
                maneuverViewOptions = ManeuverViewOptions.Builder()
                    .maneuverBackgroundColor(android.R.color.darker_gray)
                    .build()
            }

            assertNotEquals(firstComponent, reloadComponent.childComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should return ReloadingComponent that reloads child component on distanceFormatterOptions change`() =
        coroutineRule.runBlockingTest {
            val rootLayout = FrameLayout(ctx)
            val reloadComponent = sut.bind(rootLayout) as ReloadingComponent<*>
            reloadComponent.onAttached(mapboxNavigation)

            val firstComponent = reloadComponent.childComponent
            navContext.applyOptionsCustomization {
                distanceFormatterOptions = DistanceFormatterOptions.Builder(ctx)
                    .locale(Locale.CANADA)
                    .build()
            }

            assertNotEquals(firstComponent, reloadComponent.childComponent)
        }
}
