package com.mapbox.navigation.core.internal.telemetry

import androidx.annotation.StringDef

interface CustomEvent {

    /**
     * [CustomEvent] names scope
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        NavigationCustomEventType.ANALYTICS,
    )
    annotation class Type
}

object NavigationCustomEventType {
    const val ANALYTICS = "analytics"
}
