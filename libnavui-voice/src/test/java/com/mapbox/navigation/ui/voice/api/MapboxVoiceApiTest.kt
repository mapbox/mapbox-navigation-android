package com.mapbox.navigation.ui.voice.api

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

internal class MapboxVoiceApiTest {

    private lateinit var sut: MapboxVoiceApi
    private lateinit var mockSpeechProvider: MapboxSpeechProvider
    private lateinit var mockSpeechFileProvider: MapboxSpeechFileProvider

    @Before
    fun setUp() {
        mockSpeechProvider = mockk(relaxed = true)
        mockSpeechFileProvider = mockk(relaxed = true)

        sut = MapboxVoiceApi(mockSpeechProvider, mockSpeechFileProvider)
    }

    @Test
    fun `retrieveVoiceFile should download audio data using MapboxSpeechProvider`() = runBlocking {
        val voiceInstructions = Fixtures.ssmlInstructions()
        coEvery { mockSpeechProvider.load(any()) } returns ExpectedFactory.createError(Error())

        sut.retrieveVoiceFile(voiceInstructions)

        val announcement = TypeAndAnnouncement("ssml", voiceInstructions.ssmlAnnouncement()!!)
        coVerify { mockSpeechProvider.load(announcement) }
    }

    @Test
    fun `retrieveVoiceFile should save audio data to a file using MapboxSpeechFileProvider`() =
        runBlocking {
            val voiceInstructions = Fixtures.ssmlInstructions()
            val blob = byteArrayOf(11, 22)
            val blobInputStream = slot<InputStream>()
            coEvery { mockSpeechProvider.load(any()) } returns ExpectedFactory.createValue(blob)
            coEvery {
                mockSpeechFileProvider.generateVoiceFileFrom(capture(blobInputStream))
            } returns File("ignored")

            sut.retrieveVoiceFile(voiceInstructions)

            assertEquals(blob.count(), blobInputStream.captured.available())
        }

    @Test
    fun `retrieveVoiceFile should return VoiceFile on success`() =
        runBlocking {
            val voiceInstructions = Fixtures.ssmlInstructions()
            val blob = byteArrayOf(11, 22)
            val file = File("saved-audio-file")
            coEvery { mockSpeechProvider.load(any()) } returns ExpectedFactory.createValue(blob)
            coEvery { mockSpeechFileProvider.generateVoiceFileFrom(any()) } returns file

            val result = sut.retrieveVoiceFile(voiceInstructions)

            assertEquals(VoiceState.VoiceFile(file), result)
        }

    @Test
    fun `retrieveVoiceFile should return VoiceError on any error`() =
        runBlocking {
            val voiceInstructions = Fixtures.emptyInstructions()
            coEvery { mockSpeechProvider.load(any()) } returns ExpectedFactory.createError(Error())
            coEvery { mockSpeechFileProvider.generateVoiceFileFrom(any()) } throws Error()

            val result = sut.retrieveVoiceFile(voiceInstructions)

            assertTrue(result is VoiceState.VoiceError)
        }

    @Test
    fun `clean file`() = runBlocking {
        val mockedAnnouncement: SpeechAnnouncement = mockk()
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
        val mockedAnnouncement: SpeechAnnouncement = mockk()
        val nullFile: File? = null
        every { mockedAnnouncement.file } returns nullFile
        val fileProvider: MapboxSpeechFileProvider = mockk(relaxed = true)
        val speechProvider: MapboxSpeechProvider = mockk()
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        mapboxVoiceApi.clean(mockedAnnouncement)

        verify(exactly = 0) { fileProvider.delete(any()) }
    }

    @Test
    fun cancel() {
        val fileProvider = mockk<MapboxSpeechFileProvider>(relaxed = true)
        val speechProvider = mockk<MapboxSpeechProvider>()
        val mapboxVoiceApi = MapboxVoiceApi(speechProvider, fileProvider)

        mapboxVoiceApi.cancel()

        verify(exactly = 1) { fileProvider.cancel() }
    }
}
