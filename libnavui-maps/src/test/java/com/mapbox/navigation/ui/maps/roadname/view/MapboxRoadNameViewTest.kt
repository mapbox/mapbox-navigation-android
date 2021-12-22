package com.mapbox.navigation.ui.maps.roadname.view

import android.content.Context
import android.text.SpannedString
import android.text.style.ImageSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.ui.shield.model.RouteShield
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRoadNameViewTest {

    private lateinit var ctx: Context

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
        val roadComponent1 = mockk<RoadComponent> {
            every { text } returns "I-880"
            every { shield } returns null
            every { imageBaseUrl } returns "https://mapbox-navigation-shields.s3"
        }
        val roadComponent2 = mockk<RoadComponent> {
            every { text } returns "North"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent1, roadComponent2)
        }

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(
            listOf(
                ExpectedFactory.createValue(
                    mockk {
                        every { shield } returns mockk<RouteShield.MapboxLegacyShield> {
                            every { initialUrl } returns "https://mapbox-navigation-shields.s3"
                            every { url } returns "https://mapbox-navigation-shields.s3.svg"
                            every { byteArray } returns byteArrayOf(1, 2, 3, 4)
                            every { toBitmap(any(), any()) } returns mockk(relaxed = true)
                            every { compareWith(any()) } returns true
                        }
                    }
                )
            )
        )
        val spans = (view.text as SpannedString).getSpans(0, 11, ImageSpan::class.java)

        assertEquals(1, spans.size)
    }

    @Test
    fun `when render road name with mapbox shield`() {
        val roadComponent1 = mockk<RoadComponent> {
            every { text } returns "I-880"
            every { shield } returns MapboxShield.builder()
                .name("us-interstate")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .textColor("white")
                .displayRef("880")
                .build()
            every { imageBaseUrl } returns null
        }
        val roadComponent2 = mockk<RoadComponent> {
            every { text } returns "North"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent1, roadComponent2)
        }
        val textLength = road.components.joinToString(" ") { it.text }.length

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(
            listOf(
                ExpectedFactory.createValue(
                    mockk {
                        every { shield } returns mockk<RouteShield.MapboxDesignedShield> {
                            every { url } returns "https://mapbox.shields.com/url1"
                            every { byteArray } returns byteArrayOf(1, 2, 3, 4)
                            every { mapboxShield } returns mockk(relaxed = true)
                            every { shieldSprite } returns mockk(relaxed = true)
                            every { toBitmap(any(), any()) } returns mockk(relaxed = true)
                            every { compareWith(any()) } returns true
                        }
                    }
                )
            )
        )
        val spans = (view.text as SpannedString).getSpans(0, textLength, ImageSpan::class.java)

        assertEquals(1, spans.size)
    }

    @Test
    fun `when render road name with shield that does not exist`() {
        val roadComponent1 = mockk<RoadComponent> {
            every { text } returns "I-880"
            every { shield } returns MapboxShield.builder()
                .name("us-interstate")
                .baseUrl("https://api.mapbox.com/styles/v1")
                .textColor("white")
                .displayRef("880")
                .build()
            every { imageBaseUrl } returns null
        }
        val roadComponent2 = mockk<RoadComponent> {
            every { text } returns "North"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent1, roadComponent2)
        }
        val textLength = road.components.joinToString(" ") { it.text }.length

        val view = MapboxRoadNameView(context = ctx)

        view.renderRoadName(road)
        view.renderRoadNameWith(
            listOf(
                ExpectedFactory.createValue(
                    mockk {
                        every { shield } returns mockk<RouteShield.MapboxDesignedShield> {
                            every { url } returns "https://mapbox.shields.com/url1"
                            every { byteArray } returns byteArrayOf(1, 2, 3, 4)
                            every { mapboxShield } returns mockk(relaxed = true)
                            every { shieldSprite } returns mockk(relaxed = true)
                            every { toBitmap(any(), any()) } returns mockk(relaxed = true)
                            every { compareWith(any()) } returns false
                        }
                    }
                )
            )
        )
        val spans = (view.text as SpannedString).getSpans(0, textLength, ImageSpan::class.java)

        assertEquals(0, spans.size)
    }
}
