package com.mapbox.navigation.core.internal.telemetry

import androidx.annotation.RestrictTo
import com.mapbox.navigator.OuterDeviceAction

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
enum class AndroidAutoEvent {
    CONNECTED,
    DISCONNECTED,
    ;

    fun mapToNative(): OuterDeviceAction {
        return when (this) {
            CONNECTED -> OuterDeviceAction.CONNECTED
            DISCONNECTED -> OuterDeviceAction.DISCONNECTED
        }
    }
}
