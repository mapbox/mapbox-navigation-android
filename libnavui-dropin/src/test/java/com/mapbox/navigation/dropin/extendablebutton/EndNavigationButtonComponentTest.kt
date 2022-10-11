package com.mapbox.navigation.dropin.extendablebutton

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
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
class EndNavigationButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var button: MapboxExtendableButton
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: EndNavigationButtonComponent

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        mapboxNavigation = mockk(relaxed = true)
        store = spyk(TestStore())
        button = spyk(MapboxExtendableButton(context))

        sut = EndNavigationButtonComponent(store, button)
    }

    @Test
    fun `on click should dispatch endNavigation action`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        button.performClick()

        verify {
            store.dispatch(RoutesAction.SetRoutes(emptyList()))
            store.dispatch(RoutePreviewAction.Ready(emptyList()))
            store.dispatch(DestinationAction.SetDestination(null))
            store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
        }
    }
}
