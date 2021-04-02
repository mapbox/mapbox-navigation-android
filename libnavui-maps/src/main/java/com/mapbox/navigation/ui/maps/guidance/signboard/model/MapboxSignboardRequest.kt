package com.mapbox.navigation.ui.maps.guidance.signboard.model

import com.mapbox.common.HttpRequest

internal data class MapboxSignboardRequest(val requestId: Long, val httpRequest: HttpRequest)
