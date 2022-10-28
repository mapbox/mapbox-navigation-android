package com.mapbox.navigation.dropin.actionbutton

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioGuidanceButtonBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var viewGroup: ViewGroup
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navContext: NavigationViewContext
    private lateinit var sut: AudioGuidanceButtonBinder

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        viewGroup = FrameLayout(ctx)
        navContext = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { TestStore() }
        )
        mapboxNavigation = mockk(relaxed = true)
        sut = AudioGuidanceButtonBinder(navContext)
    }

    @Test
    fun `bind should bind AudioGuidanceButtonComponent`() {
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is AudioGuidanceButtonComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should reload AudioGuidanceButtonComponent when audioGuidanceButtonStyle changes`() =
        runBlockingTest {
            val components = sut.bind(viewGroup)
            components.onAttached(mapboxNavigation)

            val firstComponent = components.findComponent { it is AudioGuidanceButtonComponent }
            navContext.applyStyleCustomization {
                audioGuidanceButtonStyle = R.style.Widget_AppCompat_Button
            }
            val secondComponent = components.findComponent { it is AudioGuidanceButtonComponent }
            assertNotNull(secondComponent)
            assertNotEquals(firstComponent, secondComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should re-create MapboxAudioGuidanceButton when audioGuidanceButtonStyle changes`() =
        runBlockingTest {
            val components = sut.bind(viewGroup)
            components.onAttached(mapboxNavigation)

            val firstButton = viewGroup.children.first()
            navContext.applyStyleCustomization {
                audioGuidanceButtonStyle = R.style.Widget_AppCompat_Button
            }
            val secondButton = viewGroup.children.first()

            assertEquals(1, viewGroup.childCount)
            assertNotEquals(firstButton, secondButton)
            assertTrue(secondButton is MapboxAudioGuidanceButton)
        }
}
