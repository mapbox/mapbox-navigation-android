package com.mapbox.navigation.dropin.actionbutton

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxExtendableButtonParams
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.findComponent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maps.internal.ui.CompassButtonComponent
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
class CompassButtonBinderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var viewGroup: ViewGroup
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navContext: NavigationViewContext
    private lateinit var sut: CompassButtonBinder

    private val buttonParams = MapboxExtendableButtonParams(
        R.style.Widget_AppCompat_Button,
        LinearLayout.LayoutParams(20, 20)
    )

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
        sut = CompassButtonBinder(navContext)
    }

    @Test
    fun `bind should bind CompassButtonComponent`() {
        val components = sut.bind(viewGroup)
        components.onAttached(mapboxNavigation)

        assertNotNull(components.findComponent { it is CompassButtonComponent })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should reload CompassButtonComponent when style compassButtonParams changes`() =
        runBlockingTest {
            val components = sut.bind(viewGroup)
            components.onAttached(mapboxNavigation)

            val firstComponent = components.findComponent { it is CompassButtonComponent }
            navContext.applyStyleCustomization {
                compassButtonParams = buttonParams
            }
            val secondComponent = components.findComponent { it is CompassButtonComponent }
            assertNotNull(secondComponent)
            assertNotEquals(firstComponent, secondComponent)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `bind should re-create MapboxExtendableButton when style compassButtonParams changes`() =
        runBlockingTest {
            val components = sut.bind(viewGroup)
            components.onAttached(mapboxNavigation)

            val firstButton = viewGroup.children.first()
            navContext.applyStyleCustomization {
                compassButtonParams = buttonParams
            }
            val secondButton = viewGroup.children.first()

            assertEquals(1, viewGroup.childCount)
            assertNotEquals(firstButton, secondButton)
            assertTrue(secondButton is MapboxExtendableButton)
        }
}
