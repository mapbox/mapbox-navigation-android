package com.mapbox.navigation.ui.components.speedlimit.view

import android.content.Context
import android.os.Build
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.tripdata.speedlimit.internal.SpeedInfoValueFactory.createSpeedInfoValue
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.speedlimit.model.MapboxSpeedInfoOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxSpeedInfoViewTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxSpeedInfoView(ctx)

        assertNotNull(view.speedInfoMutcdLayout)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxSpeedInfoView(ctx, null)

        assertEquals(
            R.drawable.background_mutcd_outer_layout,
            shadowOf(view.speedInfoMutcdLayout.background).createdFromResId,
        )
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxSpeedInfoView(ctx, null)

        assertEquals(
            R.drawable.background_mutcd_outer_layout,
            shadowOf(view.speedInfoMutcdLayout.background).createdFromResId,
        )
    }

    @Test
    fun `mutcd based view is rendered when input convention is mutcd`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertTrue(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `mutcd based view is rendered when input convention and option sign is mutcd`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().renderWithSpeedSign(SpeedLimitSign.MUTCD).build()
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.applyOptions(speedInfoOption)
        view.render(expected)

        assertTrue(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `vienna based view is rendered when input convention is mutcd and option sign is vienna`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().renderWithSpeedSign(SpeedLimitSign.VIENNA).build()
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.applyOptions(speedInfoOption)
        view.render(expected)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `vienna based view is rendered when input convention is vienna`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.VIENNA,
        )

        view.render(expected)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `vienna based view is rendered when input convention and option sign is vienna`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().renderWithSpeedSign(SpeedLimitSign.VIENNA).build()
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.VIENNA,
        )

        view.applyOptions(speedInfoOption)
        view.render(expected)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `mutcd based view is rendered when input convention is vienna and option sign is mutcd`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().renderWithSpeedSign(SpeedLimitSign.MUTCD).build()
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.VIENNA,
        )

        view.applyOptions(speedInfoOption)
        view.render(expected)

        assertTrue(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `posted speed should contain mph`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.MILES_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertEquals("mph", view.speedInfoUnitTextMutcd.text)
    }

    @Test
    fun `posted speed should contain kmph`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertEquals("km/h", view.speedInfoUnitTextMutcd.text)
    }

    @Test
    fun `render posted and current speed`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 35,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertEquals(expected.currentSpeed.toString(), view.speedInfoCurrentSpeedMutcd.text)
        assertEquals(expected.postedSpeed.toString(), view.speedInfoPostedSpeedMutcd.text)
        assertEquals(expected.currentSpeed.toString(), view.speedInfoCurrentSpeedVienna.text)
        assertEquals(expected.postedSpeed.toString(), view.speedInfoPostedSpeedVienna.text)
    }

    @Test
    fun `show current speed`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertTrue(view.speedInfoCurrentSpeedMutcd.isVisible)
        assertTrue(view.speedInfoCurrentSpeedVienna.isVisible)
    }

    @Test
    fun `hide current speed speed when posted speed is 0`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 0,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertFalse(view.speedInfoCurrentSpeedMutcd.isVisible)
        assertFalse(view.speedInfoCurrentSpeedVienna.isVisible)
    }

    @Test
    fun `hide current speed speed when current speed is less than posted speed`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 40,
            postedSpeed = 60,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertFalse(view.speedInfoCurrentSpeedMutcd.isVisible)
        assertFalse(view.speedInfoCurrentSpeedVienna.isVisible)
    }

    @Test
    fun `default mutcd legend is hidden`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertFalse(view.speedInfoLegendTextMutcd.isVisible)
    }

    @Test
    fun `default mutcd unit is shown`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )

        view.render(expected)

        assertTrue(view.speedInfoUnitTextMutcd.isVisible)
    }

    @Test
    fun `apply options to hide unit and show legend`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 40,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.MUTCD,
        )
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().showLegend(true).showUnit(false).build()

        view.render(expected)
        view.applyOptions(speedInfoOption)

        assertFalse(view.speedInfoUnitTextMutcd.isVisible)
        assertTrue(view.speedInfoLegendTextMutcd.isVisible)
    }

    @Test
    fun `default option showSpeedWhenUnavailable is used and posted speed is null`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = null,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = null,
        )

        view.render(expected)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `option showSpeedWhenUnavailable is true and posted speed is null`() {
        val view = MapboxSpeedInfoView(ctx)
        val expected = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = null,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = null,
        )
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().showSpeedWhenUnavailable(true).build()

        view.applyOptions(speedInfoOption)
        view.render(expected)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)
        assertFalse(view.speedInfoCurrentSpeedMutcd.isVisible)
        assertEquals("", view.speedInfoCurrentSpeedMutcd.text)
        assertEquals("--", view.speedInfoPostedSpeedMutcd.text)
        assertFalse(view.speedInfoCurrentSpeedVienna.isVisible)
        assertEquals("", view.speedInfoCurrentSpeedVienna.text)
        assertEquals("--", view.speedInfoPostedSpeedVienna.text)
    }

    @Test
    fun `posted speed unavailable then available then unavailable`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfo1 = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = null,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = null,
        )
        val speedInfoOption =
            MapboxSpeedInfoOptions.Builder().showSpeedWhenUnavailable(true).build()

        view.applyOptions(speedInfoOption)
        view.render(speedInfo1)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)

        val speedInfo2 = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 60,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.VIENNA,
        )
        view.render(speedInfo2)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)

        view.render(speedInfo1)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `posted speed unavailable again unavailable and renderWithSign is set`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfo1 = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = null,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = null,
        )
        val speedInfoOption1 =
            MapboxSpeedInfoOptions
                .Builder()
                .showSpeedWhenUnavailable(true)
                .renderWithSpeedSign(SpeedLimitSign.VIENNA)
                .build()

        view.applyOptions(speedInfoOption1)
        view.render(speedInfo1)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)

        val speedInfoOption2 =
            MapboxSpeedInfoOptions
                .Builder()
                .showSpeedWhenUnavailable(true)
                .renderWithSpeedSign(SpeedLimitSign.MUTCD)
                .build()
        view.applyOptions(speedInfoOption2)
        view.render(speedInfo1)

        assertTrue(view.speedInfoMutcdLayout.isVisible)
        assertFalse(view.speedInfoViennaLayout.isVisible)
    }

    @Test
    fun `posted speed unavailable then available then unavailable and renderWithSign is set`() {
        val view = MapboxSpeedInfoView(ctx)
        val speedInfo1 = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = null,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = null,
        )
        val speedInfoOption1 =
            MapboxSpeedInfoOptions
                .Builder()
                .showSpeedWhenUnavailable(true)
                .renderWithSpeedSign(SpeedLimitSign.VIENNA)
                .build()

        view.applyOptions(speedInfoOption1)
        view.render(speedInfo1)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)

        val speedInfo2 = createSpeedInfoValue(
            currentSpeed = 45,
            postedSpeed = 60,
            postedSpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
            speedSignConvention = SpeedLimitSign.VIENNA,
        )
        val speedInfoOption2 =
            MapboxSpeedInfoOptions
                .Builder()
                .showSpeedWhenUnavailable(true)
                .renderWithSpeedSign(null)
                .build()
        view.applyOptions(speedInfoOption2)
        view.render(speedInfo2)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)

        view.render(speedInfo1)

        assertFalse(view.speedInfoMutcdLayout.isVisible)
        assertTrue(view.speedInfoViennaLayout.isVisible)
    }
}
