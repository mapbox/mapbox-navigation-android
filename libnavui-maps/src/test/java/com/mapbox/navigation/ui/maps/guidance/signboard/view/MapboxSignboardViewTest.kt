package com.mapbox.navigation.ui.maps.guidance.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxSignboardViewTest {

    private lateinit var ctx: Context
    @get:Rule
    private var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
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

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `render signboard null when expected failure`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Failure(SignboardError("whatever", null))
        val expected = null

        view.render(signboard)

        assertEquals(expected, Shadows.shadowOf((view.drawable as BitmapDrawable)).source)
    }

    @Test
    fun `render signboard visibility hide when expected failure`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Failure(SignboardError("whatever", null))
        val expected = GONE

        view.render(signboard)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render signboard visibility hide when expected success svg empty`() {
        val view = MapboxSignboardView(ctx)
        val signboard = Expected.Success(SignboardValue(byteArrayOf()))
        val expected = GONE

        view.render(signboard)

        assertEquals(expected, view.visibility)
    }

    @Test
    fun `render signboard visibility hide when expected success svg invalid`() =
        coroutineRule.runBlockingTest {
            val invalidSignboard = byteArrayOf(12, 11, 23)
            val mockSignboardOptions = MapboxSignboardOptions.Builder().build()
            val mockSignboardValue = mockk<SignboardValue>()
            every { mockSignboardValue.bytes } returns invalidSignboard
            every { mockSignboardValue.options } returns mockSignboardOptions
            mockkStatic(SvgUtil::class)
            coEvery { SvgUtil.renderAsBitmapWithWidth(any(), any(), any()) } returns null
            val view = MapboxSignboardView(ctx)
            val signboard = Expected.Success(mockSignboardValue)
            val expected = GONE

            view.render(signboard)
            assertEquals(expected, view.visibility)
        }

    @Test
    fun `render signboard visibility show when expected success svg valid`() =
        coroutineRule.runBlockingTest {
            val validSignboard = validSvg.toByteArray()
            val mockSignboardOptions = MapboxSignboardOptions.Builder().build()
            val mockSignboardValue = mockk<SignboardValue>()
            every { mockSignboardValue.bytes } returns validSignboard
            every { mockSignboardValue.options } returns mockSignboardOptions
            val mockBitmap = Bitmap.createBitmap(
                mockSignboardValue.options.desiredSignboardWidth,
                200,
                Bitmap.Config.ARGB_8888
            )
            mockkStatic(SvgUtil::class)
            coEvery { SvgUtil.renderAsBitmapWithWidth(any(), any(), any()) } returns mockBitmap
            val view = MapboxSignboardView(ctx)
            val signboard = Expected.Success(mockSignboardValue)
            val expected = VISIBLE

            view.render(signboard)
            assertEquals(expected, view.visibility)
        }
}
