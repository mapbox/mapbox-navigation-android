package com.mapbox.navigation.dropin.binder.infopanel

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.ViewBinder
import com.mapbox.navigation.dropin.ViewBinderCustomization
import com.mapbox.navigation.dropin.binder.EmptyBinder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class InfoPanelHeaderBinderTest {

    private lateinit var ctx: Context
    private lateinit var sut: InfoPanelHeaderBinder

    @SpyK
    var tripProgressBinder = EmptyBinder()

    @MockK
    lateinit var mockNavContext: NavigationViewContext

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        ctx = ApplicationProvider.getApplicationContext()
        sut = InfoPanelHeaderBinder(mockNavContext)

        every { mockNavContext.store } returns mockk()
        every { mockNavContext.uiBinders } returns ViewBinder().apply {
            applyCustomization(
                ViewBinderCustomization().apply {
                    infoPanelTripProgressBinder = tripProgressBinder
                }
            )
        }
        every { mockNavContext.styles } returns mockk(relaxed = true)
        every { mockNavContext.viewModel } returns mockk(relaxed = true)
    }

    @Test
    fun `bind should call tripProgressBinder`() {
        sut.bind(FrameLayout(ctx))

        verify { tripProgressBinder.bind(allAny()) }
    }
}
