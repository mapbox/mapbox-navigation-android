package com.mapbox.services.android.navigation.v5.navigation

import okhttp3.ResponseBody
import retrofit2.Response

internal class TarResponseErrorMap(private val errorCodes: HashMap<Int, String> = HashMap()) {

    companion object {
        private const val TILES_ACCESS_TOKEN_ERROR_CODE = 402
        private const val BOUNDING_BOX_ERROR_CODE = 422
        private const val TILES_ACCESS_TOKEN_ERROR_MESSAGE =
            "Unable to fetch tiles: Before you can fetch " + "routing tiles you must obtain an enterprise access token. Please contact us at support@mapbox.com"
        private const val BOUNDING_BOX_ERROR_MESSAGE =
            "Unable to fetch tiles: The bounding box you have " + "specified is too large. Please select a smaller box and try again."
    }

    init {
        errorCodes[TILES_ACCESS_TOKEN_ERROR_CODE] = TILES_ACCESS_TOKEN_ERROR_MESSAGE
        errorCodes[BOUNDING_BOX_ERROR_CODE] = BOUNDING_BOX_ERROR_MESSAGE
    }

    fun buildErrorMessageWith(response: Response<ResponseBody>): String {
        var errorMessage = errorCodes[response.code()]
        if (errorMessage == null) {
            errorMessage = "Error code ${response.code()}: ${response.message()}"
            return errorMessage
        }
        return errorMessage
    }
}
