package com.mapbox.navigation.ui.maps.signboard

import com.mapbox.common.HttpRequest

internal sealed class SignboardResult {

    data class SignboardAvailable(
        val signboardUrl: String
    ) : SignboardResult()

    object SignboardUnavailable : SignboardResult()

    data class SignboardRequest(
        val request: HttpRequest
    ) : SignboardResult()

    sealed class Signboard : SignboardResult() {
        object Empty : Signboard()
        data class Failure(val error: String?) : Signboard()
        data class Success(val data: ByteArray) : Signboard()
    }
}
