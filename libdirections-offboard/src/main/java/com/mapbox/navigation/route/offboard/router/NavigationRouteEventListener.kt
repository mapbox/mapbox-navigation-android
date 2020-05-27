package com.mapbox.navigation.route.offboard.router

import okhttp3.Call
import okhttp3.EventListener

internal class NavigationRouteEventListener(
    val time: ElapsedTime = ElapsedTime()
) : EventListener() {

    override fun callStart(call: Call) {
        super.callStart(call)
        time.start()
    }

    override fun callEnd(call: Call) {
        super.callEnd(call)
        time.end()
    }
}
