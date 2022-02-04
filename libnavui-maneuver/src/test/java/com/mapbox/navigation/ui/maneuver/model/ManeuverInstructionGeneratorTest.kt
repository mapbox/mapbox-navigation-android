package com.mapbox.navigation.ui.maneuver.model

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.maneuver.view.MapboxExitText
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldFactory
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

private const val downloadUrl = "downloadUrl"
private const val initialUrl = "initialUrl"

@ExperimentalMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
class ManeuverInstructionGeneratorTest {

    private lateinit var mockContext: Context
    private val mapboxShield = mockk<MapboxShield>()

    @Before
    fun setUp() {
        mockkObject(RoadShieldGenerator)
        every {
            RoadShieldGenerator.styleAndGetRoadShield(any(), any(), any(), any())
        } answers {
            val shieldText = firstArg<String>()
            val fullText = when (lastArg<RouteShield?>()) {
                is RouteShield.MapboxDesignedShield -> "new shield ($shieldText)"
                is RouteShield.MapboxLegacyShield -> "legacy shield ($shieldText)"
                else -> shieldText
            }
            SpannableStringBuilder(fullText)
        }
        mockContext = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkObject(RoadShieldGenerator)
    }

    @Test
    fun `when generate primary instructions return spannable`() {
        val mockManeuver = createPrimaryManeuver()
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
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
    fun `legacy shield is returned if new one is not available`() {
        val expected = SpannableStringBuilder("legacy shield (I-880) / Central Avenue ")

        val spannable = ManeuverInstructionGenerator.generatePrimary(
            mockContext,
            desiredHeight = 50,
            mockk(relaxed = true),
            createPrimaryManeuver(),
            setOf(createLegacyShield()),
        )

        assertEquals(expected, spannable)
    }

    @Test
    fun `new shield is preferred to legacy one`() {
        val expected = SpannableStringBuilder("new shield (I-880) / Central Avenue ")

        val spannable = ManeuverInstructionGenerator.generatePrimary(
            mockContext,
            desiredHeight = 50,
            mockk(relaxed = true),
            createPrimaryManeuver(),
            setOf(createLegacyShield(), createNewShield()),
        )

        assertEquals(expected, spannable)
    }

    @Test
    fun `when generate secondary instructions no maneuver return empty spannable`() {
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)

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
        val mockManeuver = SecondaryManeuver(
            text = "I-880/Central Avenue",
            type = StepManeuver.TURN,
            degrees = null,
            modifier = ManeuverModifier.SLIGHT_LEFT,
            drivingSide = null,
            componentList = componentList
        )
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
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
        val mockManeuver = SubManeuver(
            text = "I-880/Central Avenue",
            type = StepManeuver.TURN,
            degrees = null,
            modifier = ManeuverModifier.SLIGHT_LEFT,
            drivingSide = null,
            componentList = componentList
        )
        val mockDesiredHeight = 50
        val mockExitView = mockk<MapboxExitText>(relaxed = true)
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
                .shieldUrl(initialUrl)
                .mapboxShield(mapboxShield)
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

    private fun createPrimaryManeuver(): PrimaryManeuver {
        return PrimaryManeuver(
            text = "I-880/Central Avenue",
            type = StepManeuver.TURN,
            degrees = null,
            modifier = ManeuverModifier.SLIGHT_LEFT,
            drivingSide = null,
            componentList = createComponentList(),
        )
    }

    private fun createNewShield(): RouteShield.MapboxDesignedShield {
        return RouteShieldFactory.buildRouteShield(
            downloadUrl,
            byteArrayOf(),
            mapboxShield,
            mockk(),
        )
    }

    private fun createLegacyShield(): RouteShield.MapboxLegacyShield {
        return RouteShieldFactory.buildRouteShield(downloadUrl, byteArrayOf(), initialUrl)
    }
}
