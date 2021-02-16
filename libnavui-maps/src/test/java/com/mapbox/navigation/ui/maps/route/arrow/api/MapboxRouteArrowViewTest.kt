package com.mapbox.navigation.ui.maps.route.arrow.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.maps.Image
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.common.ShadowValueConverter
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowValueConverter::class])
@RunWith(RobolectricTestRunner::class)
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
    private val layerPropertyExpected = mockk<Expected<Value, String>> {
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
    private val geoJsonSourceExpected = mockk<Expected<Value, String>> {
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
        val state = RouteArrowState.UpdateRouteArrowVisibilityState(
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
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()
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
    fun render_UpdateManeuverArrowState() {
        mockkObject(RouteArrowUtils)
        val options = RouteArrowOptions.Builder(ctx).build()
        val arrowShaftFeature = mockk<Feature> {
            every { toJson() } returns "{}"
        }
        val arrowHeadFeature = mockk<Feature> {
            every { toJson() } returns "{}"
        }
        val state = RouteArrowState.UpdateManeuverArrowState(
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
            } returns ExpectedFactory.createValue()
            every {
                setStyleLayerProperty(
                    RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
                    "visibility",
                    any()
                )
            } returns ExpectedFactory.createValue()

            every {
                getStyleSourceProperties(RouteConstants.ARROW_HEAD_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                getStyleSourceProperties(RouteConstants.ARROW_SHAFT_SOURCE_ID)
            } returns geoJsonSourceExpected
            every {
                setStyleSourceProperty(RouteConstants.ARROW_HEAD_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
            every {
                setStyleSourceProperty(RouteConstants.ARROW_SHAFT_SOURCE_ID, any(), any())
            } returns ExpectedFactory.createValue()
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
            arrowHeadFeature.toJson(),
            arrowShaftSourceSlot.captured.contents.toString()
        )
        verify { RouteArrowUtils.initializeLayers(style, options) }
        unmockkObject(RouteArrowUtils)
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
