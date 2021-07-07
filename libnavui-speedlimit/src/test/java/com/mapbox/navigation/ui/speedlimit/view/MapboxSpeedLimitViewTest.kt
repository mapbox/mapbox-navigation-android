package com.mapbox.navigation.ui.speedlimit.view

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.speedlimit.R
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class MapboxSpeedLimitViewTest {

    lateinit var ctx: Context

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `constructor with context`() {
        val view = MapboxSpeedLimitView(ctx)

        assertNotNull(view.currentTextColor)
    }

    @Test
    fun `constructor with context and attr`() {
        val view = MapboxSpeedLimitView(ctx, null)
        val expectedColor = ctx.getColor(R.color.mapbox_speed_limit_text_color)

        assertEquals(expectedColor, view.currentTextColor)
    }

    @Test
    fun `constructor with context attr and defStyleAttr`() {
        val view = MapboxSpeedLimitView(ctx, null)
        val expectedColor = ctx.getColor(R.color.mapbox_speed_limit_text_color)

        assertEquals(expectedColor, view.currentTextColor)
    }

    @Test
    fun renderUpdateSpeedLimit_whenMPH_setsSpeedLimitText() = coroutineRule.runBlockingTest {
        val state = UpdateSpeedLimitValue(
            35,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(ExpectedFactory.createValue(state))
        }

        assertEquals("MAX\n20", view.text.toString())
    }

    @Test
    fun renderUpdateSpeedLimit_slow_MUTCD() = coroutineRule.runBlockingTest {
        val state = UpdateSpeedLimitValue(
            5,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(ExpectedFactory.createValue(state))
        }

        assertEquals("MAX\n5", view.text.toString())
    }

    @Test
    fun renderUpdateSpeedLimit_whenKPH_setsSpeedLimitText() = coroutineRule.runBlockingTest {
        val state = UpdateSpeedLimitValue(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.VIENNA,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(ExpectedFactory.createValue(state))
        }

        assertEquals("35", view.text.toString())
    }

    @Test
    fun renderUpdateSpeedLimit_whenMUTCD_setsBackground() = coroutineRule.runBlockingTest {
        val state = UpdateSpeedLimitValue(
            35,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(ExpectedFactory.createValue(state))
        }

        assertNotNull(view.background)
    }

    @Test
    fun renderUpdateSpeedLimit_whenVienna_setsBackground() = coroutineRule.runBlockingTest {
        val state = UpdateSpeedLimitValue(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.VIENNA,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(ExpectedFactory.createValue(state))
        }

        assertNotNull(view.background)
    }

    @Test
    fun getViewDrawable_when_MUTCD_hasCorrectNumChildren() {
        val view = MapboxSpeedLimitView(ctx)

        val drawable = view.getViewDrawable(SpeedLimitSign.MUTCD)

        assertEquals(3, drawable.numberOfLayers)
    }

    @Test
    fun getViewDrawable_when_VIENNA_hasCorrectNumChildren() {
        val view = MapboxSpeedLimitView(ctx)

        val drawable = view.getViewDrawable(SpeedLimitSign.VIENNA)

        assertEquals(3, drawable.numberOfLayers)
    }

    @Test
    fun getSizeSpanStartIndex_MUTCD() {
        val view = MapboxSpeedLimitView(ctx)

        val result = view.getSizeSpanStartIndex(SpeedLimitSign.MUTCD, "MAX\n35")

        assertEquals(4, result)
    }

    @Test
    fun getSizeSpanStartIndex_VIENNA() {
        val view = MapboxSpeedLimitView(ctx)

        val result = view.getSizeSpanStartIndex(SpeedLimitSign.VIENNA, "whatever")

        assertEquals(0, result)
    }
}
