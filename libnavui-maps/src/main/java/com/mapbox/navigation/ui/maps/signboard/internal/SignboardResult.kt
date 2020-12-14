package com.mapbox.navigation.ui.maps.signboard.internal

import com.mapbox.navigation.ui.base.MapboxResult

sealed class SignboardResult : MapboxResult {

    data class SignboardAvailable(
        val signboardUrl: String
    ) : SignboardResult()

    object SignboardUnavailable : SignboardResult()
}
