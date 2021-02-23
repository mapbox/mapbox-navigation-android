package com.mapbox.navigation.ui.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.api.speech.v1.MapboxSpeech
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class VoiceProcessorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `process PrepareVoiceRequest returns VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk(relaxed = true)
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest)
        }

    @Test
    fun `process PrepareVoiceRequest SSML announcement returns Success VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            val aSsmlAnnouncement = """
            <speak>
                <amazon:effect name="drc">
                    <prosody rate="1.08">Turn right onto Frederick Road, Maryland 3 55.</prosody>
                </amazon:effect>
            </speak>
            """.trimIndent()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns aSsmlAnnouncement
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Success)
            val actualSpeech: MapboxSpeech =
                (actual as VoiceResult.VoiceRequest.Success).requestBuilder
                    .accessToken("pk.123")
                    .build()
            val expectedSpeech: MapboxSpeech = MapboxSpeech.builder()
                .accessToken("pk.123")
                .instruction(aSsmlAnnouncement)
                .textType("ssml")
                .build()
            assertEquals(expectedSpeech, actualSpeech)
        }

    @Test
    fun `process PrepareVoiceRequest text announcement returns Success VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns null
            val anAnnouncement = "Turn right onto Frederick Road, Maryland 3 55."
            every {
                mockedVoiceInstructions.announcement()
            } returns anAnnouncement
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Success)
            val actualSpeech: MapboxSpeech =
                (actual as VoiceResult.VoiceRequest.Success).requestBuilder
                    .accessToken("pk.123")
                    .build()
            val expectedSpeech: MapboxSpeech = MapboxSpeech.builder()
                .accessToken("pk.123")
                .instruction(anAnnouncement)
                .textType("text")
                .build()
            assertEquals(expectedSpeech, actualSpeech)
        }

    @Test
    fun `process PrepareVoiceRequest null announcements returns Failure VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns null
            every {
                mockedVoiceInstructions.announcement()
            } returns null
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Failure)
            assertEquals(
                "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
                (actual as VoiceResult.VoiceRequest.Failure).error
            )
        }

    @Test
    fun `process PrepareVoiceRequest empty ssmlAnnouncement returns Failure VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns ""
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Failure)
            assertEquals(
                "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
                (actual as VoiceResult.VoiceRequest.Failure).error
            )
        }

    @Test
    fun `process PrepareVoiceRequest blank ssmlAnnouncement returns Failure VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns "    "
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Failure)
            assertEquals(
                "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
                (actual as VoiceResult.VoiceRequest.Failure).error
            )
        }

    @Test
    fun `process PrepareVoiceRequest empty announcement returns Failure VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns null
            every {
                mockedVoiceInstructions.announcement()
            } returns ""
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Failure)
            assertEquals(
                "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
                (actual as VoiceResult.VoiceRequest.Failure).error
            )
        }

    @Test
    fun `process PrepareVoiceRequest blank announcement returns Failure VoiceRequest result`() =
        coroutineRule.runBlockingTest {
            val mockedVoiceInstructions: VoiceInstructions = mockk()
            every {
                mockedVoiceInstructions.ssmlAnnouncement()
            } returns null
            every {
                mockedVoiceInstructions.announcement()
            } returns "      "
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.PrepareVoiceRequest(mockedVoiceInstructions)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceRequest.Failure)
            assertEquals(
                "VoiceInstructions announcement / ssmlAnnouncement can't be null or blank",
                (actual as VoiceResult.VoiceRequest.Failure).error
            )
        }

    @Test
    fun `process ProcessVoiceResponse returns VoiceResponse result`() =
        coroutineRule.runBlockingTest {
            val mockedResponse: Response<ResponseBody> = mockk(relaxed = true)
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.ProcessVoiceResponse(mockedResponse)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceResponse)
        }

    @Test
    fun `process ProcessVoiceResponse returns Success VoiceResponse result`() =
        coroutineRule.runBlockingTest {
            val mockedResponse: Response<ResponseBody> = mockk(relaxed = true)
            every { mockedResponse.isSuccessful } returns true
            val mockedResponseBody: ResponseBody = mockk()
            every { mockedResponse.body() } returns mockedResponseBody
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.ProcessVoiceResponse(mockedResponse)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceResponse.Success)
            assertEquals(mockedResponseBody, (actual as VoiceResult.VoiceResponse.Success).data)
        }

    @Test
    fun `process ProcessVoiceResponse successful null body returns Failure VoiceResponse result`() =
        coroutineRule.runBlockingTest {
            val mockedResponse: Response<ResponseBody> = mockk(relaxed = true)
            every { mockedResponse.isSuccessful } returns true
            every { mockedResponse.code() } returns 204
            every { mockedResponse.body() } returns null
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.ProcessVoiceResponse(mockedResponse)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceResponse.Failure)
            assertEquals(
                204,
                (actual as VoiceResult.VoiceResponse.Failure).responseCode
            )
            assertEquals(
                "No data available",
                actual.error
            )
        }

    @Test
    fun `process ProcessVoiceResponse not successful returns Failure VoiceResponse result`() =
        coroutineRule.runBlockingTest {
            val mockedResponse: Response<ResponseBody> = mockk(relaxed = true)
            every { mockedResponse.isSuccessful } returns false
            every { mockedResponse.code() } returns 403
            val responseBody: ResponseBody = mockk()
            every { responseBody.string() } returns "Forbidden"
            every { mockedResponse.errorBody() } returns responseBody
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.ProcessVoiceResponse(mockedResponse)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceResponse.Failure)
            assertEquals(
                403,
                (actual as VoiceResult.VoiceResponse.Failure).responseCode
            )
            assertEquals(
                "Forbidden",
                actual.error
            )
        }

    @Test
    fun `process ProcessVoiceResponse not successful null returns Failure VoiceResponse result`() =
        coroutineRule.runBlockingTest {
            val mockedResponse: Response<ResponseBody> = mockk(relaxed = true)
            every { mockedResponse.isSuccessful } returns false
            every { mockedResponse.code() } returns 500
            every { mockedResponse.errorBody() } returns null
            val prepareVoiceRequest: VoiceAction =
                VoiceAction.ProcessVoiceResponse(mockedResponse)

            val actual = VoiceProcessor.process(prepareVoiceRequest)

            assertTrue(actual is VoiceResult.VoiceResponse.Failure)
            assertEquals(
                500,
                (actual as VoiceResult.VoiceResponse.Failure).responseCode
            )
            assertEquals(
                "Unknown",
                actual.error
            )
        }
}
