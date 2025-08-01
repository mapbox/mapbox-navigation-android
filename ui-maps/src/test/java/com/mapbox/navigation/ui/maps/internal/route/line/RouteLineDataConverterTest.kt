package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.line.api.LineGradientCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.LineTrimCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineValueCommandHolder
import com.mapbox.navigation.ui.maps.route.line.api.unsupportedRouteLineCommandHolder
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleExpressions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.model.FadingConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteLineDataConverterTest {

    private val style = mockk<Style>(relaxed = true)
    private val layerId = "some-id"
    private val eventData = mockk<RouteLineViewOptionsData>(relaxed = true)

    @Test
    fun toRouteLineDataEmpty() = runBlocking {
        val fc = mockk<FeatureCollection>()
        val dynamicData = RouteLineDynamicEventData(
            RouteLineNoOpExpressionEventData(),
            RouteLineNoOpExpressionEventData(),
            null,
            null,
            null,
            null,
            null,
            null,
        )
        val input = RouteLineEventData(fc, dynamicData)

        val actual = input.toRouteLineData()

        assertEquals(fc, actual.featureCollection)
        checkNoOp(actual.dynamicData!!.baseExpressionCommandHolder)
        checkNoOp(actual.dynamicData.casingExpressionCommandHolder)
        assertNull(actual.dynamicData.trimOffset)
        assertNull(actual.dynamicData.trafficExpressionCommandHolder)
        assertNull(actual.dynamicData.trailExpressionCommandHolder)
        assertNull(actual.dynamicData.trailCasingExpressionCommandHolder)
        assertNull(actual.dynamicData.restrictedSectionExpressionCommandHolder)
    }

    @Test
    fun toRouteLineDataFilled() = runBlocking {
        val fc = mockk<FeatureCollection>()
        val baseExp = mockk<Expression>()
        val trafficExp = mockk<Expression>()
        val casingExp = mockk<Expression>()
        val trailValue = mockk<Value>()
        val trailCasingExp = mockk<Expression>()
        val restrictedValue = mockk<Value>()
        val blurExp = mockk<Expression>()
        val dynamicData = RouteLineDynamicEventData(
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                baseExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                casingExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                trafficExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-start",
                value = StylePropertyValue(restrictedValue, StylePropertyValueKind.CONSTANT),
            ),
            RouteLineTrimOffset(0.2),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-end",
                value = StylePropertyValue(trailValue, StylePropertyValueKind.CONSTANT),
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                trailCasingExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                blurExp,
            ),
        )
        val input = RouteLineEventData(fc, dynamicData)

        val actual = input.toRouteLineData()

        assertEquals(fc, actual.featureCollection)
        checkGradient(actual.dynamicData!!.baseExpressionCommandHolder, baseExp)
        checkGradient(actual.dynamicData.casingExpressionCommandHolder, casingExp)
        assertEquals(RouteLineTrimOffset(0.2), actual.dynamicData.trimOffset!!)
        checkTrim(
            actual.dynamicData.trafficExpressionCommandHolder!!,
            trafficExp,
            "line-trim-offset",
        )
        checkTrim(actual.dynamicData.trailExpressionCommandHolder!!, trailValue, "line-trim-end")
        checkGradient(actual.dynamicData.trailCasingExpressionCommandHolder!!, trailCasingExp)
        checkTrim(
            actual.dynamicData.restrictedSectionExpressionCommandHolder!!,
            restrictedValue,
            "line-trim-start",
        )
    }

    @Test
    fun toRouteLineDynamicDataEmpty() {
        val dynamicData = RouteLineDynamicEventData(
            RouteLineNoOpExpressionEventData(),
            RouteLineNoOpExpressionEventData(),
            null,
            null,
            null,
            null,
            null,
            null,
        )
        val actual = dynamicData.toRouteLineDynamicData()

        checkNoOp(actual.baseExpressionCommandHolder)
        checkNoOp(actual.casingExpressionCommandHolder)
        assertNull(actual.trimOffset)
        assertNull(actual.trafficExpressionCommandHolder)
        assertNull(actual.trailExpressionCommandHolder)
        assertNull(actual.trailCasingExpressionCommandHolder)
        assertNull(actual.restrictedSectionExpressionCommandHolder)
    }

    @Test
    fun toRouteLineDynamicDataFilled() = runBlocking {
        val baseExp = mockk<Expression>()
        val trafficExp = mockk<Expression>()
        val casingExp = mockk<Expression>()
        val trailValue = mockk<Value>()
        val trailCasingExp = mockk<Expression>()
        val restrictedValue = mockk<Value>()
        val blurExp = mockk<Expression>()
        val dynamicData = RouteLineDynamicEventData(
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                baseExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                casingExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                trafficExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-start",
                value = StylePropertyValue(restrictedValue, StylePropertyValueKind.CONSTANT),
            ),
            RouteLineTrimOffset(0.2),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-end",
                value = StylePropertyValue(trailValue, StylePropertyValueKind.CONSTANT),
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                trailCasingExp,
            ),
            RouteLineProviderBasedExpressionEventData(
                "line-gradient",
                blurExp,
            ),
        )
        val actual = dynamicData.toRouteLineDynamicData()

        checkGradient(actual.baseExpressionCommandHolder, baseExp)
        checkGradient(actual.casingExpressionCommandHolder, casingExp)
        assertEquals(RouteLineTrimOffset(0.2), actual.trimOffset!!)
        checkTrim(actual.trafficExpressionCommandHolder!!, trafficExp, "line-trim-offset")
        checkTrim(actual.trailExpressionCommandHolder!!, trailValue, "line-trim-end")
        checkGradient(actual.trailCasingExpressionCommandHolder!!, trailCasingExp)
        checkTrim(
            actual.restrictedSectionExpressionCommandHolder!!,
            restrictedValue,
            "line-trim-start",
        )
    }

    @Test
    fun toRouteLineDynamicDataUnknownProperty() {
        val dynamicData = RouteLineDynamicEventData(
            RouteLineProviderBasedExpressionEventData(
                "unknown",
                mockk(),
            ),
            RouteLineNoOpExpressionEventData(),
            null,
            null,
            null,
            null,
            null,
            null,
        )
        assertThrows(IllegalStateException::class.java) {
            dynamicData.toRouteLineDynamicData()
        }
    }

    @Test
    fun toInputError() = runBlocking {
        val input = ExpectedFactory.createError<RouteLineError, RouteSetValue>(
            RouteLineError(
                "some error",
                null,
            ),
        )
        val expected = RouteLineViewDataError("some error")

        val actual = input.toInput { toEventValue(coroutineContext, mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteSetValueEmpty() = runBlocking {
        val primaryFc = mockk<FeatureCollection>()
        val waypointSource = mockk<FeatureCollection>()
        val primaryDynamicData = RouteLineDynamicData(
            unsupportedRouteLineCommandHolder(),
            unsupportedRouteLineCommandHolder(),
            null,
            null,
            null,
            null,
            null,
        )
        val input = ExpectedFactory.createValue<RouteLineError, RouteSetValue>(
            RouteSetValue(
                RouteLineData(primaryFc, primaryDynamicData),
                emptyList(),
                waypointSource,
                RouteCalloutData(emptyList()),
                null,
            ),
        )
        val expected = RouteLineViewRenderRouteDrawDataInputValue(
            RouteLineEventData(
                primaryFc,
                RouteLineDynamicEventData(
                    RouteLineNoOpExpressionEventData(),
                    RouteLineNoOpExpressionEventData(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                ),
            ),
            emptyList(),
            waypointSource,
            null,
        )

        val actual = input.toInput { toEventValue(coroutineContext, mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteSetValueFilled() = runBlocking {
        val exp1 = mockk<StylePropertyValue>(relaxed = true)
        val exp2 = mockk<StylePropertyValue>(relaxed = true)
        val exp3 = mockk<StylePropertyValue>(relaxed = true)
        val exp4 = mockk<StylePropertyValue>(relaxed = true)
        val exp5 = mockk<StylePropertyValue>(relaxed = true)
        val exp6 = mockk<StylePropertyValue>(relaxed = true)
        val exp7 = mockk<StylePropertyValue>(relaxed = true)
        val exp8 = mockk<StylePropertyValue>(relaxed = true)
        val exp9 = mockk<StylePropertyValue>(relaxed = true)
        val exp10 = mockk<StylePropertyValue>(relaxed = true)
        val exp11 = mockk<StylePropertyValue>(relaxed = true)
        val exp12 = mockk<StylePropertyValue>(relaxed = true)
        val exp13 = mockk<StylePropertyValue>(relaxed = true)
        val exp14 = mockk<StylePropertyValue>(relaxed = true)
        val exp15 = mockk<StylePropertyValue>(relaxed = true)
        val exp16 = mockk<StylePropertyValue>(relaxed = true)
        val exp17 = mockk<StylePropertyValue>(relaxed = true)
        val exp18 = mockk<StylePropertyValue>(relaxed = true)
        val exp19 = mockk<StylePropertyValue>(relaxed = true)
        val primaryFc = mockk<FeatureCollection>()
        val alt1Fc = mockk<FeatureCollection>()
        val waypointSource = mockk<FeatureCollection>()
        val primaryDynamicData = RouteLineDynamicData(
            RouteLineValueCommandHolder(
                { _, _ -> exp1 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp2 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp3 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp4 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.2),
            RouteLineValueCommandHolder(
                { _, _ -> exp5 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp6 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp19 },
                LineGradientCommandApplier(),
            ),
        )
        val alt1DynamicData = RouteLineDynamicData(
            RouteLineValueCommandHolder(
                { _, _ -> exp7 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp8 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp9 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp10 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.3),
            RouteLineValueCommandHolder(
                { _, _ -> exp11 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp12 },
                LineGradientCommandApplier(),
            ),
        )
        val maskingDynamicData = RouteLineDynamicData(
            RouteLineValueCommandHolder(
                { _, _ -> exp13 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp14 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp15 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp16 },
                LineGradientCommandApplier(),
            ),
            RouteLineTrimOffset(0.1),
            RouteLineValueCommandHolder(
                { _, _ -> exp17 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp18 },
                LineTrimCommandApplier(),
            ),
        )

        val input = ExpectedFactory.createValue<RouteLineError, RouteSetValue>(
            RouteSetValue(
                RouteLineData(primaryFc, primaryDynamicData),
                listOf(
                    RouteLineData(alt1Fc, alt1DynamicData),
                ),
                waypointSource,
                RouteCalloutData(emptyList()),
                maskingDynamicData,
            ),
        )
        val expected = RouteLineViewRenderRouteDrawDataInputValue(
            RouteLineEventData(
                primaryFc,
                RouteLineDynamicEventData(
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp1),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp2),
                    RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp3),
                    RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp4),
                    RouteLineTrimOffset(0.2),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp5),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp6),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp19),
                ),
            ),
            listOf(
                RouteLineEventData(
                    alt1Fc,
                    RouteLineDynamicEventData(
                        RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp7),
                        RouteLineProviderBasedExpressionEventData("line-gradient", value = exp8),
                        RouteLineProviderBasedExpressionEventData("line-gradient", value = exp9),
                        RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp10),
                        RouteLineTrimOffset(0.3),
                        RouteLineProviderBasedExpressionEventData("line-gradient", value = exp11),
                        RouteLineProviderBasedExpressionEventData("line-gradient", value = exp12),
                        null,
                    ),
                ),
            ),
            waypointSource,
            RouteLineDynamicEventData(
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp13),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp14),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp15),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp16),
                RouteLineTrimOffset(0.1),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp17),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp18),
                null,
            ),
        )

        val actual = input.toInput { toEventValue(coroutineContext, mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteLineUpdateValueEmpty() = runBlocking {
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(
            RouteLineUpdateValue(null, emptyList(), null),
        )
        val expected = RouteLineViewRenderRouteLineUpdateDataValue(null, emptyList(), null)

        val actual = input.toInput { toEventValue(coroutineContext, mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteLineUpdateValueFilled() = runBlocking {
        val exp1 = mockk<StylePropertyValue>(relaxed = true)
        val exp2 = mockk<StylePropertyValue>(relaxed = true)
        val exp3 = mockk<StylePropertyValue>(relaxed = true)
        val exp4 = mockk<StylePropertyValue>(relaxed = true)
        val exp5 = mockk<StylePropertyValue>(relaxed = true)
        val exp6 = mockk<StylePropertyValue>(relaxed = true)
        val exp7 = mockk<StylePropertyValue>(relaxed = true)
        val exp8 = mockk<StylePropertyValue>(relaxed = true)
        val exp9 = mockk<StylePropertyValue>(relaxed = true)
        val exp10 = mockk<StylePropertyValue>(relaxed = true)
        val exp11 = mockk<StylePropertyValue>(relaxed = true)
        val exp12 = mockk<StylePropertyValue>(relaxed = true)
        val exp13 = mockk<StylePropertyValue>(relaxed = true)
        val exp14 = mockk<StylePropertyValue>(relaxed = true)
        val exp15 = mockk<StylePropertyValue>(relaxed = true)
        val exp16 = mockk<StylePropertyValue>(relaxed = true)
        val exp17 = mockk<StylePropertyValue>(relaxed = true)
        val exp18 = mockk<StylePropertyValue>(relaxed = true)
        val primaryDynamicData = RouteLineDynamicData(
            RouteLineValueCommandHolder(
                { _, _ -> exp1 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp2 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp3 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp4 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.2),
            RouteLineValueCommandHolder(
                { _, _ -> exp5 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp6 },
                LineGradientCommandApplier(),
            ),
        )
        val alt1DynamicData = RouteLineDynamicData(
            RouteLineValueCommandHolder(
                { _, _ -> exp7 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp8 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp9 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp10 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.3),
            RouteLineValueCommandHolder(
                { _, _ -> exp11 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp12 },
                LineGradientCommandApplier(),
            ),
        )
        val maskingDynamicData = RouteLineDynamicData(
            RouteLineValueCommandHolder(
                { _, _ -> exp13 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp14 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp15 },
                LineTrimCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp16 },
                LineGradientCommandApplier(),
            ),
            RouteLineTrimOffset(0.1),
            RouteLineValueCommandHolder(
                { _, _ -> exp17 },
                LineGradientCommandApplier(),
            ),
            RouteLineValueCommandHolder(
                { _, _ -> exp18 },
                LineTrimCommandApplier(),
            ),
        )

        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(
            RouteLineUpdateValue(primaryDynamicData, listOf(alt1DynamicData), maskingDynamicData),
        )
        val expected = RouteLineViewRenderRouteLineUpdateDataValue(
            RouteLineDynamicEventData(
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp1),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp2),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp3),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp4),
                RouteLineTrimOffset(0.2),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp5),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp6),
                null,
            ),
            listOf(
                RouteLineDynamicEventData(
                    RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp7),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp8),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp9),
                    RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp10),
                    RouteLineTrimOffset(0.3),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp11),
                    RouteLineProviderBasedExpressionEventData("line-gradient", value = exp12),
                    null,
                ),
            ),
            RouteLineDynamicEventData(
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp13),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp14),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp15),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp16),
                RouteLineTrimOffset(0.1),
                RouteLineProviderBasedExpressionEventData("line-gradient", value = exp17),
                RouteLineProviderBasedExpressionEventData("line-trim-start", value = exp18),
                null,
            ),
        )

        val actual = input.toInput { toEventValue(coroutineContext, mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteClearValue() = runBlocking {
        val primaryFc = mockk<FeatureCollection>()
        val altFc = mockk<FeatureCollection>()
        val waypointSource = mockk<FeatureCollection>()
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineClearValue>(
            RouteLineClearValue(
                primaryFc,
                listOf(altFc),
                waypointSource,
                RouteCalloutData(emptyList()),
            ),
        )
        val expected = RouteLineViewRenderRouteLineClearDataValue(
            primaryFc,
            listOf(altFc),
            waypointSource,
        )

        val actual = input.toInput { toEventValue() }

        assertEquals(expected, actual)
    }

    @Test
    fun viewOptionsToEventDataEmpty() {
        mockkStatic(AppCompatResources::class) {
            every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true)

            val ctx = mockk<Context>(relaxed = true)
            val colorResources = mockk<RouteLineColorResources>()
            val scaleExpressions = mockk<RouteLineScaleExpressions>()
            val fadingConfig = FadingConfig.Builder(15.0, 16.0).build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colorResources)
                .scaleExpressions(scaleExpressions)
                .routeLineBelowLayerId("someLayerId")
                .tolerance(.111)
                .displayRestrictedRoadSections(true)
                .displaySoftGradientForTraffic(true)
                .softGradientTransition(77.0)
                .waypointLayerIconOffset(listOf(3.0, 4.4))
                .waypointLayerIconAnchor(IconAnchor.BOTTOM)
                .iconPitchAlignment(IconPitchAlignment.AUTO)
                .shareLineGeometrySources(true)
                .lineDepthOcclusionFactor(0.85)
                .originWaypointIcon(123)
                .destinationWaypointIcon(456)
                .restrictedRoadDashArray(listOf(0.2, 0.8))
                .restrictedRoadLineWidth(1.2)
                .restrictedRoadOpacity(0.7)
                .routeLineBlurWidth(11.1)
                .routeLineBlurOpacity(.77)
                .routeLineBlurEnabled(true)
                .applyTrafficColorsToRouteLineBlur(true)
                .slotName("someSlotName")
                .fadeOnHighZoomsConfig(fadingConfig)
                .build()
            val expected = RouteLineViewOptionsData(
                colorResources,
                scaleExpressions,
                listOf(0.2, 0.8),
                0.7,
                1.2,
                true,
                77.0,
                123,
                456,
                listOf(3.0, 4.4),
                IconAnchor.BOTTOM,
                IconPitchAlignment.AUTO,
                true,
                "someLayerId",
                .111,
                true,
                0.85,
                "someSlotName",
                fadingConfig,
                11.1,
                true,
                true,
                .77,
            )

            val actual = viewOptions.toData()

            assertEquals(expected, actual)
        }
    }

    private fun checkNoOp(holder: RouteLineValueCommandHolder) {
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                holder.provider.generateCommand(coroutineContext, eventData)
            }
        }
        holder.applier.applyCommand(style, layerId, mockk())
        verify(exactly = 0) {
            style.setStyleLayerProperty(layerId, any(), any())
        }
    }

    private suspend fun checkGradient(holder: RouteLineValueCommandHolder, exp: Expression) {
        holder.applier.applyCommand(
            style,
            layerId,
            holder.provider.generateCommand(coroutineContext, eventData),
        )
        verify(exactly = 1) {
            style.setStyleLayerProperty(layerId, "line-gradient", exp)
        }
    }

    private suspend fun checkTrim(
        holder: RouteLineValueCommandHolder,
        exp: Value,
        property: String,
    ) {
        holder.applier.applyCommand(
            style,
            layerId,
            holder.provider.generateCommand(coroutineContext, eventData),
        )
        verify(exactly = 1) {
            style.setStyleLayerProperty(layerId, property, exp)
        }
    }
}
