package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.ui.voice.testutils.Fixtures
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createMainScopeJobControl()
        } returns JobControl(parentJob, testScope)
        mockkObject(VoiceApiProvider)
        mockkObject(VoiceInstructionsParser)
    }

    @After
    fun tearDown() {
        unmockkObject(InternalJobControlFactory)
        unmockkObject(VoiceApiProvider)
        unmockkObject(VoiceInstructionsParser)
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
        val speechConsumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> = mockk()
        val speechValueSlot = slot<Expected<SpeechError, SpeechValue>>()
        every { speechConsumer.accept(capture(speechValueSlot)) } just Runs
        val mockedInstructionFile: File = mockk()
        val mockedVoiceApi: MapboxVoiceApi = mockk()
        coEvery {
            mockedVoiceApi.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockedInstructionFile)
        val options = MapboxSpeechApiOptions.Builder().build()
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                options
            )
        } returns mockedVoiceApi
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyAccessToken, anyLanguage)

        mapboxSpeechApi.generate(mockedVoiceInstructions, speechConsumer)

        verify(exactly = 1) {
            speechConsumer.accept(
                speechValueSlot.captured
            )
        }
    }

    @Test
    fun `generate voice file onError`() = coroutineRule.runBlockingTest {
        val voiceInstructions = Fixtures.ssmlInstructions()
        val speechErrorCapture = slot<Expected<SpeechError, SpeechValue>>()
        val speechConsumer = mockk<MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>> {
            every { accept(capture(speechErrorCapture)) } just Runs
        }
        val mockedVoiceApi = mockk<MapboxVoiceApi> {
            coEvery {
                retrieveVoiceFile(voiceInstructions)
            } returns VoiceState.VoiceError("Some error message")
        }
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(any(), any(), any(), any())
        } returns mockedVoiceApi

        val sut = MapboxSpeechApi(mockk(relaxed = true), "pk.123", Locale.US.language)
        sut.generate(voiceInstructions, speechConsumer)
        val result = speechErrorCapture.captured

        assertEquals(
            voiceInstructions.announcement()!!,
            result.error!!.fallback.announcement
        )
        assertEquals(
            voiceInstructions.ssmlAnnouncement()!!,
            speechErrorCapture.captured.error!!.fallback.ssmlAnnouncement
        )
        assertNull(result.error!!.fallback.file)
    }

    @Test
    fun `generate voice file onError with IllegalStateException`() = coroutineRule.runBlockingTest {
        val voiceInstructions = Fixtures.nullInstructions()
        val speechErrorCapture = slot<Expected<SpeechError, SpeechValue>>()
        val speechConsumer = mockk<MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>> {
            every { accept(capture(speechErrorCapture)) } just Runs
        }
        val mockedVoiceApi = mockk<MapboxVoiceApi> {
            coEvery {
                retrieveVoiceFile(voiceInstructions)
            } returns VoiceState.VoiceError("Some error message")
        }
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(any(), any(), any(), any())
        } returns mockedVoiceApi

        val sut = MapboxSpeechApi(mockk(relaxed = true), "pk.123", Locale.US.language)
        sut.generate(voiceInstructions, speechConsumer)

        assertTrue(exceptions[0] is java.lang.IllegalStateException)
    }

    @Test
    fun clean() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyAccessToken = "pk.123"
        val anyLanguage = Locale.US.language
        val mockedVoiceApi: MapboxVoiceApi = mockk()
        every { mockedVoiceApi.clean(any()) } just Runs
        val options = MapboxSpeechApiOptions.Builder().build()
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(
                aMockedContext,
                anyAccessToken,
                anyLanguage,
                options
            )
        } returns mockedVoiceApi
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyAccessToken, anyLanguage)
        val anyAnnouncement: SpeechAnnouncement = mockk()

        mapboxSpeechApi.clean(anyAnnouncement)

        verify(exactly = 1) {
            mockedVoiceApi.clean(anyAnnouncement)
        }
    }

    @Test
    fun destroy() {
        val mockedVoiceApi = mockk<MapboxVoiceApi>(relaxed = true)
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(any(), any(), any(), any())
        } returns mockedVoiceApi

        val sut = MapboxSpeechApi(mockk(relaxed = true), "pk.123", Locale.US.language)
        sut.destroy()

        verify { mockedVoiceApi.destroy() }
    }
}
