package com.mapbox.navigation.ui.maps.route.line

import java.util.UUID

internal open class RouteLineHistoryRecordingInstance {
    protected val instanceId: String = UUID.randomUUID().toString()
}
