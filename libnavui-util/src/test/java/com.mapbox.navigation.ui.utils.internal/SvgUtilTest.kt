package com.mapbox.navigation.ui.utils.internal

import android.graphics.Bitmap
import com.caverock.androidsvg.SVGExternalFileResolver
import com.caverock.androidsvg.SVGParseException
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SvgUtilTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val validSvg = "<?xml version='1.0' encoding='utf8'?>\n" +
        "<svg xmlns=\"http://www.w3.org/2000/svg\" baseProfile=\"basic\" " +
        "contentScriptType=\"text/ecmascript\" contentStyleType=\"text/css\" " +
        "id=\"SI_1241914001\" preserveAspectRatio=\"xMidYMid meet\" version=\"1.1\" " +
        "viewBox=\"0 0 220 170\" x=\"0px\" y=\"0px\" zoomAndPan=\"magnify\">\n" +
        "  <g id=\"Signs\" transform=\"translate(-359.108002,0.0)\">\n" +
        "    <g id=\"SIGN_R1\">\n" +
        "      <g id=\"A2\" type=\"ExitNumber\">\n" +
        "        <g id=\"A5\" type=\"Background\">\n" +
        "          <rect class=\"background_fill_22 background_stroke_5\" fill=\"#FCFFFF\" " +
        "height=\"31.9\" rx=\"6\" ry=\"6\" stroke=\"#14171C\" stroke-width=\"3\" " +
        "width=\"220.0\" x=\"359.108002\" y=\"0.000000\" />\n" +
        "        </g>\n" +
        "      </g>\n" +
        "      <g id=\"A3\" type=\"Panel\">\n" +
        "        <g id=\"A6\" type=\"Background\">\n" +
        "          <rect class=\"background_fill_22 background_stroke_5\" fill=\"#FCFFFF\" " +
        "height=\"137.1\" rx=\"6\" ry=\"6\" stroke=\"#14171C\" stroke-width=\"3\" " +
        "width=\"220.0\" x=\"359.108002\" y=\"32.900002\" />\n" +
        "        </g>\n" +
        "        <g id=\"A9\" type=\"Shield\">\n" +
        "          <g>\n" +
        "            <rect class=\"shield_fill_19\" fill=\"#EB3B1C\" " +
        "height=\"22.424999\" rx=\"3.640000\" ry=\"2.990000\" width=\"54.599997\" " +
        "x=\"441.808006\" y=\"47.400002\" />\n" +
        "          </g>\n" +
        "          <text class=\"text_fill_5 text_font-family_1 text_font-weight_2\" " +
        "fill=\"#14171C\" font-size=\"17.549999\" font-weight=\"bold\" text-anchor=\"middle\" " +
        "x=\"468.783005\" y=\"64.300001\">M-100</text>\n" +
        "        </g>\n" +
        "        <g id=\"A17\" type=\"Text\">\n" +
        "          <text class=\"text_fill_5 text_font-family_1 text_font-weight_2\" " +
        "fill=\"#14171C\" font-size=\"26\" font-weight=\"bold\" x=\"429.507088\" " +
        "y=\"99.900263\">Cobeña</text>\n" +
        "        </g>\n" +
        "        <g id=\"A18\" type=\"Text\">\n" +
        "          <text class=\"text_fill_5 text_font-family_1 text_font-weight_2\" " +
        "fill=\"#14171C\" font-size=\"26\" font-weight=\"bold\" x=\"436.624931\" " +
        "y=\"132.508596\">Algete</text>\n" +
        "        </g>\n" +
        "      </g>\n" +
        "    </g>\n" +
        "  </g>\n" +
        "  <style type=\"text/css\">\n" +
        "    @import url(\"customcolors.css\");\n" +
        "  </style>\n" +
        "</svg>"

    private val invalidSvg = "<?xml version='1.0' encoding='utf8'?>\n" +
        "<svg xmlns=\"http://www.w3.org/2000/svg\" baseProfile=\"basic\" " +
        "contentScriptType=\"text/ecmascript\" contentStyleType=\"text/css\" " +
        "id=\"SI_1241914001\" preserveAspectRatio=\"xMidYMid meet\" version=\"1.1\" x=\"0px\" " +
        "y=\"0px\" zoomAndPan=\"magnify\">\n" +
        "  <g id=\"Signs\" transform=\"translate(-359.108002,0.0)\">\n" +
        "    <g id=\"SIGN_R1\">\n" +
        "      <g id=\"A2\" type=\"ExitNumber\">\n" +
        "        <g id=\"A5\" type=\"Background\">\n" +
        "          <rect class=\"background_fill_22 background_stroke_5\" fill=\"#FCFFFF\" " +
        "height=\"31.9\" rx=\"6\" ry=\"6\" stroke=\"#14171C\" stroke-width=\"3\" " +
        "width=\"220.0\" x=\"359.108002\" y=\"0.000000\" />\n" +
        "        </g>\n" +
        "      </g>\n" +
        "      <g id=\"A3\" type=\"Panel\">\n" +
        "        <g id=\"A6\" type=\"Background\">\n" +
        "          <rect class=\"background_fill_22 background_stroke_5\" fill=\"#FCFFFF\" " +
        "height=\"137.1\" rx=\"6\" ry=\"6\" stroke=\"#14171C\" stroke-width=\"3\" " +
        "width=\"220.0\" x=\"359.108002\" y=\"32.900002\" />\n" +
        "        </g>\n" +
        "        <g id=\"A9\" type=\"Shield\">\n" +
        "          <g>\n" +
        "            <rect class=\"shield_fill_19\" fill=\"#EB3B1C\" height=\"22.424999\" " +
        "rx=\"3.640000\" ry=\"2.990000\" width=\"54.599997\" " +
        "x=\"441.808006\" y=\"47.400002\" />\n" +
        "          </g>\n" +
        "          <text class=\"text_fill_5 text_font-family_1 text_font-weight_2\" " +
        "fill=\"#14171C\" font-size=\"17.549999\" font-weight=\"bold\" text-anchor=\"middle\" " +
        "x=\"468.783005\" y=\"64.300001\">M-100</text>\n" +
        "        </g>\n" +
        "        <g id=\"A17\" type=\"Text\">\n" +
        "          <text class=\"text_fill_5 text_font-family_1 text_font-weight_2\" " +
        "fill=\"#14171C\" font-size=\"26\" font-weight=\"bold\" x=\"429.507088\" " +
        "y=\"99.900263\">Cobeña</text>\n" +
        "        </g>\n" +
        "        <g id=\"A18\" type=\"Text\">\n" +
        "          <text class=\"text_fill_5 text_font-family_1 text_font-weight_2\" " +
        "fill=\"#14171C\" font-size=\"26\" font-weight=\"bold\" x=\"436.624931\" " +
        "l" +
        "y=\"132.508596\">Algete</text>\n" +
        "        </g>\n" +
        "      </g>\n" +
        "    </g>\n" +
        "  </g>\n" +
        "  <style type=\"text/css\">\n" +
        "    @import url(\"customcolors.css\");\n" +
        "  </style>\n" +
        "</svg>"

    @Test
    fun `render bitmap with height check bitmap properties`() {
        val mockHeight = 400
        val mockAspecRatio = 0.7727272727
        val mockWidth: Int = (mockHeight / mockAspecRatio).toInt()
        val mockStream = ByteArrayInputStream(validSvg.toByteArray())
        val mockSignboard = Bitmap.createBitmap(
            mockWidth,
            mockHeight,
            Bitmap.Config.ARGB_8888,
        )

        val actual = SvgUtil.renderAsBitmapWithHeight(mockStream, mockHeight, "")

        assertEquals(mockSignboard.height, actual?.height)
        assertEquals(mockSignboard.width, actual?.width)
        assertEquals(mockSignboard.config, actual?.config)
    }

    @Test
    fun `render bitmap with height bitmap non null`() {
        val mockHeight = 400
        val mockStream = ByteArrayInputStream(validSvg.toByteArray())

        val actual = SvgUtil.renderAsBitmapWithHeight(mockStream, mockHeight, "")

        assertNotNull(actual)
    }

    @Test
    fun `render bitmap with height bitmap null`() {
        val mockHeight = 400
        val mockStream = ByteArrayInputStream(byteArrayOf(12, 55, 98))

        val actual = SvgUtil.renderAsBitmapWithHeight(mockStream, mockHeight, "")

        assertNull(actual)
    }

    @Test
    fun `render bitmap with height css styles null`() {
        val mockHeight = 400
        val mockStream = ByteArrayInputStream(byteArrayOf(12, 55, 98))

        val actual = SvgUtil.renderAsBitmapWithHeight(mockStream, mockHeight)

        assertNull(actual)
    }

    @Test
    fun `render bitmap with height bitmap null no viewBox`() {
        val mockHeight = 400
        val mockStream = ByteArrayInputStream(invalidSvg.toByteArray())

        val actual = SvgUtil.renderAsBitmapWithHeight(mockStream, mockHeight, "")

        assertNull(actual)
    }

    @Test
    fun `render bitmap with width check bitmap properties`() =
        coroutineRule.runBlockingTest {
            val mockResolver = mockk<SVGExternalFileResolver>()
            val mockWidth = 400
            val mockAspecRatio = 0.7727272727
            val mockHeight: Int = (mockWidth * mockAspecRatio).toInt()
            val mockStream = ByteArrayInputStream(validSvg.toByteArray())
            val mockSignboard = Bitmap.createBitmap(
                mockWidth,
                mockHeight,
                Bitmap.Config.ARGB_8888,
            )

            val actual = SvgUtil.renderAsBitmapWithWidth(
                mockStream,
                mockWidth,
                "",
                mockResolver,
            )

            assertEquals(mockSignboard.height, actual.height)
            assertEquals(mockSignboard.width, actual.width)
            assertEquals(mockSignboard.config, actual.config)
        }

    @Test
    fun `render bitmap with width bitmap non null`() =
        coroutineRule.runBlockingTest {
            val mockResolver = mockk<SVGExternalFileResolver>()
            val mockWidth = 400
            val mockStream = ByteArrayInputStream(validSvg.toByteArray())

            val actual = SvgUtil.renderAsBitmapWithWidth(
                mockStream,
                mockWidth,
                "",
                mockResolver,
            )

            assertNotNull(actual)
        }

    @Test
    fun `render bitmap with width bitmap null`() =
        coroutineRule.runBlockingTest {
            val mockResolver = mockk<SVGExternalFileResolver>()
            val mockWidth = 400
            val mockStream = ByteArrayInputStream(byteArrayOf(12, 55, 98))

            assertThrows(SVGParseException::class.java) {
                SvgUtil.renderAsBitmapWithWidth(
                    mockStream,
                    mockWidth,
                    "",
                    mockResolver,
                )
            }
        }

    @Test
    fun `render bitmap with width css styles null`() =
        coroutineRule.runBlockingTest {
            val mockResolver = mockk<SVGExternalFileResolver>()
            val mockWidth = 400
            val mockStream = ByteArrayInputStream(byteArrayOf(12, 55, 98))

            assertThrows(SVGParseException::class.java) {
                SvgUtil.renderAsBitmapWithWidth(
                    mockStream,
                    mockWidth,
                    "",
                    mockResolver,
                )
            }
        }

    @Test
    fun `render bitmap with width bitmap null no viewBox`() =
        coroutineRule.runBlockingTest {
            val mockResolver = mockk<SVGExternalFileResolver>()
            val mockWidth = 400
            val mockStream = ByteArrayInputStream(invalidSvg.toByteArray())

            assertThrows("SVG's viewBox is null", Exception::class.java) {
                SvgUtil.renderAsBitmapWithWidth(
                    mockStream,
                    mockWidth,
                    "",
                    mockResolver,
                )
            }
        }
}
