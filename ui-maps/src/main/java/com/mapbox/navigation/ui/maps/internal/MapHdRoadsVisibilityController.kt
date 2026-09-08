package com.mapbox.navigation.ui.maps.internal

import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxStyleManager
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW

/**
 * Manages the HD roads visibility of a single map.
 *
 * Tied to the camera lifecycle: the already-loaded style is handled on creation and every style (re)load
 * of the map afterwards. HD roads are hidden when the coordination module is not attached; with
 * coordination attached the style configuration is left untouched.
 *
 * It hides HD roads only when Navigation SDK is initialized. For the cases with delayed
 * Navigation initialization or delayed usage of the MapboxMap it doesn't hide the HD imports from the start.
 * It means, HD roads could be visible for some period until hidden.
 *
 */
@UiThread
internal class MapHdRoadsVisibilityController(private val mapboxMap: MapboxMap) {

    private var styleLoadedCancelable: Cancelable? = null

    init {
        mapboxMap.getStyle { hideHdRoadsIfNoCoordination(it) }
        styleLoadedCancelable = mapboxMap.subscribeStyleLoaded {
            hideHdRoadsIfNoCoordination(mapboxMap)
        }
    }

    /** Cancels the map style subscription. */
    fun onDestroy() {
        styleLoadedCancelable?.cancel()
        styleLoadedCancelable = null
    }

    /**
     * Skip style changes when coordination is used, when the map is already destroyed, when the
     * style has no such import or config key, when the config value is not a boolean, and when
     * the HD roads are hidden already.
     */
    private fun hideHdRoadsIfNoCoordination(styleManager: MapboxStyleManager) {
        if (hasCoordination) {
            return
        }
        if (!mapboxMap.isValid()) {
            logW(LOG_CATEGORY) { "Skipping HD roads update: the map is destroyed" }
            return
        }
        val hdImportContents = styleManager
            .getStyleImportConfigProperty(BASEMAP_IMPORT_ID, SHOW_HD_ROADS_CONFIG_KEY)
            .value?.value?.contents
        val visible = hdImportContents as? Boolean
        if (visible == null) {
            logW(LOG_CATEGORY) {
                "Unexpected type of $BASEMAP_IMPORT_ID:$SHOW_HD_ROADS_CONFIG_KEY: " +
                    "expected Boolean, got ${hdImportContents?.javaClass?.simpleName} " +
                    "($hdImportContents)"
            }
            return
        }
        if (!visible) {
            return
        }
        styleManager.setStyleImportConfigProperty(
            BASEMAP_IMPORT_ID,
            SHOW_HD_ROADS_CONFIG_KEY,
            Value.valueOf(false),
        ).fold(
            { error ->
                logW(LOG_CATEGORY) {
                    "Failed to hide $BASEMAP_IMPORT_ID:$SHOW_HD_ROADS_CONFIG_KEY, error: $error"
                }
            },
            {
                logI(LOG_CATEGORY) { "Hide $BASEMAP_IMPORT_ID:$SHOW_HD_ROADS_CONFIG_KEY" }
            },
        )
    }

    companion object {
        private const val LOG_CATEGORY = "MapHDRoadsVisibilityController"
        internal const val BASEMAP_IMPORT_ID = "basemap"
        internal const val SHOW_HD_ROADS_CONFIG_KEY = "showHdRoads"
        internal var hasCoordination = false
    }
}

/**
 * Marks whether a coordination navigation context is attached; drives the HD roads visibility
 * of the [MapHdRoadsVisibilityController]-managed maps. Must be called only by the coordination
 * module. Takes effect on the next style (re)load of each map.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun setCoordinationEnabled(enabled: Boolean) {
    MapHdRoadsVisibilityController.hasCoordination = enabled
}
