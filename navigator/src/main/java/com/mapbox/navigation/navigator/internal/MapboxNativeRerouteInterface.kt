package com.mapbox.navigation.navigator.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteDetectorInterface
import com.mapbox.navigator.RerouteObserver

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface MapboxNativeRerouteInterface {
    fun addRerouteObserver(nativeRerouteObserver: RerouteObserver)
    fun removeRerouteObserver(nativeRerouteObserver: RerouteObserver)

    fun addNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver,
    )

    fun getRerouteDetector(): RerouteDetectorInterface?
    fun getRerouteController(): RerouteControllerInterface?

    fun nativeRerouteEnabled(): Boolean
}
