@file:JvmName("ShieldExtensions")

package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.navigation.base.utils.ifNonNull
import com.mapbox.navigator.Shield

fun Shield?.toMapboxShield(): MapboxShield? {
    return ifNonNull(this) { mapboxShield ->
        MapboxShield
            .builder()
            .name(mapboxShield.name)
            .baseUrl(mapboxShield.baseUrl)
            .textColor(mapboxShield.textColor)
            .displayRef(mapboxShield.displayRef)
            .build()
    }
}
