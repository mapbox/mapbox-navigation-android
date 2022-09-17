package com.mapbox.navigation.dropin.component.infopanel

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.NavigationViewModel
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.testutil.TestLifecycleOwner
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InfoPanelComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var layout: StubLayout
    private lateinit var navContext: NavigationViewContext

    private lateinit var sut: InfoPanelComponent

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        layout = StubLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(1, 1)
        }
        navContext = spyk(
            NavigationViewContext(
                context = ctx,
                lifecycleOwner = TestLifecycleOwner(),
                viewModel = NavigationViewModel(),
                storeProvider = { TestStore() }
            )
        )

        sut = InfoPanelComponent(layout, navContext)
    }

    @Test
    fun `onAttached should observe and apply info panel styles`() = coroutineRule.runBlockingTest {
        navContext.applyStyleCustomization {
            infoPanelBackground = R.drawable.mapbox_ic_camera_recenter
            infoPanelMarginStart = 11
            infoPanelMarginEnd = 22
        }
        sut.onAttached(mockk())

        val lp = layout.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(11, lp.leftMargin)
        assertEquals(22, lp.rightMargin)
        assertEquals(R.drawable.mapbox_ic_camera_recenter, layout.backgroundResId)
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
