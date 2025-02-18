package com.mapbox.navigation.mapgpt.api.native

sealed class MapGptSessionType {
    object ASR: MapGptSessionType()
    object TEXT: MapGptSessionType()
    object BOTH: MapGptSessionType()
}
