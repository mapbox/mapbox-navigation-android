@file:JvmName("MapboxDirectionsEx")

package com.mapbox.navigation.route.internal.util

import com.mapbox.api.directions.v5.MapboxDirections
import okhttp3.HttpUrl

internal fun MapboxDirections.httpUrl(): HttpUrl = this.cloneCall().request().url()
