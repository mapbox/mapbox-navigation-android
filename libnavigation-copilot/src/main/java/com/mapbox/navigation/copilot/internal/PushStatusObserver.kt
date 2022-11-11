package com.mapbox.navigation.copilot.internal

/**
 * An interface which enables receiving information about push's status.
 */
fun interface PushStatusObserver {

    /**
     * Invoked every time the [PushStatus] is updated.
     *
     * @param pushStatus [PushStatus]
     */
    fun onPushStatusChanged(pushStatus: PushStatus)
}
