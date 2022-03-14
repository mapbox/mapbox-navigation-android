package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.RerouteCallback
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver
import java.net.URL

internal class NativeRerouteControllerWrapper(
    private val accessToken: String?,
    private val nativeRerouteController: RerouteControllerInterface,
    private val nativeNavigator: MapboxNativeNavigator,
) : NativeExtendedRerouteControllerInterface {

    private var rerouteOptionsAdapter: RerouteOptionsAdapter? = null
    private var rerouteCallback: ((result: Expected<RerouteError, RerouteInfo>) -> Unit)? = null

    override fun reroute(url: String, callback: RerouteCallback) {
        val newUrl = when (val adapter = rerouteOptionsAdapter) {
            // access token is not attached to provided url and requests are failed
            // TODO remove workaround when nn #issue is resolved
            null -> RouteOptions.fromUrl(URL(url)).toUrl(accessToken ?: "").toString()
            else -> adapter.onRouteOptions(RouteOptions.fromUrl(URL(url)))
                .toUrl(accessToken ?: "")
                .toString()
        }
        nativeRerouteController.reroute(newUrl) { result ->
            rerouteCallback?.invoke(result)
            callback.run(result)
        }
    }

    override fun cancel() {
        nativeRerouteController.cancel()
    }

    override fun addRerouteObserver(rerouteObserver: RerouteObserver) {
        nativeNavigator.addRerouteObserver(rerouteObserver)
    }

    override fun forceReroute() {
        nativeNavigator.getRerouteDetector().forceReroute()
    }

    override fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        this.rerouteOptionsAdapter = rerouteOptionsAdapter
    }

    override fun setRerouteCallbackListener(
        rerouteCallback: ((result: Expected<RerouteError, RerouteInfo>) -> Unit)?
    ) {
        this.rerouteCallback = rerouteCallback
    }
}
