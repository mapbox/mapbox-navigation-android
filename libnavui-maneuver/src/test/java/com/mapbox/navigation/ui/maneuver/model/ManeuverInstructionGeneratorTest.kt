package com.mapbox.navigation.ui.maneuver.model

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.ui.maneuver.view.MapboxExitText
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ManeuverInstructionGeneratorTest {

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockkObject(RoadShieldGenerator)
        mockContext = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldGenerator)
    }

    @Test
    fun `when generate primary instructions return spannable`() {
        val componentList = createComponentList()
        val mockManeuver = PrimaryManeuver
            .Builder()
            .text("I-880/Central Avenue")
            .type(StepManeuver.TURN)
            .degrees(null)
            .modifier(ManeuverModifier.SLIGHT_LEFT)
            .drivingSide(null)
            .componentList(componentList)
            .build()
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
        every {
            RoadShieldGenerator.styleAndGetRoadShield(any(), any(), any())
        } returns SpannableStringBuilder("I-880")
        val expected = SpannableStringBuilder("I-880 / Central Avenue ")

        val spannable = ManeuverInstructionGenerator.generatePrimary(
            mockContext,
            mockDesiredHeight,
            mockExitView,
            mockManeuver
        )

        assertEquals(expected, spannable)
    }

    @Test
    fun `when generate secondary instructions no maneuver return empty spannable`() {
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
        every {
            RoadShieldGenerator.styleAndGetRoadShield(any(), any(), any())
        } returns SpannableStringBuilder("I-880")

        val spannable = ManeuverInstructionGenerator.generateSecondary(
            mockContext,
            mockDesiredHeight,
            mockExitView,
            null
        )

        assertTrue(spannable.isEmpty())
    }

    @Test
    fun `when generate secondary instructions return spannable`() {
        val componentList = createComponentList()
        val mockManeuver = SecondaryManeuver
            .Builder()
            .text("I-880/Central Avenue")
            .type(StepManeuver.TURN)
            .degrees(null)
            .modifier(ManeuverModifier.SLIGHT_LEFT)
            .drivingSide(null)
            .componentList(componentList)
            .build()
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
        every {
            RoadShieldGenerator.styleAndGetRoadShield(any(), any(), any())
        } returns SpannableStringBuilder("I-880")
        val expected = SpannableStringBuilder("I-880 / Central Avenue ")

        val spannable = ManeuverInstructionGenerator.generateSecondary(
            mockContext,
            mockDesiredHeight,
            mockExitView,
            mockManeuver
        )

        assertEquals(expected, spannable)
    }

    @Test
    fun `when generate sub instructions no maneuver return empty spannable`() {
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
        every {
            RoadShieldGenerator.styleAndGetRoadShield(any(), any(), any())
        } returns SpannableStringBuilder("I-880")

        val spannable = ManeuverInstructionGenerator.generateSub(
            mockContext,
            mockDesiredHeight,
            mockExitView,
            null
        )

        assertTrue(spannable.isEmpty())
    }

    @Test
    fun `when generate sub instructions return spannable`() {
        val componentList = createComponentList()
        val mockManeuver = SubManeuver
            .Builder()
            .text("I-880/Central Avenue")
            .type(StepManeuver.TURN)
            .degrees(null)
            .modifier(ManeuverModifier.SLIGHT_LEFT)
            .drivingSide(null)
            .componentList(componentList)
            .build()
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
        every {
            RoadShieldGenerator.styleAndGetRoadShield(any(), any(), any())
        } returns SpannableStringBuilder("I-880")
        val expected = SpannableStringBuilder("I-880 / Central Avenue ")

        val spannable = ManeuverInstructionGenerator.generateSub(
            mockContext,
            mockDesiredHeight,
            mockExitView,
            mockManeuver
        )

        assertEquals(expected, spannable)
    }

    private fun createComponentList(): List<Component> {
        val roadShieldNumberComponent = Component(
            BannerComponents.ICON,
            RoadShieldComponentNode
                .Builder()
                .text("I-880")
                .build()
        )
        val delimiterComponentNode = Component(
            BannerComponents.DELIMITER,
            DelimiterComponentNode
                .Builder()
                .text("/")
                .build()
        )
        val textComponentNode = Component(
            BannerComponents.TEXT,
            TextComponentNode
                .Builder()
                .text("Central Avenue")
                .abbr(null)
                .abbrPriority(null)
                .build()
        )
        return listOf(
            roadShieldNumberComponent,
            delimiterComponentNode,
            textComponentNode
        )
    }
}
