package com.mapbox.navigation.ui.maps.route.line.model

import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class SegmentColorTypeTest(
    private val type: SegmentColorType,
    private val options: RouteLineViewOptionsData,
    private val expected: Int,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<Array<Any>> {
            mockkStatic("androidx.appcompat.content.res.AppCompatResources")
            every { AppCompatResources.getDrawable(any(), any()) } returns mockk()
            val options = MapboxRouteLineViewOptions.Builder(mockk(relaxed = true))
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .routeDefaultColor(1)
                        .routeCasingColor(2)
                        .routeUnknownCongestionColor(3)
                        .routeLowCongestionColor(4)
                        .routeModerateCongestionColor(5)
                        .routeHeavyCongestionColor(6)
                        .routeSevereCongestionColor(7)
                        .alternativeRouteDefaultColor(8)
                        .alternativeRouteUnknownCongestionColor(9)
                        .alternativeRouteLowCongestionColor(10)
                        .alternativeRouteModerateCongestionColor(11)
                        .alternativeRouteHeavyCongestionColor(12)
                        .alternativeRouteSevereCongestionColor(13)
                        .inActiveRouteLegsColor(14)
                        .inactiveRouteLegUnknownCongestionColor(15)
                        .inactiveRouteLegLowCongestionColor(16)
                        .inactiveRouteLegModerateCongestionColor(17)
                        .inactiveRouteLegHeavyCongestionColor(18)
                        .inactiveRouteLegSevereCongestionColor(19)
                        .routeLineTraveledColor(20)
                        .routeLineTraveledCasingColor(21)
                        .routeClosureColor(22)
                        .restrictedRoadColor(23)
                        .alternativeRouteClosureColor(24)
                        .alternativeRouteRestrictedRoadColor(25)
                        .inactiveRouteLegClosureColor(26)
                        .inactiveRouteLegRestrictedRoadColor(27)
                        .inactiveRouteLegCasingColor(28)
                        .alternativeRouteCasingColor(29)
                        .build(),
                )
                .build()
                .toData()
            unmockkStatic("androidx.appcompat.content.res.AppCompatResources")
            val result = listOf(
                arrayOf(SegmentColorType.PRIMARY_DEFAULT, options, 1),
                arrayOf(SegmentColorType.PRIMARY_LOW_CONGESTION, options, 4),
                arrayOf(SegmentColorType.PRIMARY_MODERATE_CONGESTION, options, 5),
                arrayOf(SegmentColorType.PRIMARY_HEAVY_CONGESTION, options, 6),
                arrayOf(SegmentColorType.PRIMARY_SEVERE_CONGESTION, options, 7),
                arrayOf(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, options, 3),
                arrayOf(SegmentColorType.PRIMARY_CASING, options, 2),
                arrayOf(SegmentColorType.PRIMARY_CLOSURE, options, 22),
                arrayOf(SegmentColorType.PRIMARY_RESTRICTED, options, 23),
                arrayOf(SegmentColorType.TRAVELED, options, 20),
                arrayOf(SegmentColorType.TRAVELED_CASING, options, 21),
                arrayOf(SegmentColorType.INACTIVE_DEFAULT, options, 14),
                arrayOf(SegmentColorType.INACTIVE_LOW_CONGESTION, options, 16),
                arrayOf(SegmentColorType.INACTIVE_MODERATE_CONGESTION, options, 17),
                arrayOf(SegmentColorType.INACTIVE_HEAVY_CONGESTION, options, 18),
                arrayOf(SegmentColorType.INACTIVE_SEVERE_CONGESTION, options, 19),
                arrayOf(SegmentColorType.INACTIVE_UNKNOWN_CONGESTION, options, 15),
                arrayOf(SegmentColorType.INACTIVE_CASING, options, 28),
                arrayOf(SegmentColorType.INACTIVE_CLOSURE, options, 26),
                arrayOf(SegmentColorType.INACTIVE_RESTRICTED, options, 27),
                arrayOf(SegmentColorType.ALTERNATIVE_DEFAULT, options, 8),
                arrayOf(SegmentColorType.ALTERNATIVE_LOW_CONGESTION, options, 10),
                arrayOf(SegmentColorType.ALTERNATIVE_MODERATE_CONGESTION, options, 11),
                arrayOf(SegmentColorType.ALTERNATIVE_HEAVY_CONGESTION, options, 12),
                arrayOf(SegmentColorType.ALTERNATIVE_SEVERE_CONGESTION, options, 13),
                arrayOf(SegmentColorType.ALTERNATIVE_UNKNOWN_CONGESTION, options, 9),
                arrayOf(SegmentColorType.ALTERNATIVE_CASING, options, 29),
                arrayOf(SegmentColorType.ALTERNATIVE_CLOSURE, options, 24),
                arrayOf(SegmentColorType.ALTERNATIVE_RESTRICTED, options, 25),
                arrayOf(SegmentColorType.TRANSPARENT, options, Color.TRANSPARENT),
            )
            assertEquals(SegmentColorType.values().size, result.size)
            return result
        }
    }

    @Test
    fun getColor() {
        assertEquals(expected, type.getColor(options))
    }
}
