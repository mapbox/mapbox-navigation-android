package com.mapbox.navigation.core.reroute

import com.mapbox.bindgen.Expected
import com.mapbox.navigator.RerouteCallback
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver

internal class DisabledRerouteControllerInterface : NativeExtendedRerouteControllerInterface {
    override fun addRerouteObserver(rerouteObserver: RerouteObserver) = Unit

    override fun forceReroute() = Unit

    override fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) = Unit

    override fun setRerouteCallbackListener(
        rerouteCallback: ((result: Expected<RerouteError, RerouteInfo>) -> Unit)?
    ) = Unit

    override fun reroute(url: String, callback: RerouteCallback) = Unit

    override fun cancel() = Unit
}
