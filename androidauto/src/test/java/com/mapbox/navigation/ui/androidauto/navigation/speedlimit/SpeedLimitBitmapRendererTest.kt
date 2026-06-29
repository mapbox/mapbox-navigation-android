package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Bitmap
import android.os.Build
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class SpeedLimitBitmapRendererTest {

    private val sut = SpeedLimitBitmapRenderer()

    @Test
    fun `getBitmap - should return correctly sized bitmap for SpeedLimitSign VIENNA`() {
        val bmp = sut.getBitmap(SpeedLimitSign.VIENNA)

        assertEquals(ViennaSpeedLimitDrawable.WIDTH, bmp.width)
        assertEquals(ViennaSpeedLimitDrawable.HEIGHT, bmp.height)
        assertEquals(Bitmap.Config.ARGB_8888, bmp.config)
    }

    @Test
    fun `getBitmap - should return correctly sized bitmap for SpeedLimitSign MUTCD`() {
        val bmp = sut.getBitmap(SpeedLimitSign.MUTCD)

        assertEquals(MutcdSpeedLimitDrawable.WIDTH, bmp.width)
        assertEquals(MutcdSpeedLimitDrawable.HEIGHT, bmp.height)
        assertEquals(Bitmap.Config.ARGB_8888, bmp.config)
    }

    @Test
    fun `getBitmap - should return same bitmap for the same SpeedLimitSign`() {
        val bmp1 = sut.getBitmap(SpeedLimitSign.VIENNA)
        val bmp2 = sut.getBitmap(SpeedLimitSign.VIENNA)

        assertSame(bmp1, bmp2)
    }

    @Test
    fun `getBitmap - should return separate bitmaps for different SpeedLimitSign`() {
        val bmp1 = sut.getBitmap(SpeedLimitSign.VIENNA)
        val bmp2 = sut.getBitmap(SpeedLimitSign.MUTCD)

        assertNotSame(bmp1, bmp2)
    }
}
