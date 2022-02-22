package com.mapbox.navigation.dropin.binder.infopanel

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InfoPanelTripProgressBinderTest {

    private lateinit var ctx: Context
    private lateinit var sut: InfoPanelTripProgressBinder

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = InfoPanelTripProgressBinder()
    }

    @Test
    fun `bind should return TripProgressComponent`() {
        val result = sut.bind(FrameLayout(ctx))

        assertTrue(
            "expected TripProgressComponent instance",
            result is TripProgressComponent
        )
    }
}
