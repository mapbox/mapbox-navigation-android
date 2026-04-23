package com.mapbox.navigation.core.internal

import android.os.HandlerThread
import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.RoadGraphDataUpdateCallback
import com.mapbox.navigation.core.RoadGraphVersionInfoCallback
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val MapboxNavigation.nativeNavigator: MapboxNativeNavigator
    get() = navigator

fun MapboxNavigation.internalSetExternallyRefreshedRoutes(
    routes: List<NavigationRoute>,
    isManualRefresh: Boolean,
) {
    setExternallyRefreshedRoutes(routes, isManualRefresh)
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
val MapboxNavigation.locationInputThread: HandlerThread
    get() = locationInputHandlerThread

@ExperimentalPreviewMapboxNavigationAPI
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.internalRequestHDGraphDataUpdate(callback: RoadGraphDataUpdateCallback) {
    CacheHandleWrapper.requestHDGraphDataUpdate(navigator.cache, callback)
}

@ExperimentalPreviewMapboxNavigationAPI
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigation.internalGetHDGraphVersionInfo(
    callback: RoadGraphVersionInfoCallback,
) {
    navigator.cache.getCurrentHDGraphVersionInfo { isVersionResolved, currentVersionInfo ->
        if (!isVersionResolved || currentVersionInfo == null) {
            callback.onError(isTimeoutError = !isVersionResolved)
        } else {
            callback.onVersionInfo(
                RoadGraphVersionInfoCallback.VersionInfo(
                    dataset = currentVersionInfo.dataset,
                    version = currentVersionInfo.version,
                ),
            )
        }
    }
}
