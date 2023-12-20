package com.mapbox.navigation.ui.maps.internal.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.utils.internal.android.isVisible
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecenterButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context

    private lateinit var sut: RecenterButtonComponent

    private lateinit var button: MapboxExtendableButton
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var contract: RecenterButtonComponentContract
    private lateinit var visibilityFlow: MutableStateFlow<Boolean>

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        button = MapboxExtendableButton(ctx)
        mapboxNavigation = mockk(relaxed = true)
        visibilityFlow = MutableStateFlow(false)
        contract = mockk(relaxed = true) {
            every { isVisible } returns visibilityFlow
        }

        sut = RecenterButtonComponent(button) {
            contract
        }
    }

    @Test
    fun `should use contract to handle button onClick`() {
        sut.onAttached(mapboxNavigation)

        button.performClick()

        verify { contract.onClick(any()) }
    }

    @Test
    fun `should contract isVisible flow and update button visibility`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mapboxNavigation)

            assertFalse(button.isVisible)

            visibilityFlow.value = true
            assertTrue(button.isVisible)
        }
}
