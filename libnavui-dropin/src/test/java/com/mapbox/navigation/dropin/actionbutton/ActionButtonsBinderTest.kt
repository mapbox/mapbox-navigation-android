package com.mapbox.navigation.dropin.actionbutton

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponent
import com.mapbox.navigation.ui.maps.internal.ui.CompassButtonComponent
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponent
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ActionButtonsBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var viewGroup: ViewGroup
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navContext: NavigationViewContext
    private lateinit var sut: ActionButtonsBinder

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
        sut = MapboxActionButtonsBinder()
        sut.context = navContext
    }

    @Test
    fun `should add custom buttons to Action Buttons layout`() {
        val customButtons = listOf(
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.START),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.START),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.START),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.END),
            ActionButtonDescription(View(ctx), ActionButtonDescription.Position.END),
        )
        sut.customButtons = customButtons

        sut.bind(viewGroup)
        val binder = MapboxActionButtonsLayoutBinding.bind(viewGroup.children.first())

        val topButtons = binder.buttonsContainerTop.children.toList()
        val bottomButtons = binder.buttonsContainerBottom.children.toList()
        assertEquals(3, topButtons.size)
        assertEquals(2, bottomButtons.size)
        assertEquals(customButtons[0].view, topButtons[0])
        assertEquals(customButtons[1].view, topButtons[1])
        assertEquals(customButtons[2].view, topButtons[2])
        assertEquals(customButtons[3].view, bottomButtons[0])
        assertEquals(customButtons[4].view, bottomButtons[1])
    }

    @Test
    fun `should NOT bind CompassButtonComponent when option showCompassActionButton is FALSE`() {
        navContext.applyOptionsCustomization {
            showCompassActionButton = false
        }
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNull(components.findComponent { it is CompassButtonComponent })
        assertTrue(viewGroup.findViewById<ViewGroup>(R.id.buttonsContainerCompass).isEmpty())
    }

    @Test
    fun `should bind CompassButtonComponent when option showCompassActionButton is TRUE`() {
        navContext.applyOptionsCustomization {
            showCompassActionButton = true
        }
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is CompassButtonComponent })
        assertTrue(viewGroup.findViewById<ViewGroup>(R.id.buttonsContainerCompass).isNotEmpty())
    }

    @Test
    fun `should bind CameraModeButtonComponent`() {
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is CameraModeButtonComponent })
        assertTrue(viewGroup.findViewById<ViewGroup>(R.id.buttonsContainerCamera).isNotEmpty())
    }

    @Test
    fun `should bind AudioGuidanceButtonComponent`() {
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is AudioGuidanceButtonComponent })
        assertTrue(viewGroup.findViewById<ViewGroup>(R.id.buttonsContainerAudio).isNotEmpty())
    }

    @Test
    fun `should bind RecenterButtonComponent`() {
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is RecenterButtonComponent })
        assertTrue(viewGroup.findViewById<ViewGroup>(R.id.buttonsContainerRecenter).isNotEmpty())
    }

    @Test
    fun `should use custom binder for Compass Button`() {
        class MyCompassButtonComponent : UIComponent()
        navContext.apply {
            applyBinderCustomization {
                actionCompassButtonBinder = UIBinder {
                    MyCompassButtonComponent()
                }
            }
            applyOptionsCustomization {
                showCompassActionButton = true
            }
        }

        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is MyCompassButtonComponent })
        assertNull(
            "should NOT bind default CompassButtonComponent",
            components.findComponent { it is CompassButtonComponent }
        )
    }

    @Test
    fun `should use custom binder for Camera Mode Button`() {
        class MyCameraModeButtonComponent : UIComponent()
        navContext.applyBinderCustomization {
            actionCameraModeButtonBinder = UIBinder {
                MyCameraModeButtonComponent()
            }
        }

        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is MyCameraModeButtonComponent })
        assertNull(
            "should NOT bind default CameraModeButtonComponent",
            components.findComponent { it is CameraModeButtonComponent }
        )
    }

    @Test
    fun `should use custom binder for Toggle Audio Button`() {
        class MyAudioGuidanceButtonComponent : UIComponent()
        navContext.applyBinderCustomization {
            actionToggleAudioButtonBinder = UIBinder {
                MyAudioGuidanceButtonComponent()
            }
        }

        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is MyAudioGuidanceButtonComponent })
        assertNull(
            "should NOT bind default AudioGuidanceButtonComponent",
            components.findComponent { it is AudioGuidanceButtonComponent }
        )
    }

    @Test
    fun `should use custom binder for Recenter Camera Button`() {
        class MyRecenterButtonComponent : UIComponent()
        navContext.applyBinderCustomization {
            actionRecenterButtonBinder = UIBinder {
                MyRecenterButtonComponent()
            }
        }

        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is MyRecenterButtonComponent })
        assertNull(
            "should NOT bind default RecenterButtonComponent",
            components.findComponent { it is RecenterButtonComponent }
        )
    }
}
