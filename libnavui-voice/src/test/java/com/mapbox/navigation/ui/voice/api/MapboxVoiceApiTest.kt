package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.voice.model.VoiceState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.File

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

    @Test
    fun `retrieve voice file voice request failure if announcement is null`() = runBlocking {
        val voiceInstructions: VoiceInstructions = mockk()
        every {
            voiceInstructions.announcement()
        } returns null
        every {
            voiceInstructions.ssmlAnnouncement()
        } returns null
        val speechProvider: MapboxSpeechProvider = mockk(relaxed = true)
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        val voiceState = mapboxVoiceApi.retrieveVoiceFile(voiceInstructions)

        assertTrue(voiceState is VoiceState.VoiceError)
        assertEquals(
            "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
            (
                voiceState as VoiceState.VoiceError
                ).exception
        )
    }

    @Test
    fun `retrieve voice file voice request failure if announcement is blank`() = runBlocking {
        val voiceInstructions: VoiceInstructions = mockk()
        every {
            voiceInstructions.announcement()
        } returns ""
        every {
            voiceInstructions.ssmlAnnouncement()
        } returns null
        val speechProvider: MapboxSpeechProvider = mockk(relaxed = true)
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        val voiceState = mapboxVoiceApi.retrieveVoiceFile(voiceInstructions)

        assertTrue(voiceState is VoiceState.VoiceError)
        assertEquals(
            "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
            (voiceState as VoiceState.VoiceError).exception
        )
    }

    @Test
    fun `retrieve voice file voice response failure no data`() = runBlocking {
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
        every { response.code() } returns 204
        every { response.body() } returns null
        val voiceResponse: VoiceState.VoiceResponse = mockk(relaxed = true)
        every { voiceResponse.response } returns response
        val speechProvider: MapboxSpeechProvider = mockk(relaxed = true)
        coEvery { speechProvider.enqueueCall(any()) } returns voiceResponse
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val file: File = mockk(relaxed = true)
        coEvery { fileProvider.generateVoiceFileFrom(any()) } returns file
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        val voiceState = mapboxVoiceApi.retrieveVoiceFile(voiceInstructions)

        assertTrue(voiceState is VoiceState.VoiceError)
        assertEquals(
            "code: 204, error: No data available",
            (voiceState as VoiceState.VoiceError).exception
        )
    }

    @Test
    fun `retrieve voice file voice response failure not successful`() = runBlocking {
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
        every { response.isSuccessful } returns false
        every { response.code() } returns 403
        val responseBody: ResponseBody = mockk()
        every { responseBody.string() } returns "Forbidden"
        every { response.errorBody() } returns responseBody
        val voiceResponse: VoiceState.VoiceResponse = mockk(relaxed = true)
        every { voiceResponse.response } returns response
        val speechProvider: MapboxSpeechProvider = mockk(relaxed = true)
        coEvery { speechProvider.enqueueCall(any()) } returns voiceResponse
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val file: File = mockk(relaxed = true)
        coEvery { fileProvider.generateVoiceFileFrom(any()) } returns file
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        val voiceState = mapboxVoiceApi.retrieveVoiceFile(voiceInstructions)

        assertTrue(voiceState is VoiceState.VoiceError)
        assertEquals(
            "code: 403, error: Forbidden",
            (voiceState as VoiceState.VoiceError).exception
        )
    }

    @Test
    fun `retrieve voice file voice response failure not successful no error body`() = runBlocking {
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
        every { response.isSuccessful } returns false
        every { response.code() } returns 500
        every { response.errorBody() } returns null
        val voiceResponse: VoiceState.VoiceResponse = mockk(relaxed = true)
        every { voiceResponse.response } returns response
        val speechProvider: MapboxSpeechProvider = mockk(relaxed = true)
        coEvery { speechProvider.enqueueCall(any()) } returns voiceResponse
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val file: File = mockk(relaxed = true)
        coEvery { fileProvider.generateVoiceFileFrom(any()) } returns file
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        val voiceState = mapboxVoiceApi.retrieveVoiceFile(voiceInstructions)

        assertTrue(voiceState is VoiceState.VoiceError)
        assertEquals(
            "code: 500, error: Unknown",
            (voiceState as VoiceState.VoiceError).exception
        )
    }

    @Test
    fun `clean file`() = runBlocking {
        val mockedAnnouncement: Announcement = mockk()
        val mockedFile: File = mockk()
        every { mockedAnnouncement.file } returns mockedFile
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val speechProvider: MapboxSpeechProvider = mockk()
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        mapboxVoiceApi.clean(mockedAnnouncement)

        verify(exactly = 1) { fileProvider.delete(mockedFile) }
    }

    @Test
    fun `clean no file`() = runBlocking {
        val mockedAnnouncement: Announcement = mockk()
        val nullFile: File? = null
        every { mockedAnnouncement.file } returns nullFile
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val speechProvider: MapboxSpeechProvider = mockk()
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        mapboxVoiceApi.clean(mockedAnnouncement)

        verify(exactly = 0) { fileProvider.delete(any()) }
    }
}
