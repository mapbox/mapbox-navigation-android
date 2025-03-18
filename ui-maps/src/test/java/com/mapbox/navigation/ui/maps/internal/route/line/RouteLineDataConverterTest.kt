package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.line.api.LineGradientCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.LineTrimCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineExpressionCommandHolder
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
        val trailExp = mockk<Expression>()
        val trailCasingExp = mockk<Expression>()
        val restrictedExp = mockk<Expression>()
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
                "line-gradient",
                restrictedExp,
            ),
            RouteLineTrimOffset(0.2),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                trailExp,
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
        checkTrimOffset(actual.dynamicData.trafficExpressionCommandHolder!!, trafficExp)
        checkTrimOffset(actual.dynamicData.trailExpressionCommandHolder!!, trailExp)
        checkGradient(actual.dynamicData.trailCasingExpressionCommandHolder!!, trailCasingExp)
        checkGradient(actual.dynamicData.restrictedSectionExpressionCommandHolder!!, restrictedExp)
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
        val trailExp = mockk<Expression>()
        val trailCasingExp = mockk<Expression>()
        val restrictedExp = mockk<Expression>()
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
                "line-gradient",
                restrictedExp,
            ),
            RouteLineTrimOffset(0.2),
            RouteLineProviderBasedExpressionEventData(
                "line-trim-offset",
                trailExp,
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
        checkTrimOffset(actual.trafficExpressionCommandHolder!!, trafficExp)
        checkTrimOffset(actual.trailExpressionCommandHolder!!, trailExp)
        checkGradient(actual.trailCasingExpressionCommandHolder!!, trailCasingExp)
        checkGradient(actual.restrictedSectionExpressionCommandHolder!!, restrictedExp)
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

        val actual = input.toInput { toEventValue(mockk()) }

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

        val actual = input.toInput { toEventValue(mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteSetValueFilled() = runBlocking {
        val exp1 = mockk<Expression>(relaxed = true)
        val exp2 = mockk<Expression>(relaxed = true)
        val exp3 = mockk<Expression>(relaxed = true)
        val exp4 = mockk<Expression>(relaxed = true)
        val exp5 = mockk<Expression>(relaxed = true)
        val exp6 = mockk<Expression>(relaxed = true)
        val exp7 = mockk<Expression>(relaxed = true)
        val exp8 = mockk<Expression>(relaxed = true)
        val exp9 = mockk<Expression>(relaxed = true)
        val exp10 = mockk<Expression>(relaxed = true)
        val exp11 = mockk<Expression>(relaxed = true)
        val exp12 = mockk<Expression>(relaxed = true)
        val exp13 = mockk<Expression>(relaxed = true)
        val exp14 = mockk<Expression>(relaxed = true)
        val exp15 = mockk<Expression>(relaxed = true)
        val exp16 = mockk<Expression>(relaxed = true)
        val exp17 = mockk<Expression>(relaxed = true)
        val exp18 = mockk<Expression>(relaxed = true)
        val exp19 = mockk<Expression>(relaxed = true)
        val primaryFc = mockk<FeatureCollection>()
        val alt1Fc = mockk<FeatureCollection>()
        val waypointSource = mockk<FeatureCollection>()
        val primaryDynamicData = RouteLineDynamicData(
            RouteLineExpressionCommandHolder(
                { exp1 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp2 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp3 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp4 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.2),
            RouteLineExpressionCommandHolder(
                { exp5 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp6 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp19 },
                LineGradientCommandApplier(),
            ),
        )
        val alt1DynamicData = RouteLineDynamicData(
            RouteLineExpressionCommandHolder(
                { exp7 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp8 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp9 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp10 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.3),
            RouteLineExpressionCommandHolder(
                { exp11 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp12 },
                LineGradientCommandApplier(),
            ),
        )
        val maskingDynamicData = RouteLineDynamicData(
            RouteLineExpressionCommandHolder(
                { exp13 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp14 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp15 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp16 },
                LineGradientCommandApplier(),
            ),
            RouteLineTrimOffset(0.1),
            RouteLineExpressionCommandHolder(
                { exp17 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp18 },
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
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp1),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp2),
                    RouteLineProviderBasedExpressionEventData("line-trim-offset", exp3),
                    RouteLineProviderBasedExpressionEventData("line-trim-offset", exp4),
                    RouteLineTrimOffset(0.2),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp5),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp6),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp19),
                ),
            ),
            listOf(
                RouteLineEventData(
                    alt1Fc,
                    RouteLineDynamicEventData(
                        RouteLineProviderBasedExpressionEventData("line-trim-offset", exp7),
                        RouteLineProviderBasedExpressionEventData("line-gradient", exp8),
                        RouteLineProviderBasedExpressionEventData("line-gradient", exp9),
                        RouteLineProviderBasedExpressionEventData("line-trim-offset", exp10),
                        RouteLineTrimOffset(0.3),
                        RouteLineProviderBasedExpressionEventData("line-gradient", exp11),
                        RouteLineProviderBasedExpressionEventData("line-gradient", exp12),
                        null,
                    ),
                ),
            ),
            waypointSource,
            RouteLineDynamicEventData(
                RouteLineProviderBasedExpressionEventData("line-gradient", exp13),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp14),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp15),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp16),
                RouteLineTrimOffset(0.1),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp17),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp18),
                null,
            ),
        )

        val actual = input.toInput { toEventValue(mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteLineUpdateValueEmpty() = runBlocking {
        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(
            RouteLineUpdateValue(null, emptyList(), null),
        )
        val expected = RouteLineViewRenderRouteLineUpdateDataValue(null, emptyList(), null)

        val actual = input.toInput { toEventValue(mockk()) }

        assertEquals(expected, actual)
    }

    @Test
    fun toInputRouteLineUpdateValueFilled() = runBlocking {
        val exp1 = mockk<Expression>(relaxed = true)
        val exp2 = mockk<Expression>(relaxed = true)
        val exp3 = mockk<Expression>(relaxed = true)
        val exp4 = mockk<Expression>(relaxed = true)
        val exp5 = mockk<Expression>(relaxed = true)
        val exp6 = mockk<Expression>(relaxed = true)
        val exp7 = mockk<Expression>(relaxed = true)
        val exp8 = mockk<Expression>(relaxed = true)
        val exp9 = mockk<Expression>(relaxed = true)
        val exp10 = mockk<Expression>(relaxed = true)
        val exp11 = mockk<Expression>(relaxed = true)
        val exp12 = mockk<Expression>(relaxed = true)
        val exp13 = mockk<Expression>(relaxed = true)
        val exp14 = mockk<Expression>(relaxed = true)
        val exp15 = mockk<Expression>(relaxed = true)
        val exp16 = mockk<Expression>(relaxed = true)
        val exp17 = mockk<Expression>(relaxed = true)
        val exp18 = mockk<Expression>(relaxed = true)
        val primaryDynamicData = RouteLineDynamicData(
            RouteLineExpressionCommandHolder(
                { exp1 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp2 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp3 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp4 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.2),
            RouteLineExpressionCommandHolder(
                { exp5 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp6 },
                LineGradientCommandApplier(),
            ),
        )
        val alt1DynamicData = RouteLineDynamicData(
            RouteLineExpressionCommandHolder(
                { exp7 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp8 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp9 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp10 },
                LineTrimCommandApplier(),
            ),
            RouteLineTrimOffset(0.3),
            RouteLineExpressionCommandHolder(
                { exp11 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp12 },
                LineGradientCommandApplier(),
            ),
        )
        val maskingDynamicData = RouteLineDynamicData(
            RouteLineExpressionCommandHolder(
                { exp13 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp14 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp15 },
                LineTrimCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp16 },
                LineGradientCommandApplier(),
            ),
            RouteLineTrimOffset(0.1),
            RouteLineExpressionCommandHolder(
                { exp17 },
                LineGradientCommandApplier(),
            ),
            RouteLineExpressionCommandHolder(
                { exp18 },
                LineTrimCommandApplier(),
            ),
        )

        val input = ExpectedFactory.createValue<RouteLineError, RouteLineUpdateValue>(
            RouteLineUpdateValue(primaryDynamicData, listOf(alt1DynamicData), maskingDynamicData),
        )
        val expected = RouteLineViewRenderRouteLineUpdateDataValue(
            RouteLineDynamicEventData(
                RouteLineProviderBasedExpressionEventData("line-gradient", exp1),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp2),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp3),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp4),
                RouteLineTrimOffset(0.2),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp5),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp6),
                null,
            ),
            listOf(
                RouteLineDynamicEventData(
                    RouteLineProviderBasedExpressionEventData("line-trim-offset", exp7),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp8),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp9),
                    RouteLineProviderBasedExpressionEventData("line-trim-offset", exp10),
                    RouteLineTrimOffset(0.3),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp11),
                    RouteLineProviderBasedExpressionEventData("line-gradient", exp12),
                    null,
                ),
            ),
            RouteLineDynamicEventData(
                RouteLineProviderBasedExpressionEventData("line-gradient", exp13),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp14),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp15),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp16),
                RouteLineTrimOffset(0.1),
                RouteLineProviderBasedExpressionEventData("line-gradient", exp17),
                RouteLineProviderBasedExpressionEventData("line-trim-offset", exp18),
                null,
            ),
        )

        val actual = input.toInput { toEventValue(mockk()) }

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

    private fun checkNoOp(holder: RouteLineExpressionCommandHolder) {
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                holder.provider.generateCommand(eventData)
            }
        }
        holder.applier.applyCommand(style, layerId, mockk())
        verify(exactly = 0) {
            style.setStyleLayerProperty(layerId, any(), any())
        }
    }

    private suspend fun checkGradient(holder: RouteLineExpressionCommandHolder, exp: Expression) {
        holder.applier.applyCommand(style, layerId, holder.provider.generateCommand(eventData))
        verify(exactly = 1) {
            style.setStyleLayerProperty(layerId, "line-gradient", exp)
        }
    }

    private suspend fun checkTrimOffset(holder: RouteLineExpressionCommandHolder, exp: Expression) {
        holder.applier.applyCommand(style, layerId, holder.provider.generateCommand(eventData))
        verify(exactly = 1) {
            style.setStyleLayerProperty(layerId, "line-trim-offset", exp)
        }
    }
}
