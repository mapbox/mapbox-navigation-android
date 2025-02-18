package com.mapbox.navigation.mapgpt.core.api.native

import com.mapbox.mapgpt.experimental.MapgptSessionType
import com.mapbox.navigation.mapgpt.api.native.MapGptSessionType

fun MapGptSessionType.toMapgptSessionType(): MapgptSessionType {
    return when (this) {
        MapGptSessionType.ASR -> MapgptSessionType.ASR
        MapGptSessionType.TEXT -> MapgptSessionType.TEXT
        MapGptSessionType.BOTH -> MapgptSessionType.BOTH
    }
}

