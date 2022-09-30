package com.mapbox.navigation.dropin.infopanel

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.extendablebutton.RoutePreviewButtonComponent
import com.mapbox.navigation.dropin.extendablebutton.StartNavigationButtonComponent
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InfoPanelHeaderDestinationPreviewBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: InfoPanelHeaderDestinationPreviewBinder

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
        mapboxNavigation = mockk(relaxed = true)

        sut = InfoPanelHeaderDestinationPreviewBinder(navContext)
    }

    @Test
    fun `bind should inflate destination preview header layout`() {
        val rootLayout = FrameLayout(ctx)

        sut.bind(rootLayout)

        assertNotNull(rootLayout.findViewById(R.id.poiName))
        assertNotNull(rootLayout.findViewById(R.id.routePreviewContainer))
        assertNotNull(rootLayout.findViewById(R.id.startNavigationContainer))
    }

    @Test
    fun `bind should return and bind POINameComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is POINameComponent })
    }

    @Test
    fun `bind should return and bind RoutePreviewButtonComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is RoutePreviewButtonComponent })
    }

    @Test
    fun `bind should return and bind StartNavigationButtonComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is StartNavigationButtonComponent })
    }
}
