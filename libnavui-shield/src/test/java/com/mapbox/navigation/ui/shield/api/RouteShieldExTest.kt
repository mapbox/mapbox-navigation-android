package com.mapbox.navigation.ui.shield.api

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.N])
class RouteShieldExTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when get mapbox designed shield with default height`() {
        mockkObject(SvgUtil)
        val mockBitmap = mockk<Bitmap> {
            every { width } returns 72
            every { height } returns 24
        }
        val mapboxDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed",
            byteArrayOf(1),
            mockk(),
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )
        every { SvgUtil.renderAsBitmapWith(any(), any(), any(), any()) } returns mockBitmap

        val actual = mapboxDesignedShield.toBitmap(context.resources)

        assertNotNull(actual)
        assertEquals(mockBitmap.height, actual?.height)
        unmockkObject(SvgUtil)
    }

    @Test
    fun `when get mapbox designed shield with specified height`() {
        mockkObject(SvgUtil)
        val mockBitmap = mockk<Bitmap> {
            every { width } returns 124
            every { height } returns 255
        }
        val mapboxDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed",
            byteArrayOf(1),
            mockk(),
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )
        every { SvgUtil.renderAsBitmapWithHeight(any(), any(), any()) } returns mockBitmap

        val actual = mapboxDesignedShield.toBitmap(context.resources, 255)

        assertNotNull(actual)
        assertEquals(mockBitmap.width, actual?.width)
        unmockkObject(SvgUtil)
    }

    @Test
    fun `when get mapbox legacy shield with default height`() {
        mockkObject(SvgUtil)
        val mockBitmap = mockk<Bitmap> {
            every { width } returns 12
            every { height } returns 36
        }
        val mapboxDesignedShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )
        every { SvgUtil.renderAsBitmapWithHeight(any(), any(), any()) } returns mockBitmap

        val actual = mapboxDesignedShield.toBitmap(context.resources)

        assertNotNull(actual)
        assertEquals(mockBitmap.width, actual?.width)
        unmockkObject(SvgUtil)
    }

    @Test
    fun `when get mapbox legacy shield with specified height`() {
        mockkObject(SvgUtil)
        val mockBitmap = mockk<Bitmap> {
            every { width } returns 155
            every { height } returns 333
        }
        val mapboxDesignedShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )
        every { SvgUtil.renderAsBitmapWithHeight(any(), any(), any()) } returns mockBitmap

        val actual = mapboxDesignedShield.toBitmap(context.resources, 333)

        assertNotNull(actual)
        assertEquals(mockBitmap.width, actual?.width)
        unmockkObject(SvgUtil)
    }
}
