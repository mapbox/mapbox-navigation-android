package com.mapbox.navigation.navigator.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigator.RerouteObserver

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface RerouteEventsProvider {
    fun addRerouteObserver(nativeRerouteObserver: RerouteObserver)
    fun removeRerouteObserver(nativeRerouteObserver: RerouteObserver)
    fun addNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver,
    )
}
