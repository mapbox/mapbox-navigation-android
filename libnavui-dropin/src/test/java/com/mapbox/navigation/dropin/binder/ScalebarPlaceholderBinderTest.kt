package com.mapbox.navigation.dropin.binder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.NavigationViewStyles
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.coordinator.ScalebarPlaceholderBinder
import com.mapbox.navigation.dropin.util.TestStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ScalebarPlaceholderBinderTest {

    private lateinit var ctx: Context
    private lateinit var viewGroup: ViewGroup

    private lateinit var navContext: NavigationViewContext

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        viewGroup = FrameLayout(ctx)
        viewGroup.addView(View(ctx))

        navContext = mockk(relaxed = true) {
            every { store } returns TestStore()
            every { styles } returns NavigationViewStyles(ctx)
            every { viewModel } returns mockk {
                every { viewModelScope } returns MainScope()
            }
        }
    }

    @Test
    fun shouldLeaveOnlyPlaceholder() {
        val binder = ScalebarPlaceholderBinder(navContext)
        binder.bind(viewGroup)
        Assert.assertEquals(1, viewGroup.childCount)
        Assert.assertEquals(R.id.scalebarPlaceholder, viewGroup.getChildAt(0).id)
    }
}
