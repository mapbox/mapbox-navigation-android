package com.mapbox.navigation.ui.shield.model

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.N])
class RouteShieldTest {

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
        val mapboxLegacyShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )
        every { SvgUtil.renderAsBitmapWithHeight(any(), any(), any()) } returns mockBitmap

        val actual = mapboxLegacyShield.toBitmap(context.resources)

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
        val mapboxLegacyShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )
        every { SvgUtil.renderAsBitmapWithHeight(any(), any(), any()) } returns mockBitmap

        val actual = mapboxLegacyShield.toBitmap(context.resources, 333)

        assertNotNull(actual)
        assertEquals(mockBitmap.width, actual?.width)
        unmockkObject(SvgUtil)
    }

    @Test
    fun `when compare two legacy shields that have same initial url`() {
        val mapboxLegacyShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )
        val otherLegacyShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )

        val result = mapboxLegacyShield.compareWith(otherLegacyShield.initialUrl)

        assertTrue(result)
    }

    @Test
    fun `when compare two legacy shields that don't have same initial url`() {
        val mapboxLegacyShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy1.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy1",
        )
        val otherLegacyShield = RouteShield.MapboxLegacyShield(
            url = "https://shield.mapbox.legacy.svg",
            byteArrayOf(1),
            initialUrl = "https://shield.mapbox.legacy",
        )

        val result = mapboxLegacyShield.compareWith(otherLegacyShield.initialUrl)

        assertFalse(result)
    }

    @Test
    fun `when compare two mapbox designed shields where other is null`() {
        val mapboxDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed.us-interstate-3",
            byteArrayOf(1),
            mockk {
                every { name() } returns "us-interstate"
                every { textColor() } returns "white"
                every { baseUrl() } returns "https://api.mapbox.com/styles/v1"
                every { displayRef() } returns "880"
            },
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )

        val result = mapboxDesignedShield.compareWith(null)

        assertFalse(result)
    }

    @Test
    fun `when compare two mapbox designed shields that have same shields`() {
        val mapboxDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed.us-interstate-3",
            byteArrayOf(1),
            MapboxShield.builder()
                .name("us-interstate")
                .textColor("white")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .displayRef("880")
                .build(),
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )
        val otherDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed.us-interstate-3",
            byteArrayOf(1),
            MapboxShield.builder()
                .name("us-interstate")
                .textColor("white")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .displayRef("880")
                .build(),
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )

        val result = mapboxDesignedShield.compareWith(otherDesignedShield.mapboxShield)

        assertTrue(result)
    }

    @Test
    fun `when compare two mapbox designed shields that don't have same same shields`() {
        val mapboxDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed.us-interstate-3",
            byteArrayOf(1),
            MapboxShield.builder()
                .name("us-interstate")
                .textColor("white")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .displayRef("880")
                .build(),
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )
        val otherDesignedShield = RouteShield.MapboxDesignedShield(
            url = "https://shield.mapbox.designed.us-interstate-3",
            byteArrayOf(1),
            MapboxShield.builder()
                .name("us-interstate")
                .textColor("white")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .displayRef("990")
                .build(),
            mockk {
                every { spriteAttributes().width() } returns 72
                every { spriteAttributes().height() } returns 24
            }
        )

        val result = mapboxDesignedShield.compareWith(otherDesignedShield.mapboxShield)

        assertFalse(result)
    }
}
