package com.mapbox.navigation.ui.voice.model

import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

/**
 * Immutable object representing the voice data to be played.
 */
internal sealed class VoiceState {

    /**
     * State representing data about the response from Mapbox's API Voice.
     * @param response
     */
    data class VoiceResponse(val response: Response<ResponseBody>) : VoiceState()

    /**
     * State representing data about the instruction file.
     * @property instructionFile [File]
     */
    data class VoiceFile(val instructionFile: File) : VoiceState()

    /**
     * The state is returned if there is an error preparing the [File]
     * @property exception String error message
     */
    data class VoiceError(val exception: String) : VoiceState()
}
