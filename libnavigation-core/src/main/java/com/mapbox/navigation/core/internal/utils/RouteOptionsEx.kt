package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl

/**
 * Check if uuid is valid:
 * - [RouteOptions] is not **null**;
 * - uuid is not empty;
 * - uuid is not equal to [MapboxNativeNavigatorImpl.OFFLINE_UUID].
 */
internal fun RouteOptions?.isUuidValidForRefresh(): Boolean =
    this != null &&
        requestUuid().isNotEmpty() &&
        requestUuid() != MapboxNativeNavigatorImpl.OFFLINE_UUID
