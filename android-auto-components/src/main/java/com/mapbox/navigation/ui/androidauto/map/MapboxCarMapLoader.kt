package com.mapbox.navigation.ui.androidauto.map

import androidx.car.app.CarContext
import androidx.car.app.Session
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.maps.NavigationStyles

/**
 * Default map style loader that is designed for Android Auto. This map loader assumes you want to
 * keep map styles in sync with the [CarContext.isDarkMode].
 *
 * To use, register an instance to [MapboxCarMap.registerObserver]. It will automatically load the
 * map for as long as it is registered. To disable you can unregister it with
 * [MapboxCarMap.unregisterObserver]. Override the [Session.onCarConfigurationChanged] and call the
 * [onCarConfigurationChanged].
 */
class MapboxCarMapLoader : MapboxCarMapObserver {

    private var mapboxMap: MapboxMap? = null
    private var lightStyleOverride: StyleContract.StyleExtension? = null
    private var darkStyleOverride: StyleContract.StyleExtension? = null
    private var loadedStyle: StyleContract.StyleExtension? = null

    private val logMapError = object : OnMapLoadErrorListener {
        override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
            val errorData = "${eventData.type} ${eventData.message}"
            logAndroidAutoFailure("onMapLoadError $errorData")
            // Let the next configuration change try to load the style again.
            loadedStyle = null
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap()
        with(mapboxCarMapSurface) {
            logAndroidAuto("onAttached load style")
            val styleExtension = getStyleExtension(carContext.isDarkMode)
            loadedStyle = styleExtension
            mapSurface.getMapboxMap().loadStyle(
                styleExtension,
                onStyleLoaded = { logAndroidAuto("onAttached style loaded") },
                onMapLoadErrorListener = logMapError,
            )
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapboxMap = null
        loadedStyle = null
    }

    /**
     * Returns the current style contract. If an override has not been set the default is returned.
     */
    fun getStyleExtension(isDarkMode: Boolean): StyleContract.StyleExtension {
        return if (isDarkMode) {
            darkStyleOverride ?: DEFAULT_NIGHT_STYLE
        } else {
            lightStyleOverride ?: DEFAULT_DAY_STYLE
        }
    }

    /**
     * Set the map style to apply when [CarContext.isDarkMode] is false. Setting to `null` will use
     * a default map style.
     *
     * @see onCarConfigurationChanged to apply changes after changing.
     */
    fun setLightStyleOverride(styleContract: StyleContract.StyleExtension?) = apply {
        this.lightStyleOverride = styleContract
    }

    /**
     * Set the map style to apply when [CarContext.isDarkMode] is true. Setting to `null` will use
     * a default map style.
     *
     * @see onCarConfigurationChanged to apply changes after changing.
     */
    fun setDarkStyleOverride(styleContract: StyleContract.StyleExtension?) = apply {
        this.darkStyleOverride = styleContract
    }

    /**
     * This will use [CarContext.isDarkMode] to determine if the dark or light style should be
     * loaded. The style is loaded only when it differs from the loaded one, which happens when
     * [CarContext.isDarkMode] or the override for it changed. Other configuration changes do not
     * reload the style. If this is called while the map is detached, there is no operation.
     *
     * @see setLightStyleOverride
     * @see setDarkStyleOverride
     *
     * @param carContext forwarded from [Session.onCarConfigurationChanged]
     */
    fun onCarConfigurationChanged(carContext: CarContext) = apply {
        val mapboxMap = mapboxMap ?: run {
            logAndroidAuto(
                "onCarConfigurationChanged did not load the map because the map is not attached",
            )
            return@apply
        }
        val styleExtension = getStyleExtension(carContext.isDarkMode)
        if (styleExtension === loadedStyle) {
            logAndroidAuto("onCarConfigurationChanged keeps the loaded map style")
            return@apply
        }
        loadedStyle = styleExtension
        mapboxMap.loadStyle(
            styleExtension,
            onStyleLoaded = { style ->
                logAndroidAuto("updateMapStyle styleAvailable ${style.styleURI}")
            },
            onMapLoadErrorListener = logMapError,
        )
    }

    private companion object {
        private val DEFAULT_DAY_STYLE = style(NavigationStyles.NAVIGATION_DAY_STYLE) { }
        private val DEFAULT_NIGHT_STYLE = style(NavigationStyles.NAVIGATION_NIGHT_STYLE) { }
    }
}
