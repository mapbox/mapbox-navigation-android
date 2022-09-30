package com.mapbox.navigation.dropin.tripprogress

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.dropin.internal.extensions.ReloadingComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class TripProgressBinderTest {

    private lateinit var ctx: Context
    private lateinit var sut: TripProgressBinder

    @MockK
    lateinit var mockNavContext: NavigationViewContext

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockNavContext.styles } returns mockk()
        ctx = ApplicationProvider.getApplicationContext()
        sut = TripProgressBinder(mockNavContext)
    }

    @Test
    fun `bind should return TripProgressComponent`() {
        every { mockNavContext.styles.tripProgressStyle } returns MutableStateFlow(1)
        every { mockNavContext.options.distanceFormatterOptions } returns MutableStateFlow(
            DistanceFormatterOptions.Builder(ctx).build()
        )
        val result = sut.bind(FrameLayout(ctx))

        assertTrue(
            "expected TripProgressComponent instance",
            result is ReloadingComponent<*>
        )
    }
}
