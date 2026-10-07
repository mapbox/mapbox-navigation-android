package com.mapbox.navigation.ui.androidauto.navigation.lanes

import android.content.Context
import android.graphics.Color
import androidx.car.app.model.CarIcon
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class CarLaneIconRendererTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val options = CarLaneIconOptions.Builder().build(context)
    private val sut = CarLaneIconRenderer(context)

    @Test
    fun `every lanes icon owns its bitmap`() {
        val first = sut.renderLanesIcons(emptyList(), Color.RED, options)
        val second = sut.renderLanesIcons(emptyList(), Color.BLUE, options)

        assertNotSame(first.bitmap(), second.bitmap())
    }

    @Test
    fun `rendering new lanes does not change an earlier lanes icon`() {
        val first = sut.renderLanesIcons(emptyList(), Color.RED, options)
        sut.renderLanesIcons(emptyList(), Color.BLUE, options)

        assertEquals(Color.RED, first.bitmap().getPixel(0, 0))
    }

    private fun CarIcon.bitmap() = icon!!.bitmap!!
}
