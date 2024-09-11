package com.mapbox.navigation.ui.maps.util

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.DelayedRoutesRenderedCallback
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedCallback

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun RoutesRenderedCallback.toDelayedRoutesRenderedCallback():
    DelayedRoutesRenderedCallback = DelayedRoutesRenderedCallback(this)
