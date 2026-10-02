package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener

class MapUserStyleObserver : MapboxCarMapObserver {
    var userId: String = ""
    var styleId: String = ""

    private val onStyleLoadedListener = OnStyleLoadedListener {
        updateState()
    }
    private var mapboxCarMapSurface: MapboxCarMapSurface? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onAttached(mapboxCarMapSurface)
        this.mapboxCarMapSurface = mapboxCarMapSurface
        updateState()
        mapboxCarMapSurface.mapSurface.getMapboxMap()
            .addOnStyleLoadedListener(onStyleLoadedListener)
    }

    private fun updateState() {
        mapboxCarMapSurface?.mapSurface?.getMapboxMap()?.getStyle()?.let { style ->
            val (parsedUserId, parsedStyleId) = parseStyleUri(style.styleURI)
            userId = parsedUserId
            styleId = parsedStyleId
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        mapboxCarMapSurface.mapSurface.getMapboxMap()
            .removeOnStyleLoadedListener(onStyleLoadedListener)
        userId = ""
        styleId = ""
        this.mapboxCarMapSurface = null
    }
}

private const val MAPBOX_STYLE_URI_PREFIX = "mapbox://styles/"

/**
 * Extracts the user and style ids from a `mapbox://styles/{user}/{style}` URI. Any other URI, such
 * as a style loaded from JSON or from an `https://` URL, and a Mapbox URI without both ids,
 * produce empty ids. Callers pass empty ids to the shield APIs as null, so only the legacy shields
 * are used for those styles.
 */
internal fun parseStyleUri(styleUri: String): Pair<String, String> {
    if (!styleUri.startsWith(MAPBOX_STYLE_URI_PREFIX)) return "" to ""
    val segments = styleUri.removePrefix(MAPBOX_STYLE_URI_PREFIX)
        .substringBefore('?')
        // limit = 2 keeps any further path, such as "/draft", in the style id.
        .split("/", limit = 2)
    val userId = segments.getOrNull(0).orEmpty()
    val styleId = segments.getOrNull(1).orEmpty()
    return if (userId.isEmpty() || styleId.isEmpty()) "" to "" else userId to styleId
}
