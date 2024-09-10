package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.nativeNavigator
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.ADASISv2MessageCallback

/**
 * Class that provides ADASIS API.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class AdasisManager internal constructor(
    private val navigator: MapboxNativeNavigator,
) {

    @Volatile
    private var adasisObserverParams: Pair<AdasisConfig, AdasisV2MessageObserver>? = null
    private val nativeAdasisMessageObserver: ADASISv2MessageCallback = ADASISv2MessageCallback {
        adasisObserverParams?.second?.onMessage(it)
    }

    init {
        navigator.setNativeNavigatorRecreationObserver {
            adasisObserverParams?.let { (config, _) ->
                setMessageObserver(config)
            }
        }
    }

    private fun setMessageObserver(config: AdasisConfig) {
        navigator.setAdasisMessageCallback(
            nativeAdasisMessageObserver,
            config.toNativeAdasisConfig(),
        )
    }

    /**
     * Sets a callback for ADASIS messages
     *
     * @param adasisConfig Adasis config
     * @param observer Adasis message observer
     */
    fun setAdasisMessageObserver(adasisConfig: AdasisConfig, observer: AdasisV2MessageObserver) {
        adasisObserverParams = adasisConfig to observer
        setMessageObserver(adasisConfig)
    }

    /**
     * Resets observer previously set via [setAdasisMessageObserver]
     */
    fun resetAdasisMessageObserver() {
        adasisObserverParams = null
        navigator.resetAdasisMessageCallback()
    }

    /**
     * Returns ADASIS data for a given Edge. If no ADAS data is available, returns null
     *
     * @param edgeId edgeId The ID of the edge for which to retrieve ADASis attributes
     *
     * @see [EHorizonObserver]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun getAdasisEdgeAttributes(edgeId: Long): AdasEdgeAttributes? {
        return navigator.graphAccessor.getAdasAttributes(edgeId)?.let {
            AdasEdgeAttributes.createFromNativeObject(it)
        }
    }

    companion object {

        /**
         * Creates a new instance of [AdasisManager] using the provided [MapboxNavigation] instance.
         *
         * **Note:** The lifecycle of the [MapboxNavigation] instance must exceed that of the
         * [AdasisManager]. In particular, [MapboxNavigation] should not be destroyed while the
         * returned [AdasisManager] is still in use.
         *
         * @return a new instance of [AdasisManager]
         */
        fun create(mapboxNavigation: MapboxNavigation): AdasisManager {
            return AdasisManager(mapboxNavigation.nativeNavigator)
        }
    }
}
