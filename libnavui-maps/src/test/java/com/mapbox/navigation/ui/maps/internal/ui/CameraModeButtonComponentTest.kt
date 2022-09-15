package com.mapbox.navigation.ui.maps.internal.ui

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraModeButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var cameraModeButton: MapboxCameraModeButton
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var contract: StubContract
    private lateinit var sut: CameraModeButtonComponent

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        cameraModeButton = spyk(MapboxCameraModeButton(context, null))
        mapboxNavigation = mockk(relaxed = true)
        contract = spyk(StubContract())
        sut = CameraModeButtonComponent(cameraModeButton) { contract }
    }

    @Test
    fun `should update button visibility`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        contract.isVisible.value = false
        assertFalse(cameraModeButton.isVisible)

        contract.isVisible.value = true
        assertTrue(cameraModeButton.isVisible)
    }

    @Test
    fun `should update button state`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        contract.buttonState.value = NavigationCameraState.OVERVIEW
        verify { cameraModeButton.setState(NavigationCameraState.OVERVIEW) }

        contract.buttonState.value = NavigationCameraState.FOLLOWING
        verify { cameraModeButton.setState(NavigationCameraState.FOLLOWING) }
    }

    @Test
    fun `should forward button clicks to contract`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        cameraModeButton.performClick()

        verify { contract.onClick(any()) }
    }

    private class StubContract : CameraModeButtonComponentContract {
        override var isVisible = MutableStateFlow(false)
        override var buttonState = MutableStateFlow(NavigationCameraState.IDLE)
        override fun onClick(view: View) = Unit
    }
}
