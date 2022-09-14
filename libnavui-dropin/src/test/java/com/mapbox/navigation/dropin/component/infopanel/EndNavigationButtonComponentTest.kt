package com.mapbox.navigation.dropin.component.infopanel

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxExtendableButtonParams
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.internal.extensions.recreateButton
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EndNavigationButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var buttonParams: MutableStateFlow<MapboxExtendableButtonParams>
    private lateinit var button: MapboxExtendableButton
    private lateinit var buttonContainer: ViewGroup
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: EndNavigationButtonComponent

    private val layoutParams: LinearLayout.LayoutParams = mockk(relaxed = true)
    private val initialParams =
        MapboxExtendableButtonParams(R.style.DropInStyleExitButton, layoutParams)
    private val updatedParams =
        MapboxExtendableButtonParams(R.style.DropInStyleRecenterButton, layoutParams)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapboxNavigation = mockk(relaxed = true)
        store = spyk(TestStore())
        buttonParams = MutableStateFlow(initialParams)
        button = spyk(MapboxExtendableButton(context))
        buttonContainer = mockk(relaxed = true)

        mockkStatic("com.mapbox.navigation.dropin.internal.extensions.ViewGroupExKt")
        every { buttonContainer.recreateButton(any()) } returns button

        sut = EndNavigationButtonComponent(store, buttonContainer, buttonParams)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `should observe params and recreate button`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)
        buttonParams.value = updatedParams

        verifyOrder {
            buttonContainer.recreateButton(initialParams)
            buttonContainer.recreateButton(updatedParams)
        }
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
