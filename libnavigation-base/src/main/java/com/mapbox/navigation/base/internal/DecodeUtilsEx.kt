package com.mapbox.navigation.base.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.utils.DecodeUtils

fun DecodeUtils.clearCache() {
    clearCacheInternal()
}

fun DecodeUtils.clearCacheExceptFor(routes: List<DirectionsRoute>) {
    clearCacheInternalExceptFor(routes)
}
