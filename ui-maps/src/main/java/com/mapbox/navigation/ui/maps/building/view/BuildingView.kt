package com.mapbox.navigation.ui.maps.building.view

import androidx.annotation.UiThread
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions

@UiThread
internal interface BuildingView {

    fun highlightBuilding(
        style: Style,
        buildings: List<QueriedRenderedFeature>,
        options: MapboxBuildingHighlightOptions,
    )

    fun removeBuildingHighlight(style: Style, options: MapboxBuildingHighlightOptions)

    fun clear(style: Style)
}
