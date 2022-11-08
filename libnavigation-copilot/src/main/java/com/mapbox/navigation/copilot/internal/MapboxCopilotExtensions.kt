@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.copilot.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.MapboxCopilot

/**
 * Register [PushStatusObserver] to receive information about push's status.
 *
 * @param pushStatusObserver
 */
fun MapboxCopilot.registerPushStatusObserver(pushStatusObserver: PushStatusObserver) {
    pushStatusObservers.add(pushStatusObserver)
}

/**
 * Unregister [PushStatusObserver].
 *
 * @param pushStatusObserver
 */
fun MapboxCopilot.unregisterPushStatusObserver(pushStatusObserver: PushStatusObserver) {
    pushStatusObservers.remove(pushStatusObserver)
}

/**
 * Unregister all [PushStatusObserver]s.
 *
 * @see [registerPushStatusObserver]
 */
fun MapboxCopilot.unregisterAllPushStatusObservers() {
    pushStatusObservers.clear()
}
