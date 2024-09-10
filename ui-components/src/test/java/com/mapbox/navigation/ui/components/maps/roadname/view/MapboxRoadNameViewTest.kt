package com.mapbox.navigation.ui.components.maps.roadname.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.SpannedString
import android.text.style.ImageSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRoadNameViewTest {

    private lateinit var ctx: Context
    private val newShieldBitmap = mockk<Bitmap>(relaxed = true)
    private val legacyShieldBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when render only road name`() {
        val roadComponent = mockk<RoadComponent> {
            every { text } returns "I-880 North"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent)
        }
        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        val spans = (view.text as SpannedString).getSpans(0, 11, ImageSpan::class.java)

        assertEquals(0, spans.size)
    }

    @Test
    fun `when render road name with legacy shield without shieldName`() {
        val road = createRoadWithShields()

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(listOf(createMapboxLegacyShieldResult()))
        val spans = (view.text as SpannedString).getSpans(0, 11, ImageSpan::class.java)

        assertEquals(1, spans.size)
        val spanDrawable = spans.first().drawable
        assertTrue(spanDrawable is BitmapDrawable)
        assertEquals(legacyShieldBitmap, (spanDrawable as BitmapDrawable).bitmap)
    }

    @Test
    fun `when render road name with mapbox shield`() {
        val road = createRoadWithShields()
        val textLength = road.components.joinToString(" ") { it.text }.length

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(listOf(createMapboxDesignedShieldResult(suitable = true)))
        val spans = (view.text as SpannedString).getSpans(0, textLength, ImageSpan::class.java)

        assertEquals(1, spans.size)
        val spanDrawable = spans.first().drawable
        assertTrue(spanDrawable is BitmapDrawable)
        assertEquals(newShieldBitmap, (spanDrawable as BitmapDrawable).bitmap)
    }

    @Test
    fun `mapbox shield is preferred to legacy one`() {
        val road = createRoadWithShields()
        val textLength = road.components.joinToString(" ") { it.text }.length

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(
            listOf(
                createMapboxLegacyShieldResult(),
                createMapboxDesignedShieldResult(suitable = true),
            ),
        )
        val spans = (view.text as SpannedString).getSpans(0, textLength, ImageSpan::class.java)

        assertEquals(1, spans.size)
        val spanDrawable = spans.first().drawable
        assertTrue(spanDrawable is BitmapDrawable)
        assertEquals(newShieldBitmap, (spanDrawable as BitmapDrawable).bitmap)
    }

    @Test
    fun `when render road name with shield that does not exist`() {
        val road = createRoadWithShields()
        val textLength = road.components.joinToString(" ") { it.text }.length

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(listOf(createMapboxDesignedShieldResult(suitable = false)))
        val spans = (view.text as SpannedString).getSpans(0, textLength, ImageSpan::class.java)

        assertEquals(0, spans.size)
    }

    private fun createMapboxDesignedShieldResult(
        suitable: Boolean,
    ): Expected<RouteShieldError, RouteShieldResult> {
        return ExpectedFactory.createValue(
            mockk {
                every { shield } returns mockk<RouteShield.MapboxDesignedShield> {
                    every { url } returns "https://mapbox.shields.com/url1"
                    every { byteArray } returns byteArrayOf(1, 2, 3, 4)
                    every { mapboxShield } returns mockk(relaxed = true)
                    every { shieldSprite } returns mockk(relaxed = true)
                    every { toBitmap(any(), any()) } returns newShieldBitmap
                    every { compareWith(any()) } returns suitable
                }
            },
        )
    }

    private fun createMapboxLegacyShieldResult(): Expected<RouteShieldError, RouteShieldResult> {
        return ExpectedFactory.createValue(
            mockk {
                every { shield } returns mockk<RouteShield.MapboxLegacyShield> {
                    every { initialUrl } returns "https://mapbox-navigation-shields.s3"
                    every { url } returns "https://mapbox-navigation-shields.s3.svg"
                    every { byteArray } returns byteArrayOf(1, 2, 3, 4)
                    every { toBitmap(any(), any()) } returns legacyShieldBitmap
                    every { compareWith(any()) } returns true
                }
            },
        )
    }

    private fun createRoadWithShields(): Road {
        val roadComponent1 = mockk<RoadComponent> {
            every { text } returns "I-880"
            every { shield } returns MapboxShield.builder()
                .name("us-interstate")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .textColor("white")
                .displayRef("880")
                .build()
            every { imageBaseUrl } returns "https://mapbox-navigation-shields.s3"
        }
        val roadComponent2 = mockk<RoadComponent> {
            every { text } returns "North"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        return mockk {
            every { components } returns listOf(roadComponent1, roadComponent2)
        }
    }
}
