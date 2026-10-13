package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

class CarRoadLabelBitmapRendererTest : MapboxRobolectricTestRunner() {

    private val sut = CarRoadLabelBitmapRenderer()
    private val road = listOf(
        mockk<RoadComponent>(relaxed = true) {
            every { text } returns "Main Street"
            every { shield } returns null
            every { imageBaseUrl } returns null
        },
    )

    @Test
    fun `label is scaled with the density`() {
        val baseline = sut.render(resources(densityDpi = 160), road, emptyList())!!
        val doubled = sut.render(resources(densityDpi = 320), road, emptyList())!!

        // Text bounds are rounded separately at each size, so allow a pixel of rounding.
        assertEquals(baseline.width * 2f, doubled.width.toFloat(), 2f)
        assertEquals(baseline.height * 2f, doubled.height.toFloat(), 2f)
    }

    @Test
    fun `label without a width limit keeps its size`() {
        val unlimited = sut.render(resources(densityDpi = 160), road, emptyList())!!
        val wide = sut.render(
            resources(densityDpi = 160),
            road,
            emptyList(),
            maxWidthPx = unlimited.width,
        )!!

        assertEquals(unlimited.width, wide.width)
        assertEquals(unlimited.height, wide.height)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `long road name is ellipsized to the width limit`() {
        listOf(160, 320).forEach { densityDpi ->
            val resources = resources(densityDpi)
            val unlimited = sut.render(resources, longRoad, emptyList())!!
            val maxWidthPx = unlimited.width / 2

            val limited = sut.render(resources, longRoad, emptyList(), maxWidthPx = maxWidthPx)!!

            assertTrue(
                "densityDpi=$densityDpi width=${limited.width} max=$maxWidthPx",
                limited.width <= maxWidthPx,
            )
            assertEquals(unlimited.height.toFloat(), limited.height.toFloat(), 2f)
        }
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `components that do not fit are dropped`() {
        val resources = resources(densityDpi = 160)
        val first = sut.render(resources, road, emptyList())!!
        val twoRoads = road + longRoad

        val limited = sut.render(resources, twoRoads, emptyList(), maxWidthPx = first.width)!!

        assertEquals(first.width, limited.width)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `short text that does not fit is not rendered`() {
        val resources = resources(densityDpi = 160)
        val shortRoad = listOf(roadComponent("AB"))
        // The 20dp text padding at each side leaves 10px, less than "A" with an ellipsis.
        assertNull(sut.render(resources, shortRoad, emptyList(), maxWidthPx = 50))
    }

    @Test
    fun `shield wider than the width limit is not rendered`() {
        val resources = resources(densityDpi = 160)
        val shieldRoad = listOf(roadComponent("I-95", imageBaseUrl = SHIELD_URL))

        val label = sut.render(resources, shieldRoad, listOf(shield(width = 200)), maxWidthPx = 100)

        assertNull(label)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `text after a shield is ellipsized to the width limit`() {
        val resources = resources(densityDpi = 160)
        val shields = listOf(shield(width = 60))
        val shieldRoad = listOf(roadComponent("I-95", imageBaseUrl = SHIELD_URL)) + longRoad
        val shieldOnly = sut.render(resources, shieldRoad.take(1), shields)!!
        val unlimited = sut.render(resources, shieldRoad, shields)!!

        val limited = sut.render(resources, shieldRoad, shields, maxWidthPx = 250)!!

        assertTrue("width=${limited.width}", limited.width in shieldOnly.width + 1..250)
        assertTrue(limited.width < unlimited.width)
    }

    @Test
    fun `prefix does not split a surrogate pair`() {
        val text = "Rd \uD83D\uDE97 Exit"

        assertEquals("Rd ", text.takeWholeChars(4))
        assertEquals("Rd \uD83D\uDE97", text.takeWholeChars(5))
        assertEquals("Rd", text.takeWholeChars(2))
    }

    @Test
    fun `nothing is rendered when no text fits`() {
        assertNull(sut.render(resources(densityDpi = 160), road, emptyList(), maxWidthPx = 1))
    }

    private val longRoad = listOf(
        roadComponent("A Very Long Street Name That Does Not Fit The Car Screen"),
    )

    private fun roadComponent(text: String, imageBaseUrl: String? = null) =
        mockk<RoadComponent>(relaxed = true) {
            every { this@mockk.text } returns text
            every { shield } returns null
            every { this@mockk.imageBaseUrl } returns imageBaseUrl
        }

    private fun shield(width: Int) = mockk<RouteShield.MapboxLegacyShield> {
        every { compareWith(SHIELD_URL) } returns true
        every { toBitmap(any(), any()) } returns createBitmap(width, 18)
    }

    private fun resources(densityDpi: Int): Resources {
        val context: Context = ApplicationProvider.getApplicationContext()
        val configuration = Configuration(context.resources.configuration).also {
            it.densityDpi = densityDpi
        }
        return context.createConfigurationContext(configuration).resources
    }

    private companion object {
        private const val SHIELD_URL = "https://shield.test/i-95"
    }
}
