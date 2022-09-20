package com.mapbox.navigation.dropin

import android.content.Context
import android.content.res.Configuration
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlin.properties.Delegates

/**
 * Class that is a central place for map style loading.
 */
internal class MapStyleLoader(
    private val context: Context,
    private val options: NavigationViewOptions,
) {

    private val _loadedMapStyle = MutableStateFlow<Style?>(null)

    /**
     * StateFlow of fully loaded [Style] in [mapboxMap].
     */
    val loadedMapStyle = _loadedMapStyle.asStateFlow()

    /**
     * Current MapboxMap used by DropInNavigationView.
     *
     * Setting new MapboxMap will register `this` as OnStyleLoadedListener that
     * updates [_loadedMapStyle] StateFlow value every time new [Style] is fully loaded.
     */
    var mapboxMap: MapboxMap? by Delegates.observable(null) { _, oldMap, newMap ->
        oldMap?.removeOnStyleLoadedListener(onStyleLoadedListener)
        if (newMap != null) {
            _loadedMapStyle.value = newMap.getStyle()
            newMap.addOnStyleLoadedListener(onStyleLoadedListener)
        }
    }

    /**
     * Load initial map [Style] using latest [NavigationViewOptions.mapStyleUriNight] or
     * [NavigationViewOptions.mapStyleUriDay] value.
     */
    fun loadInitialStyle() {
        loadMapStyle(
            selectStyle(options.mapStyleUriDay.value, options.mapStyleUriNight.value)
        )
    }

    /**
     * Coroutine that observes both [NavigationViewOptions.mapStyleUriNight] and
     * [NavigationViewOptions.mapStyleUriDay] flows and reloads map [Style]
     */
    suspend fun observeAndReloadNewStyles() {
        combine(
            options.mapStyleUriDay,
            options.mapStyleUriNight,
            ::selectStyle
        ).collect(::loadMapStyle)
    }

    private fun loadMapStyle(styleUri: String) {
        if (styleUri != mapboxMap?.getStyle()?.styleURI) {
            mapboxMap?.loadStyleUri(styleUri)
        }
    }

    private fun selectStyle(dayStyleUri: String, nightStyleUri: String): String =
        if (isNightModeEnabled()) {
            nightStyleUri
        } else {
            dayStyleUri
        }

    private fun currentUiMode(): Int {
        return context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)
    }

    private fun isNightModeEnabled(): Boolean {
        return currentUiMode() == Configuration.UI_MODE_NIGHT_YES
    }

    private val onStyleLoadedListener = OnStyleLoadedListener {
        _loadedMapStyle.value = mapboxMap?.getStyle()
    }
}
