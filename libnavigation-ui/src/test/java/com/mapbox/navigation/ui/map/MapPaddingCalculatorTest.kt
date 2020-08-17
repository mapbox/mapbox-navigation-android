package com.mapbox.navigation.ui.map

import android.content.Context
import android.content.res.Resources
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.navigation.ui.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapPaddingCalculatorTest {

    @Test
    fun calculateDefaultPaddingPortrait() {
        val resource: Resources = mockk {
            every { getDimension(R.dimen.mapbox_summary_bottom_sheet_height) } returns 72f
            every { getDimension(R.dimen.mapbox_wayname_view_height) } returns 32f
        }
        val ctx: Context = mockk {
            every { resources } returns resource
        }
        val mapView: MapView = mockk {
            every { height } returns 1024
            every { width } returns 768
            every { context } returns ctx
        }

        val result = MapPaddingCalculator.calculateDefaultPadding(mapView)

        assertEquals(4, result.size)
        assertEquals(0, result[0])
        assertEquals(672, result[1])
        assertEquals(0, result[2])
        assertEquals(0, result[3])
    }

    @Test
    fun calculateDefaultPaddingLandscape() {
        val resource: Resources = mockk {
            every { getDimension(R.dimen.mapbox_summary_bottom_sheet_height) } returns 56f
            every { getDimension(R.dimen.mapbox_wayname_view_height) } returns 24f
        }
        val ctx: Context = mockk {
            every { resources } returns resource
        }
        val mapView: MapView = mockk {
            every { height } returns 1024
            every { width } returns 768
            every { context } returns ctx
        }

        val result = MapPaddingCalculator.calculateDefaultPadding(mapView)

        assertEquals(4, result.size)
        assertEquals(0, result[0])
        assertEquals(752, result[1])
        assertEquals(0, result[2])
        assertEquals(0, result[3])
    }

    @Test
    fun calculateTopPaddingWithoutWaynamePortrait() {
        val resource: Resources = mockk {
            every { getDimension(R.dimen.mapbox_summary_bottom_sheet_height) } returns 72f
        }
        val ctx: Context = mockk {
            every { resources } returns resource
        }
        val mapView: MapView = mockk {
            every { height } returns 1024
            every { width } returns 768
            every { context } returns ctx
        }

        val result = MapPaddingCalculator.calculateTopPaddingWithoutWayname(mapView)

        assertEquals(736, result)
    }

    @Test
    fun calculateTopPaddingWithoutWaynameLandscape() {
        val resource: Resources = mockk {
            every { getDimension(R.dimen.mapbox_summary_bottom_sheet_height) } returns 56f
        }
        val ctx: Context = mockk {
            every { resources } returns resource
        }
        val mapView: MapView = mockk {
            every { height } returns 768
            every { width } returns 1024
            every { context } returns ctx
        }

        val result = MapPaddingCalculator.calculateTopPaddingWithoutWayname(mapView)

        assertEquals(600, result)
    }
}
