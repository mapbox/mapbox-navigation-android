package com.mapbox.navigation.dropin.tripprogress

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.tripprogress.internal.ui.TripProgressComponent
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class TripProgressBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var navContext: NavigationViewContext
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: TripProgressBinder

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

        sut = TripProgressBinder(navContext)
    }

    @Test
    fun `bind should return and bind TripProgressComponent`() {
        val components = sut.bind(FrameLayout(ctx))
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is TripProgressComponent })
    }
}
