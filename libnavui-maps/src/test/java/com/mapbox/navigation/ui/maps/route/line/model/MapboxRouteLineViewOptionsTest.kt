package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.model.FadingConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass

class MapboxRouteLineViewOptionsTest :
    BuilderTest<MapboxRouteLineViewOptions, MapboxRouteLineViewOptions.Builder>() {

    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
    }

    @Test(expected = IllegalArgumentException::class)
    fun lineDepthOcclusionFactorTooSmall() {
        MapboxRouteLineViewOptions.Builder(ctx).lineDepthOcclusionFactor(-0.1).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun lineDepthOcclusionFactorTooBig() {
        MapboxRouteLineViewOptions.Builder(ctx).lineDepthOcclusionFactor(1.1).build()
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test(expected = IllegalArgumentException::class)
    fun fadeOnHighZoomsConfigStartIsGreaterThanFinish() {
        MapboxRouteLineViewOptions.Builder(ctx)
            .fadeOnHighZoomsConfig(
                FadingConfig.Builder(16.1, 16.0).build(),
            )
            .build()
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun fadeOnHighZoomsConfigStartIsEqualToFinish() {
        MapboxRouteLineViewOptions.Builder(ctx)
            .fadeOnHighZoomsConfig(
                FadingConfig.Builder(16.0, 16.0).build(),
            )
            .build()
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun fadeOnHighZoomsConfigStartIsLessThanFinish() {
        MapboxRouteLineViewOptions.Builder(ctx)
            .fadeOnHighZoomsConfig(
                FadingConfig.Builder(16.0, 16.1).build(),
            )
            .build()
    }

    override fun getImplementationClass(): KClass<MapboxRouteLineViewOptions> =
        MapboxRouteLineViewOptions::class

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun getFilledUpBuilder(): MapboxRouteLineViewOptions.Builder {
        val routeLineColorResources = RouteLineColorResources.Builder()
            .routeLineTraveledColor(Color.BLACK)
            .build()
        return MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(routeLineColorResources)
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
            .scaleExpressions(
                RouteLineScaleExpressions.Builder()
                    .routeLineScaleExpression(
                        MapboxRouteLineUtils.buildScalingExpression(
                            listOf(
                                RouteLineScaleValue(4f, 3f, 1.5f),
                                RouteLineScaleValue(10f, 4f, 1.5f),
                                RouteLineScaleValue(13f, 6f, 1.5f),
                                RouteLineScaleValue(16f, 10f, 1.5f),
                                RouteLineScaleValue(19f, 14f, 1.5f),
                                RouteLineScaleValue(22f, 18f, 1.5f),
                            ),
                        ),
                    )
                    .build(),
            )
            .slotName("foobar")
            .fadeOnHighZoomsConfig(FadingConfig.Builder(16.0, 16.1).build())
    }

    override fun trigger() {
    }
}
