@file:JvmName("MapboxDirectionsEx")

package com.mapbox.navigation.route.internal.util

import com.mapbox.api.directions.v5.MapboxDirections
import okhttp3.HttpUrl
import okhttp3.Request

internal fun MapboxDirections.httpUrl(): HttpUrl = (this.cloneCall().request() as Request).url
