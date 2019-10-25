package com.mapbox.navigation.base.route

import com.mapbox.navigation.utils.time.ElapsedTime
import okhttp3.Call
import okhttp3.EventListener

class NavigationRouteEventListener
@JvmOverloads
constructor(
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
