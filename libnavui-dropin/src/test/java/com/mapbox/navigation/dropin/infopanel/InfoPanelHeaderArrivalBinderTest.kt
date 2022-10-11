package com.mapbox.navigation.dropin.infopanel

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isEmpty
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.arrival.ArrivalTextComponent
import com.mapbox.navigation.dropin.extendablebutton.EndNavigationButtonComponent
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InfoPanelHeaderArrivalBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: InfoPanelHeaderArrivalBinder

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

        sut = InfoPanelHeaderArrivalBinder(navContext)
    }

    @Test
    fun `bind should inflate arrival header layout`() {
        val rootLayout = FrameLayout(ctx)

        sut.bind(rootLayout)

        assertNotNull(rootLayout.findViewById(R.id.arrivedTextContainer))
        assertNotNull(rootLayout.findViewById(R.id.endNavigationButtonLayout))
    }

    @Test
    fun `bind should return and bind ArrivalTextComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is ArrivalTextComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `should NOT bind ArrivalTextComponent when ViewOptionsCustomization showArrivalText is FALSE`() {
        navContext.applyOptionsCustomization {
            showArrivalText = false
        }
        val rootLayout = FrameLayout(ctx)
        val components = sut.bind(rootLayout)
        components.onAttached(mapboxNavigation)

        assertNull(components.findComponent { it is POINameComponent })
        assertTrue((rootLayout.findViewById(R.id.arrivedTextContainer) as ViewGroup).isEmpty())
    }

    @Test
    fun `bind should return and bind EndNavigationButtonComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is EndNavigationButtonComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should NOT bind EndNavigationButtonComponent when ViewOptionsCustomization showEndNavigationButton is FALSE`() {
        navContext.applyOptionsCustomization {
            showEndNavigationButton = false
        }
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNull(components.findComponent { it is EndNavigationButtonComponent })
    }
}
