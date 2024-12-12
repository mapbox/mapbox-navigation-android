package com.mapbox.navigation.ui.maps.util

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.None
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.StyleManager as MapsStyleManager

internal class StyleManager(
    private val originalStyleManager: MapsStyleManager,
) {

    fun removeStyleLayer(layerId: String): Expected<String, None> {
        return originalStyleManager.removeStyleLayer(layerId)
    }

    fun removeStyleImage(imageId: String): Expected<String, None> {
        return originalStyleManager.removeStyleImage(imageId)
    }

    fun removeStyleSource(sourceId: String): Expected<String, None> {
        return originalStyleManager.removeStyleSource(sourceId)
    }
}

internal val Style.sdkStyleManager: StyleManager
    get() = StyleManager(this.styleManager)

internal val MapboxMap.sdkStyleManager: StyleManager
    get() = StyleManager(this.styleManager)
