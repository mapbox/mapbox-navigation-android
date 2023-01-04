package com.mapbox.navigation.dropin.infopanel

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.navigationview.NavigationViewModel
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InfoPanelComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var layout: StubLayout
    private lateinit var parent: ViewGroup
    private lateinit var navContext: NavigationViewContext

    private lateinit var sut: InfoPanelComponent

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        layout = StubLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(1, 1)
        }
        parent = FrameLayout(ctx).apply {
            addView(layout)
            layoutParams = ViewGroup.MarginLayoutParams(1, 1)
        }
        navContext = NavigationViewContext(
            context = ctx,
            lifecycleOwner = TestLifecycleOwner(),
            viewModel = NavigationViewModel(),
            storeProvider = { TestStore() },
        )

        sut = InfoPanelComponent(layout, navContext)
    }

    @Test
    fun `onAttached should observe and apply info panel styles`() {
        navContext.applyStyleCustomization {
            infoPanelBackground = R.drawable.mapbox_ic_puck
            infoPanelMarginStart = 11
            infoPanelMarginEnd = 22
        }
        sut.onAttached(mockk())

        val lp = layout.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(11, lp.leftMargin)
        assertEquals(22, lp.rightMargin)
        assertEquals(R.drawable.mapbox_ic_puck, layout.backgroundResId)
    }

    @Test
    fun `onAttached should observe and apply insets`() {
        navContext.systemBarsInsets.value = Insets.of(33, 44, 55, 66)
        sut.onAttached(mockk())

        val lp = parent.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(33, lp.leftMargin)
        assertEquals(55, lp.rightMargin)
    }

    // Stub layout that exposes assigned background resource id.
    private class StubLayout(context: Context) : FrameLayout(context) {
        var backgroundResId = 0

        override fun setBackgroundResource(resid: Int) {
            super.setBackgroundResource(resid)
            backgroundResId = resid
        }
    }
}
