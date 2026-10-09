package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Bitmap
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun `getBitmap - never draws into the bitmap returned by the previous call`() {
        val bmp1 = sut.getBitmap(SpeedLimitSign.VIENNA, speedLimit = 50)
        val bmp2 = sut.getBitmap(SpeedLimitSign.VIENNA, speedLimit = 60)

        assertNotSame(bmp1, bmp2)
    }

    @Test
    fun `getBitmap - alternates between two bitmaps for the same SpeedLimitSign`() {
        val bmp1 = sut.getBitmap(SpeedLimitSign.MUTCD)
        val bmp2 = sut.getBitmap(SpeedLimitSign.MUTCD)
        val bmp3 = sut.getBitmap(SpeedLimitSign.MUTCD)
        val bmp4 = sut.getBitmap(SpeedLimitSign.MUTCD)

        assertSame(bmp1, bmp3)
        assertSame(bmp2, bmp4)
    }

    @Test
    fun `getBitmap - a different SpeedLimitSign does not advance the other sign buffers`() {
        val mutcd1 = sut.getBitmap(SpeedLimitSign.MUTCD)
        sut.getBitmap(SpeedLimitSign.VIENNA)
        val mutcd2 = sut.getBitmap(SpeedLimitSign.MUTCD)
        sut.getBitmap(SpeedLimitSign.VIENNA)
        val mutcd3 = sut.getBitmap(SpeedLimitSign.MUTCD)

        assertNotSame(mutcd1, mutcd2)
        assertSame(mutcd1, mutcd3)
    }

    @Test
    fun `getBitmap - should return separate bitmaps for different SpeedLimitSign`() {
        val bmp1 = sut.getBitmap(SpeedLimitSign.VIENNA)
        val bmp2 = sut.getBitmap(SpeedLimitSign.MUTCD)

        assertNotSame(bmp1, bmp2)
    }

    @Test
    fun `getBitmap - should scale the bitmap with the density`() {
        val sut = SpeedLimitBitmapRenderer(scale = 2f)

        val mutcd = sut.getBitmap(SpeedLimitSign.MUTCD)
        assertEquals(MutcdSpeedLimitDrawable.WIDTH * 2, mutcd.width)
        assertEquals(MutcdSpeedLimitDrawable.HEIGHT * 2, mutcd.height)

        val vienna = sut.getBitmap(SpeedLimitSign.VIENNA)
        assertEquals(ViennaSpeedLimitDrawable.WIDTH * 2, vienna.width)
        assertEquals(ViennaSpeedLimitDrawable.HEIGHT * 2, vienna.height)
    }

    @Test
    fun `getBitmap - alternates between two scaled bitmaps at a scale above 1`() {
        val sut = SpeedLimitBitmapRenderer(scale = 2f)

        val bmp1 = sut.getBitmap(SpeedLimitSign.MUTCD)
        val bmp2 = sut.getBitmap(SpeedLimitSign.MUTCD)
        val bmp3 = sut.getBitmap(SpeedLimitSign.MUTCD)

        assertNotSame(bmp1, bmp2)
        assertSame(bmp1, bmp3)
        assertEquals(MutcdSpeedLimitDrawable.WIDTH * 2, bmp2.width)
    }
}
