package com.mapbox.navigation.ui.maps.route.arrow.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Image
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowAddedValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowVisibilityChangeValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.RemoveArrowValue
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRouteArrowViewTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun render_UpdateRouteArrowVisibilityState() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(RouteArrowUtils)
        val arrowLayer = mockk<Layer>(relaxed = true)
        val options = RouteArrowOptions.Builder(ctx).build()
        val state = ArrowVisibilityChangeValue(
            listOf(Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.NONE))
        )
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns arrowLayer
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteArrowView(options).render(style, state)

        verify { arrowLayer.visibility(Visibility.NONE) }
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun render_UpdateManeuverArrowValue() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val arrowShaftSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowHeadSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowLayer = mockk<Layer>(relaxed = true)
        val arrowShaftFeature = mockk<Feature> {
            every { toJson() } returns "{}"
        }
        val arrowHeadFeature = mockk<Feature> {
            every { toJson() } returns "{}"
        }
        val state = UpdateManeuverArrowValue(
            listOf(Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.NONE)),
            arrowShaftFeature,
            arrowHeadFeature
        )
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getSource(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns arrowHeadSource
            every { getSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns arrowShaftSource
            every { getLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns arrowLayer
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteArrowView(options).renderManeuverUpdate(
            style,
            ExpectedFactory.createValue(state)
        )

        verify { arrowShaftSource.feature(state.arrowShaftFeature!!) }
        verify { arrowHeadSource.feature(state.arrowHeadFeature!!) }
        verify { arrowLayer.visibility(Visibility.NONE) }
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun render_AddArrowState_initializesLayers() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val arrowShaftSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowHeadSource = mockk<GeoJsonSource>(relaxed = true)
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getSource(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns arrowHeadSource
            every { getSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns arrowShaftSource
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val state = ArrowAddedValue(
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf())
        )

        MapboxRouteArrowView(options).render(style, state)

        verify { arrowShaftSource.featureCollection(state.arrowShaftFeatureCollection) }
        verify { arrowHeadSource.featureCollection(state.arrowHeadFeatureCollection) }
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun render_AddArrowState() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val arrowShaftSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowHeadSource = mockk<GeoJsonSource>(relaxed = true)
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getSource(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns arrowHeadSource
            every { getSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns arrowShaftSource
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val featureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.5234885,37.9754331]]}}"
        val arrowShaftFeature = Feature.fromJson(featureJson)
        val arrowHeadFeature = Feature.fromJson(featureJson)
        val arrowShaftFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowShaftFeature))
        val arrowHeadFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowHeadFeature))
        val state = ArrowAddedValue(
            arrowShaftFeatureCollection,
            arrowHeadFeatureCollection
        )

        MapboxRouteArrowView(options).render(style, state)

        verify { arrowShaftSource.featureCollection(arrowShaftFeatureCollection) }
        verify { arrowHeadSource.featureCollection(arrowHeadFeatureCollection) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun render_ExpectedAddArrowState() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(RouteArrowUtils)
        val arrowShaftSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowHeadSource = mockk<GeoJsonSource>(relaxed = true)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getSource(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns arrowHeadSource
            every { getSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns arrowShaftSource
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val featureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.5234885,37.9754331]]}}"
        val arrowShaftFeature = Feature.fromJson(featureJson)
        val arrowHeadFeature = Feature.fromJson(featureJson)
        val arrowShaftFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowShaftFeature))
        val arrowHeadFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowHeadFeature))
        val state: Expected<InvalidPointError, ArrowAddedValue> = ExpectedFactory.createValue(
            ArrowAddedValue(
                arrowShaftFeatureCollection,
                arrowHeadFeatureCollection
            )
        )

        MapboxRouteArrowView(options).render(style, state)

        verify { arrowShaftSource.featureCollection(arrowShaftFeatureCollection) }
        verify { arrowHeadSource.featureCollection(arrowHeadFeatureCollection) }
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun render_RemoveArrowState() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(RouteArrowUtils)
        val arrowShaftSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowHeadSource = mockk<GeoJsonSource>(relaxed = true)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getSource(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns arrowHeadSource
            every { getSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns arrowShaftSource
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val featureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.5234885,37.9754331]]}}"
        val arrowShaftFeature = Feature.fromJson(featureJson)
        val arrowHeadFeature = Feature.fromJson(featureJson)
        val arrowShaftFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowShaftFeature))
        val arrowHeadFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowHeadFeature))
        val state = RemoveArrowValue(
            arrowShaftFeatureCollection,
            arrowHeadFeatureCollection
        )

        MapboxRouteArrowView(options).render(style, state)

        verify { arrowShaftSource.featureCollection(arrowShaftFeatureCollection) }
        verify { arrowHeadSource.featureCollection(arrowHeadFeatureCollection) }
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun render_ClearArrowsState() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val arrowShaftSource = mockk<GeoJsonSource>(relaxed = true)
        val arrowHeadSource = mockk<GeoJsonSource>(relaxed = true)
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getSource(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns arrowHeadSource
            every { getSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns arrowShaftSource
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val featureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.5234885,37.9754331]]}}"
        val arrowShaftFeature = Feature.fromJson(featureJson)
        val arrowHeadFeature = Feature.fromJson(featureJson)
        val arrowShaftFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowShaftFeature))
        val arrowHeadFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowHeadFeature))
        val state = ClearArrowsValue(
            arrowShaftFeatureCollection,
            arrowHeadFeatureCollection
        )

        MapboxRouteArrowView(options).render(style, state)

        verify { arrowShaftSource.featureCollection(arrowShaftFeatureCollection) }
        verify { arrowHeadSource.featureCollection(arrowHeadFeatureCollection) }
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
        unmockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
    }

    @Test
    fun getVisibility() {
        mockkObject(MapboxRouteLineUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style>()
        every {
            MapboxRouteLineUtils.getLayerVisibility(
                style,
                RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID
            )
        } returns Visibility.VISIBLE

        val result = MapboxRouteArrowView(options).getVisibility(style)

        assertEquals(Visibility.VISIBLE, result)
        unmockkObject(MapboxRouteLineUtils)
    }

    private fun mockCheckForLayerInitialization(style: Style) {
        val mockImage = mockk<Image>()

        with(style) {
            every { styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
            every { getStyleImage(RouteConstants.ARROW_HEAD_ICON) } returns mockImage
            every { getStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) } returns mockImage
        }
    }
}
