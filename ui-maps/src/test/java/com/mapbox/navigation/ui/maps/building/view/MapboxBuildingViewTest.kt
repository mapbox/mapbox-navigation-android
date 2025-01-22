package com.mapbox.navigation.ui.maps.building.view

import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MapboxBuildingViewTest(private val is3dStyle: Boolean) {

    private val view2D = mockk<Mapbox2DBuildingView>(relaxed = true)
    private val view3D = mockk<Mapbox3DBuildingView>(relaxed = true)

    private val buildingView = MapboxBuildingView(view2D, view3D)

    @Test
    fun `check dispatches calls to correct implementation`() {
        val (activeView, inactiveView) = if (is3dStyle) {
            view3D to view2D
        } else {
            view2D to view3D
        }

        val style = mockk<Style> {
            every { styleLayerExists("building-extrusion") } returns is3dStyle
        }

        val features = mockk<List<QueriedRenderedFeature>>(relaxed = true)
        val options = mockk<MapboxBuildingHighlightOptions>(relaxed = true)

        buildingView.highlightBuilding(style, features, options)
        buildingView.removeBuildingHighlight(style, options)
        buildingView.clear(style)

        verify(exactly = 1) {
            activeView.highlightBuilding(style, features, options)
        }

        verify(exactly = 1) {
            activeView.removeBuildingHighlight(style, options)
        }

        verify(exactly = 1) {
            activeView.clear(style)
        }

        verifyOrder {
            activeView.highlightBuilding(style, features, options)
            activeView.removeBuildingHighlight(style, options)
            activeView.clear(style)
        }

        verify {
            inactiveView wasNot Called
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = listOf(true, false)
    }
}
