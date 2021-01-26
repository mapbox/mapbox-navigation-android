package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.voice.model.VoiceState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.File

@ExperimentalCoroutinesApi
class MapboxVoiceApiTest {

    @Test
    fun `retrieve voice file`() = runBlocking {
        val voiceInstructions: VoiceInstructions = mockk()
        every {
            voiceInstructions.announcement()
        } returns "Turn right onto Frederick Road, Maryland 3 55."
        every {
            voiceInstructions.ssmlAnnouncement()
        } returns """
            <speak>
                <amazon:effect name="drc">
                    <prosody rate="1.08">Turn right onto Frederick Road, Maryland 3 55.</prosody>
                </amazon:effect>
            </speak>
        """.trimIndent()
        val response: Response<ResponseBody> = mockk(relaxed = true)
        every { response.isSuccessful } returns true
        every { response.body() } returns mockk()
        val voiceResponse: VoiceState.VoiceResponse = mockk(relaxed = true)
        every { voiceResponse.response } returns response
        val speechProvider: MapboxSpeechProvider = mockk(relaxed = true)
        coEvery { speechProvider.enqueueCall(any()) } returns voiceResponse
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val file: File = mockk(relaxed = true)
        coEvery { fileProvider.generateVoiceFileFrom(any()) } returns file
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        val voiceState = mapboxVoiceApi.retrieveVoiceFile(voiceInstructions)

        assertTrue(voiceState is VoiceState.VoiceFile)
        assertEquals(file, (voiceState as VoiceState.VoiceFile).instructionFile)
    }
}
