package com.mapbox.navigation.ui.components.maneuver.model

import android.content.Context
import android.text.style.ImageSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.tripdata.shield.model.RouteShieldFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
class RoadShieldGeneratorTest {

    lateinit var ctx: Context

    private val mockShieldIcon: ByteArray = (
        "<?xml version='1.0' encoding='utf8'?>\n" +
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
            "fill=\"#14171C\" font-size=\"17.549999\" font-weight=\"bold\" " +
            "text-anchor=\"middle\" " +
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
        ).toByteArray()

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when road shield icon is null then spannable empty image span`() {
        val mockShieldText = "880"
        val mockDesiredHeight = 50
        val mockResources = ctx.resources

        val spannable = RoadShieldGenerator.styleAndGetRoadShield(
            mockShieldText,
            mockDesiredHeight,
            mockResources,
        )
        val imageSpan = spannable.getSpans(0, spannable.length, ImageSpan::class.java)

        assertTrue(spannable.isNotEmpty())
        assertTrue(imageSpan.isEmpty())
    }

    @Test
    fun `when road shield icon not null but empty then spannable has empty image span`() {
        val mockShieldText = "880"
        val mockDesiredHeight = 50
        val mockResources = ctx.resources
        val emptyShieldIcon = byteArrayOf()

        val spannable = RoadShieldGenerator.styleAndGetRoadShield(
            mockShieldText,
            mockDesiredHeight,
            mockResources,
            RouteShieldFactory.buildRouteShield(
                "https://mapbox_legacy_url.svg",
                emptyShieldIcon,
                "https://mapbox_legacy_url",
            ),
        )
        val imageSpan = spannable.getSpans(0, spannable.length, ImageSpan::class.java)

        assertTrue(spannable.isNotEmpty())
        assertTrue(imageSpan.isEmpty())
    }

    @Test
    fun `when road shield valid then spannable has contains image span`() {
        val mockShieldText = "880"
        val mockDesiredHeight = 50
        val mockResources = ctx.resources

        val spannable = RoadShieldGenerator.styleAndGetRoadShield(
            mockShieldText,
            mockDesiredHeight,
            mockResources,
            RouteShieldFactory.buildRouteShield(
                "https://mapbox_legacy_url.svg",
                mockShieldIcon,
                "https://mapbox_legacy_url",
            ),
        )
        val imageSpan = spannable.getSpans(0, spannable.length, ImageSpan::class.java)

        assertTrue(spannable.isNotEmpty())
        assertTrue(imageSpan.size == 1)
        assertNotNull(imageSpan[0].drawable)
    }
}
