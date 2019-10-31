package com.mapbox.navigation.base.model.route

enum class RouteState private constructor() {
    INVALID,
    INITIALIZED,
    TRACKING,
    COMPLETE,
    OFFROUTE,
    STALE
}
