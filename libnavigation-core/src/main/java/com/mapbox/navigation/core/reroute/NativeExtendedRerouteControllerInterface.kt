package com.mapbox.navigation.core.reroute

import com.mapbox.bindgen.Expected
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver

internal interface NativeExtendedRerouteControllerInterface : RerouteControllerInterface {

    fun addRerouteObserver(rerouteObserver: RerouteObserver)

    fun forceReroute()

    fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?)

    fun setRerouteCallbackListener(
        rerouteCallback: ((result: Expected<RerouteError, RerouteInfo>) -> Unit)?
    )
}
