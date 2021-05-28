package com.mapbox.navigation.ui.maps.route.arrow.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Image
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.common.ShadowValueConverter
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowAddedValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowVisibilityChangeValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.arrow.model.RemoveArrowValue
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(shadows = [ShadowValueConverter::class])
@RunWith(NavSDKRobolectricTestRunner::class)
class MapboxRouteArrowViewTest {

    lateinit var ctx: Context

    private val layerTypeValue = mockk<Value> {
        every { contents } returns "line"
    }
    private val sourceValue = mockk<Value> {
        every { contents } returns "mapbox-navigation-route-source"
    }
    private val layerValue = mockk<Value> {
        every { contents } returns HashMap<String, Value>().also {
            it["type"] = layerTypeValue
            it["source"] = sourceValue
        }
    }
    private val layerPropertyExpected = mockk<Expected<String, Value>> {
        every { value.hint(Value::class) } returns layerValue
    }
    private val geoJsonSourceTypeValue = mockk<Value> {
        every { contents } returns "geojson"
    }
    private val geoJsonSourceValue = mockk<Value> {
        every { contents } returns HashMap<String, Value>().also {
            it["type"] = geoJsonSourceTypeValue
        }
    }
    private val geoJsonSourceExpected = mockk<Expected<String, Value>> {
        every { value.hint(Value::class) } returns geoJsonSourceValue
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun render_UpdateRouteArrowVisibilityState() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val state = ArrowVisibilityChangeValue(
            listOf(Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.NONE))
        )
        val visibilityValueSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteArrowView(options).render(style, state)

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                "visibility",
                capture(visibilityValueSlot)
            )
        }
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            visibilityValueSlot.captured.contents.toString().toLowerCase()
        )
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
    }

    @Test
    fun render_UpdateManeuverArrowValue() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
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
        val visibilityValueSlot = slot<Value>()
        val arrowHeadSourceSlot = slot<Value>()
        val arrowShaftSourceSlot = slot<Value>()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }

        MapboxRouteArrowView(options).renderManeuverUpdate(
            style,
            com.mapbox.navigation.ui.base.model.Expected.Success(state)
        )

        verify {
            style.setStyleLayerProperty(
                RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                "visibility",
                capture(visibilityValueSlot)
            )
        }
        assertEquals(
            Visibility.NONE.value.toLowerCase(),
            visibilityValueSlot.captured.contents.toString().toLowerCase()
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_HEAD_SOURCE_ID,
                any(),
                capture(arrowHeadSourceSlot)
            )
        }
        assertEquals(
            arrowHeadFeature.toJson(),
            arrowHeadSourceSlot.captured.contents.toString()
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                any(),
                capture(arrowShaftSourceSlot)
            )
        }
        assertEquals(
            arrowShaftFeature.toJson(),
            arrowShaftSourceSlot.captured.contents.toString()
        )
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
    }

    @Test
    fun render_AddArrowState_initializesLayers() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val state = ArrowAddedValue(
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf())
        )

        MapboxRouteArrowView(options).render(style, state)

        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
    }

    @Test
    fun render_AddArrowState() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val arrowHeadSourceSlot = slot<Value>()
        val arrowShaftSourceSlot = slot<Value>()
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

        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_HEAD_SOURCE_ID,
                any(),
                capture(arrowHeadSourceSlot)
            )
        }
        assertEquals(
            arrowHeadFeatureCollection.toJson(),
            arrowHeadSourceSlot.captured.contents.toString()
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                any(),
                capture(arrowShaftSourceSlot)
            )
        }
        assertEquals(
            arrowShaftFeatureCollection.toJson(),
            arrowShaftSourceSlot.captured.contents.toString()
        )
        unmockkObject(RouteArrowUtils)
    }

    @Test
    fun render_ExpectedAddArrowState() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val arrowHeadSourceSlot = slot<Value>()
        val arrowShaftSourceSlot = slot<Value>()
        val featureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\"," +
            "\"coordinates\":[[-122.5234885,37.9754331]]}}"
        val arrowShaftFeature = Feature.fromJson(featureJson)
        val arrowHeadFeature = Feature.fromJson(featureJson)
        val arrowShaftFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowShaftFeature))
        val arrowHeadFeatureCollection = FeatureCollection.fromFeatures(listOf(arrowHeadFeature))
        val state = com.mapbox.navigation.ui.base.model.Expected.Success(
            ArrowAddedValue(
                arrowShaftFeatureCollection,
                arrowHeadFeatureCollection
            )
        )

        MapboxRouteArrowView(options).render(style, state)

        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_HEAD_SOURCE_ID,
                any(),
                capture(arrowHeadSourceSlot)
            )
        }
        assertEquals(
            arrowHeadFeatureCollection.toJson(),
            arrowHeadSourceSlot.captured.contents.toString()
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                any(),
                capture(arrowShaftSourceSlot)
            )
        }
        assertEquals(
            arrowShaftFeatureCollection.toJson(),
            arrowShaftSourceSlot.captured.contents.toString()
        )
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
    }

    @Test
    fun render_RemoveArrowState() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val arrowHeadSourceSlot = slot<Value>()
        val arrowShaftSourceSlot = slot<Value>()
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

        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_HEAD_SOURCE_ID,
                any(),
                capture(arrowHeadSourceSlot)
            )
        }
        assertEquals(
            arrowHeadFeatureCollection.toJson(),
            arrowHeadSourceSlot.captured.contents.toString()
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                any(),
                capture(arrowShaftSourceSlot)
            )
        }
        assertEquals(
            arrowShaftFeatureCollection.toJson(),
            arrowShaftSourceSlot.captured.contents.toString()
        )
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
    }

    @Test
    fun render_ClearArrowsState() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isFullyLoaded() } returns true
            every { fullyLoaded } returns true
            every {
                getStyleLayerProperties(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns layerPropertyExpected
            every {
                setStyleSourceProperty(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createNone()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createNone()
        }.also {
            mockCheckForLayerInitialization(it)
        }
        val arrowHeadSourceSlot = slot<Value>()
        val arrowShaftSourceSlot = slot<Value>()
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

        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_HEAD_SOURCE_ID,
                any(),
                capture(arrowHeadSourceSlot)
            )
        }
        assertEquals(
            arrowHeadFeatureCollection.toJson(),
            arrowHeadSourceSlot.captured.contents.toString()
        )
        verify {
            style.setStyleSourceProperty(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                any(),
                capture(arrowShaftSourceSlot)
            )
        }
        assertEquals(
            arrowShaftFeatureCollection.toJson(),
            arrowShaftSourceSlot.captured.contents.toString()
        )
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
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
