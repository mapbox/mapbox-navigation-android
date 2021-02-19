package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

@ExperimentalCoroutinesApi
class MapboxSpeechApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private var exceptions: MutableList<Throwable> = mutableListOf()
    private val coroutineExceptionHandler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            exceptions.add(exception)
        }
    private val testScope = CoroutineScope(
        parentJob + coroutineRule.testDispatcher + coroutineExceptionHandler
    )

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        mockkObject(VoiceApiProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
        unmockkObject(VoiceApiProvider)
        exceptions.clear()
    }

    @Test
    fun `generate voice file onAvailable`() = coroutineRule.runBlockingTest {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedVoiceInstructions: VoiceInstructions = mockk()
        val anAnnouncement = "Turn right onto Frederick Road, Maryland 3 55."
        val aSsmlAnnouncement = """
            <speak>
                <amazon:effect name="drc">
                    <prosody rate="1.08">Turn right onto Frederick Road, Maryland 3 55.</prosody>
                </amazon:effect>
            </speak>
        """.trimIndent()
        every { mockedVoiceInstructions.announcement() } returns anAnnouncement
        every { mockedVoiceInstructions.ssmlAnnouncement() } returns aSsmlAnnouncement
        val speechCallback: SpeechCallback = mockk()
        every { speechCallback.onAvailable(any()) } just Runs
        val mockedInstructionFile: File = mockk()
        val mockedVoiceApi: MapboxVoiceApi = mockk()
        coEvery {
            mockedVoiceApi.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockedInstructionFile)
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(aMockedContext, anyAccessToken, anyLanguage)
        } returns mockedVoiceApi
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyAccessToken, anyLanguage)

        mapboxSpeechApi.generate(mockedVoiceInstructions, speechCallback)

        verify(exactly = 1) {
            speechCallback.onAvailable(
                SpeechState.Speech.Available(
                    Announcement(anAnnouncement, aSsmlAnnouncement, mockedInstructionFile)
                )
            )
        }
    }

    @Test
    fun `generate voice file onError`() = coroutineRule.runBlockingTest {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedVoiceInstructions: VoiceInstructions = mockk()
        val speechCallback: SpeechCallback = mockk()
        every { speechCallback.onError(any()) } just Runs
        val mockedVoiceError: VoiceState.VoiceError = VoiceState.VoiceError(
            "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank"
        )
        val mockedVoiceApi: MapboxVoiceApi = mockk()
        coEvery {
            mockedVoiceApi.retrieveVoiceFile(any())
        } returns mockedVoiceError
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(aMockedContext, anyAccessToken, anyLanguage)
        } returns mockedVoiceApi
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyAccessToken, anyLanguage)

        mapboxSpeechApi.generate(mockedVoiceInstructions, speechCallback)

        verify(exactly = 1) {
            speechCallback.onError(
                SpeechState.Speech.Error(
                    "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank"
                )
            )
        }
    }

    @Test
    fun `generate voice file can't produce VoiceResponse VoiceState`() =
        coroutineRule.runBlockingTest {
            val aMockedContext: Context = mockk(relaxed = true)
            val anyAccessToken = "pk.123"
            val anyLanguage = Locale.US.language
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            val speechCallback: SpeechCallback = mockk()
            val mockedVoiceResponse: VoiceState.VoiceResponse = VoiceState.VoiceResponse(mockk())
            val mockedVoiceApi: MapboxVoiceApi = mockk()
            coEvery {
                mockedVoiceApi.retrieveVoiceFile(any())
            } returns mockedVoiceResponse
            every {
                VoiceApiProvider.retrieveMapboxVoiceApi(aMockedContext, anyAccessToken, anyLanguage)
            } returns mockedVoiceApi
            val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyAccessToken, anyLanguage)

            mapboxSpeechApi.generate(mockedVoiceInstructions, speechCallback)

            assertTrue(exceptions[0] is java.lang.IllegalStateException)
            assertEquals(
                "Invalid state: retrieveVoiceFile can't produce VoiceResponse VoiceState",
                exceptions[0].localizedMessage
            )
        }

    @Test
    fun clean() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedVoiceApi: MapboxVoiceApi = mockk()
        every { mockedVoiceApi.clean(any()) } just Runs
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(aMockedContext, anyAccessToken, anyLanguage)
        } returns mockedVoiceApi
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyAccessToken, anyLanguage)
        val anyAnnouncement: Announcement = mockk()

        mapboxSpeechApi.clean(anyAnnouncement)

        verify(exactly = 1) {
            mockedVoiceApi.clean(anyAnnouncement)
        }
    }
}
