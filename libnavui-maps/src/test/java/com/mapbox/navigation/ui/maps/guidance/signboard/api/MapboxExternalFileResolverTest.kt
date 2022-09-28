package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.content.Context
import android.content.res.AssetManager
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxExternalFileResolverTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var ctx: Context
    private lateinit var assetManager: AssetManager
    private lateinit var externalFileResolver: MapboxExternalFileResolver

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        assetManager = ctx.assets
        externalFileResolver = MapboxExternalFileResolver(assetManager)
    }

    @Test
    fun `resolve font when font style normal font weight bold`() {
        val mockFontFamily = "Roboto"
        val mockFontStyle = "Normal"
        val mockFontWeight = 700

        val typeface =
            externalFileResolver.resolveFont(mockFontFamily, mockFontWeight, mockFontStyle)

        assertNotNull(typeface)
    }

    @Test
    fun `resolve font when font style italic font weight normal`() {
        val mockFontFamily = "Roboto"
        val mockFontStyle = "Italic"
        val mockFontWeight = 400

        val typeface =
            externalFileResolver.resolveFont(mockFontFamily, mockFontWeight, mockFontStyle)

        assertNotNull(typeface)
    }

    @Test
    fun `resolve font when font style italic font weight bold`() {
        val mockFontFamily = "Roboto"
        val mockFontStyle = "Italic"
        val mockFontWeight = 700

        val typeface =
            externalFileResolver.resolveFont(mockFontFamily, mockFontWeight, mockFontStyle)

        assertNotNull(typeface)
    }

    @Test
    fun `resolve font when font style normal font weight normal`() {
        val mockFontFamily = "Roboto"
        val mockFontStyle = "Normal"
        val mockFontWeight = 400

        val typeface =
            externalFileResolver.resolveFont(mockFontFamily, mockFontWeight, mockFontStyle)

        assertNotNull(typeface)
    }

    @Test fun `resolve font when font family file not available`() {
        val mockFontFamily = "MyFontFamily"
        val mockFontStyle = "Normal"
        val mockFontWeight = 400

        val typeface =
            externalFileResolver.resolveFont(mockFontFamily, mockFontWeight, mockFontStyle)

        assertNull(typeface)
    }
}
