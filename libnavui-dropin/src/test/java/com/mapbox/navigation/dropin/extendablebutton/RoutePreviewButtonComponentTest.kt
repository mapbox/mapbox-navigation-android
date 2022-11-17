package com.mapbox.navigation.dropin.extendablebutton

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutePreviewButtonComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var button: MapboxExtendableButton
    private lateinit var sut: RoutePreviewButtonComponent

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = spyk(TestStore())
        mapboxNavigation = mockk(relaxed = true)
        button = spyk(MapboxExtendableButton(context))

        sut = RoutePreviewButtonComponent(store, button)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onClick routePreview should dispatch FetchRouteAndShowRoutePreview action`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)
        button.performClick()

        verify(exactly = 1) {
            store.dispatch(RoutePreviewAction.FetchRouteAndShowRoutePreview)
        }
    }
}
