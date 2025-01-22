package com.mapbox.navigation.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.model.SpeechValue
import com.mapbox.navigation.voice.model.VoiceState
import com.mapbox.navigation.voice.testutils.Fixtures
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@ExperimentalCoroutinesApi
class MapboxSpeechApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val voiceAPI = mockk<MapboxVoiceApi>(relaxed = true)
    private val parentJob = SupervisorJob()
    private val predownloadParentJob = SupervisorJob()
    private var exceptions: MutableList<Throwable> = mutableListOf()
    private val coroutineExceptionHandler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            exceptions.add(exception)
        }
    private val testScope = CoroutineScope(
        parentJob + coroutineRule.testDispatcher + coroutineExceptionHandler,
    )
    private val predownloadScope = CoroutineScope(
        predownloadParentJob + coroutineRule.testDispatcher + coroutineExceptionHandler,
    )

    @Before
    fun setUp() {
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createMainScopeJobControl()
        } returns JobControl(parentJob, testScope)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(predownloadParentJob, predownloadScope)
        mockkObject(VoiceApiProvider)
        every {
            VoiceApiProvider.retrieveMapboxVoiceApi(any(), any(), any())
        } returns voiceAPI
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
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockedInstructionFile)
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyLanguage)

        mapboxSpeechApi.generate(mockedVoiceInstructions, speechConsumer)

        verify(exactly = 1) {
            speechConsumer.accept(
                speechValueSlot.captured,
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
        coEvery {
            voiceAPI.retrieveVoiceFile(voiceInstructions)
        } returns VoiceState.VoiceError("Some error message")

        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.generate(voiceInstructions, speechConsumer)
        val result = speechErrorCapture.captured

        assertEquals(
            voiceInstructions.announcement()!!,
            result.error!!.fallback.announcement,
        )
        assertEquals(
            voiceInstructions.ssmlAnnouncement()!!,
            speechErrorCapture.captured.error!!.fallback.ssmlAnnouncement,
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
        coEvery {
            voiceAPI.retrieveVoiceFile(voiceInstructions)
        } returns VoiceState.VoiceError("Some error message")

        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.generate(voiceInstructions, speechConsumer)

        assertTrue(exceptions[0] is java.lang.IllegalStateException)
    }

    @Test
    fun `generatePredownloaded for invalid instruction`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.generatePredownloaded(VoiceInstructions.builder().build(), consumer)

        coVerify(exactly = 0) {
            consumer.accept(any())
        }
        assertTrue(exceptions[0] is java.lang.IllegalStateException)
    }

    @Test
    fun `generatePredownloaded no cached value`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.generatePredownloaded(
            VoiceInstructions.builder().announcement("turn up and down").build(),
            consumer,
        )

        coVerify(exactly = 1) {
            consumer.accept(match { it.isError })
        }
    }

    @Test
    fun `generatePredownloaded has cached value`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        val file = mockk<File>(relaxed = true)
        val speechAnnouncement = SpeechAnnouncement.Builder("turn up and down").file(file).build()
        coEvery { voiceAPI.retrieveVoiceFile(instruction) } returns VoiceState.VoiceFile(file)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction))

        sut.generatePredownloaded(instruction, consumer)

        coVerify(exactly = 1) {
            consumer.accept(match { it.value!!.announcement == speechAnnouncement })
        }
    }

    @Test
    fun `generatePredownloaded has removed via clean cached value`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        val file = mockk<File>(relaxed = true)
        val speechAnnouncement = SpeechAnnouncement.Builder("turn up and down").file(file).build()
        coEvery { voiceAPI.retrieveVoiceFile(instruction) } returns VoiceState.VoiceFile(file)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction))
        sut.clean(speechAnnouncement)

        sut.generatePredownloaded(instruction, consumer)

        coVerify(exactly = 1) {
            consumer.accept(match { it.isError })
        }
    }

    @Test
    fun `generatePredownloaded, another cached value is cleaned`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        val file = mockk<File>(relaxed = true)
        val speechAnnouncement = SpeechAnnouncement.Builder("turn up and down").file(file).build()
        coEvery { voiceAPI.retrieveVoiceFile(instruction) } returns VoiceState.VoiceFile(file)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction))
        sut.clean(SpeechAnnouncement.Builder("announcement").build())

        sut.generatePredownloaded(instruction, consumer)

        coVerify(exactly = 1) {
            consumer.accept(match { it.value!!.announcement == speechAnnouncement })
        }
    }

    @Test
    fun `generatePredownloaded, fallback cached value is cleaned`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        val file = mockk<File>(relaxed = true)
        val speechAnnouncement = SpeechAnnouncement.Builder("turn up and down").file(file).build()
        val fallbackAnnouncement = SpeechAnnouncement.Builder("turn up and down").build()
        coEvery { voiceAPI.retrieveVoiceFile(instruction) } returns VoiceState.VoiceFile(file)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction))
        sut.clean(fallbackAnnouncement)

        sut.generatePredownloaded(instruction, consumer)

        coVerify(exactly = 1) {
            consumer.accept(match { it.value!!.announcement == speechAnnouncement })
        }
    }

    @Test
    fun `generatePredownloaded has removed via destroy cached value`() = coroutineRule.runBlockingTest {
        val consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> =
            mockk(relaxed = true)
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        val file = mockk<File>(relaxed = true)
        coEvery { voiceAPI.retrieveVoiceFile(instruction) } returns VoiceState.VoiceFile(file)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction))
        sut.cancelPredownload()

        sut.generatePredownloaded(instruction, consumer)

        coVerify(exactly = 1) {
            consumer.accept(match { it.isError })
        }
    }

    @Test
    fun `predownload with empty list`() = coroutineRule.runBlockingTest {
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(emptyList())

        coVerify(exactly = 0) { voiceAPI.retrieveVoiceFile(any()) }
    }

    @Test
    fun `predownload with invalid instruction`() = coroutineRule.runBlockingTest {
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(listOf(VoiceInstructions.builder().build()))

        coVerify(exactly = 0) { voiceAPI.retrieveVoiceFile(any()) }
    }

    @Test
    fun `predownload with new instruction`() = coroutineRule.runBlockingTest {
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(listOf(instruction))

        coVerify(exactly = 1) { voiceAPI.retrieveVoiceFile(instruction) }
    }

    @Test
    fun `predownload with same instruction of different type`() = coroutineRule.runBlockingTest {
        val announcement = "turn up and down"
        val instruction = VoiceInstructions.builder().announcement(announcement).build()
        val newInstruction = VoiceInstructions.builder().ssmlAnnouncement(announcement).build()
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockk(relaxed = true))
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(listOf(instruction))
        clearMocks(voiceAPI, answers = false)

        sut.predownload(listOf(newInstruction))

        coVerify(exactly = 1) { voiceAPI.retrieveVoiceFile(newInstruction) }
    }

    @Test
    fun `predownload with existing instruction`() = coroutineRule.runBlockingTest {
        val announcement = "turn up and down"
        val instruction = VoiceInstructions.builder().announcement(announcement).build()
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockk(relaxed = true))
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(listOf(instruction))
        clearMocks(voiceAPI, answers = false)

        sut.predownload(listOf(instruction))

        coVerify(exactly = 0) { voiceAPI.retrieveVoiceFile(instruction) }
    }

    @Test
    fun `predownload with multiple new instructions`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(listOf(instruction1, instruction2))

        coVerify(exactly = 1) {
            voiceAPI.retrieveVoiceFile(instruction1)
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }

    @Test
    fun `failed download does not save instruction`() = coroutineRule.runBlockingTest {
        val instruction = VoiceInstructions.builder().announcement("turn up and down").build()
        coEvery { voiceAPI.retrieveVoiceFile(instruction) } returns VoiceState.VoiceError("")
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.predownload(listOf(instruction))
        clearMocks(voiceAPI, answers = false)

        sut.predownload(listOf(instruction))

        coVerify(exactly = 1) { voiceAPI.retrieveVoiceFile(instruction) }
    }

    @Test
    fun `clean existing instruction with file`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        val file1 = mockk<File>(relaxed = true)
        val file2 = mockk<File>(relaxed = true)
        coEvery {
            voiceAPI.retrieveVoiceFile(instruction1)
        } returns VoiceState.VoiceFile(file1)
        coEvery {
            voiceAPI.retrieveVoiceFile(instruction2)
        } returns VoiceState.VoiceFile(file2)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction1, instruction2))
        clearMocks(voiceAPI, answers = false)

        sut.clean(SpeechAnnouncement.Builder("turn up and down").file(file1).build())

        sut.predownload(listOf(instruction1, instruction2))
        coVerify(exactly = 1) {
            voiceAPI.retrieveVoiceFile(instruction1)
        }
        coVerify(exactly = 0) {
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }

    @Test
    fun `clean existing instruction with no file`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockk(relaxed = true))
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction1, instruction2))
        clearMocks(voiceAPI, answers = false)

        sut.clean(SpeechAnnouncement.Builder("turn up and down").build())

        sut.predownload(listOf(instruction1, instruction2))
        coVerify(exactly = 0) {
            voiceAPI.retrieveVoiceFile(instruction1)
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }

    @Test
    fun `clean existing instruction with different type`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockk(relaxed = true))
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction1, instruction2))
        clearMocks(voiceAPI, answers = false)

        sut.clean(
            SpeechAnnouncement.Builder("new announcement")
                .ssmlAnnouncement("turn up and down")
                .build(),
        )

        sut.predownload(listOf(instruction1, instruction2))
        coVerify(exactly = 0) {
            voiceAPI.retrieveVoiceFile(instruction1)
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }

    @Test
    fun `clean unexisting instruction`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockk(relaxed = true))
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction1, instruction2))
        clearMocks(voiceAPI, answers = false)

        sut.clean(SpeechAnnouncement.Builder("new announcement").build())

        sut.predownload(listOf(instruction1, instruction2))
        coVerify(exactly = 0) {
            voiceAPI.retrieveVoiceFile(instruction1)
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }

    @Test
    fun `clean invalid instruction`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        coEvery {
            voiceAPI.retrieveVoiceFile(any())
        } returns VoiceState.VoiceFile(mockk(relaxed = true))
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction1, instruction2))
        clearMocks(voiceAPI, answers = false)

        sut.clean(SpeechAnnouncement.Builder("").build())

        sut.predownload(listOf(instruction1, instruction2))
        coVerify(exactly = 0) {
            voiceAPI.retrieveVoiceFile(instruction1)
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }

    @Test
    fun `clean with no instructions`() {
        val aMockedContext: Context = mockk(relaxed = true)
        val anyLanguage = Locale.US.language
        val mapboxSpeechApi = MapboxSpeechApi(aMockedContext, anyLanguage)
        val anyAnnouncement: SpeechAnnouncement = mockk(relaxed = true)

        mapboxSpeechApi.clean(anyAnnouncement)

        verify(exactly = 1) {
            voiceAPI.clean(anyAnnouncement)
        }
    }

    @Test
    fun `destroy with no instructions`() {
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)

        sut.cancelPredownload()

        coVerify(exactly = 0) { voiceAPI.clean(any()) }
    }

    @Test
    fun `destroy removes all instructions`() = coroutineRule.runBlockingTest {
        val instruction1 = VoiceInstructions.builder().announcement("turn up and down").build()
        val instruction2 = VoiceInstructions.builder().announcement("dance and jump").build()
        val file1: File = mockk(relaxed = true)
        val file2: File = mockk(relaxed = true)
        val announcement1 = SpeechAnnouncement.Builder("turn up and down").file(file1).build()
        val announcement2 = SpeechAnnouncement.Builder("dance and jump").file(file2).build()
        coEvery { voiceAPI.retrieveVoiceFile(instruction1) } returns VoiceState.VoiceFile(file1)
        coEvery { voiceAPI.retrieveVoiceFile(instruction2) } returns VoiceState.VoiceFile(file2)
        val sut = MapboxSpeechApi(mockk(relaxed = true), Locale.US.language)
        sut.predownload(listOf(instruction1, instruction2))
        clearMocks(voiceAPI, answers = false)

        sut.cancelPredownload()

        coVerify(exactly = 1) {
            voiceAPI.clean(announcement1)
            voiceAPI.clean(announcement2)
        }

        sut.predownload(listOf(instruction1, instruction2))
        coVerify(exactly = 1) {
            voiceAPI.retrieveVoiceFile(instruction1)
            voiceAPI.retrieveVoiceFile(instruction2)
        }
    }
}
