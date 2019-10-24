package com.mapbox.navigation.base.trip

interface TripService {
    val tripNotification: TripNotification

    fun startService(stateListener: StateListener)
    fun stopService()

    interface StateListener {
        fun onStateChanged(state: Any)

        // TODO state enum or separate lifecycle methods?
    }
}
