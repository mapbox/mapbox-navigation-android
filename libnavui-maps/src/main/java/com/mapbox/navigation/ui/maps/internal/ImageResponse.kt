package com.mapbox.navigation.ui.maps.internal

import java.io.InputStream

internal sealed class ImageResponse {
    data class Success(val bytes: ByteArray) : ImageResponse()
    data class Failure(val error: String? = null) : ImageResponse()
}
