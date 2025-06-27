package com.mapbox.navigation.ui.maps.route.callout.api.compose

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions

/**
 * UI state for route callout.
 * Normally, route callouts are drawn under the hood in NavSDK when this feature is enabled in [MapboxRouteLineApiOptions].
 * However, there might be cases when app wants to only get the callout data from NavSDK and attach the DVA itself.
 * An example of such a case is using Mapbox Maps SDK Compose extensions: attaching a DVA for
 * Compose MapboxMap is done via [compose-specific API](https://docs.mapbox.com/android/maps/examples/compose/dynamic-view-annotations/),
 * which is not currently supported by NavSDK.
 * In this case you may listen to [CalloutComposeUiStateData] updates and use its information by attach a DVA.
 * See [CalloutComposeUiStateProvider] for details on how to subscribe.
 *
 * @param callouts a list of currently relevant route callouts, see [CalloutComposeUiState] for details.
 */
@ExperimentalPreviewMapboxNavigationAPI
class CalloutComposeUiStateData internal constructor(
    val callouts: List<CalloutComposeUiState>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CalloutComposeUiStateData

        return callouts == other.callouts
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return callouts.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CalloutComposeUiStateData(callouts=$callouts)"
    }
}
