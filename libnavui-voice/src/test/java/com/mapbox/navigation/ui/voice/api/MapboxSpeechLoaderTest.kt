package com.mapbox.navigation.ui.voice.api

import android.net.Uri
import androidx.core.net.toUri
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.ui.voice.testutils.Fixtures
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class MapboxSpeechLoaderTest {

    private lateinit var sut: MapboxSpeechLoader

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private var accessToken = "access_token"
    private val language = "en"
    private val apiOptions = MapboxSpeechApiOptions.Builder()
        .baseUri("https://example.com")
        .build()
    private val sku = "SKU"

    private lateinit var mockResourceLoader: ResourceLoader
    private var stubSkuTokenProvider: UrlSkuTokenProvider = UrlSkuTokenProvider {
        URL("$it&sku=$sku")
    }

    @Before
    fun setUp() {
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(mockk(), coroutineRule.createTestScope())
        mockkObject(ThreadController)

        mockResourceLoader = mockk(relaxed = true)

        sut = MapboxSpeechLoader(
            accessToken = accessToken,
            language = language,
            urlSkuTokenProvider = stubSkuTokenProvider,
            options = apiOptions,
            resourceLoader = mockResourceLoader
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `load should use ResourceLoader to load audio data`() = runBlocking {
        val instructions = Fixtures.ssmlInstructions()
        val requestCapture = slot<ResourceLoadRequest>()
        val callbackCapture = slot<ResourceLoadCallback>()

        val loadResult = Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND)
        every {
            mockResourceLoader.load(capture(requestCapture), capture(callbackCapture))
        } answers {
            callbackCapture.captured.onFinish(
                requestCapture.captured,
                ExpectedFactory.createValue(loadResult)
            )
            1L
        }

        sut.load(instructions)

        val loadRequest = requestCapture.captured
        val loadRequestUri = loadRequest.url.toUri()
        assertEquals("https", loadRequestUri.scheme)
        assertEquals("example.com", loadRequestUri.authority)
        assertEquals(
            "/voice/v1/speak/${UrlUtils.encodePathSegment(instructions.ssmlAnnouncement()!!)}",
            loadRequestUri.encodedPath
        )
        assertEquals(
            mapOf(
                "textType" to "ssml",
                "language" to language,
                "access_token" to accessToken,
                "sku" to sku
            ),
            loadRequestUri.getQueryParams()
        )
        assertEquals(
            NetworkRestriction.DISALLOW_ALL,
            loadRequest.networkRestriction
        )
        assertEquals(
            ResourceLoadFlags.ACCEPT_EXPIRED,
            loadRequest.flags
        )
    }

    @Test
    fun `load should return Expected with non empty audio BLOB on success`() =
        runBlocking {
            val instructions = Fixtures.textInstructions()
            val blob = byteArrayOf(12, 23, 34)
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                Fixtures.resourceData(blob),
                ResourceLoadStatus.AVAILABLE
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(instructions)

            assertEquals(blob, result.value)
        }

    @Test
    fun `load should return Expected with an Error on AVAILABLE loader response with empty BLOB`() =
        runBlocking {
            val expectedError = "No data available."
            val instructions = Fixtures.textInstructions()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                Fixtures.resourceData(byteArrayOf()),
                ResourceLoadStatus.AVAILABLE
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(instructions)

            assertEquals(expectedError, result.error!!.localizedMessage)
        }

    @Test
    fun `load should return Expected with an Error on UNAUTHORIZED loader response`() =
        runBlocking {
            val expectedError = "Your token cannot access this resource."
            val instructions = Fixtures.textInstructions()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                null,
                ResourceLoadStatus.UNAUTHORIZED
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(instructions)

            assertEquals(expectedError, result.error!!.localizedMessage)
        }

    @Test
    fun `load should return Expected with an Error on NOT_FOUND loader response`() =
        runBlocking {
            val expectedError = "Resource is missing."
            val instructions = Fixtures.textInstructions()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                null,
                ResourceLoadStatus.NOT_FOUND
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(instructions)

            assertEquals(expectedError, result.error!!.localizedMessage)
        }

    @Test
    fun `trigger with empty list`() = coroutineRule.runBlockingTest {
        sut.trigger(emptyList())

        verify(exactly = 0) { mockResourceLoader.load(any(), any()) }
    }

    @Test
    fun `trigger with invalid instruction`() = coroutineRule.runBlockingTest {
        sut.trigger(listOf(VoiceInstructions.builder().build()))

        verify(exactly = 0) { mockResourceLoader.load(any(), any()) }
    }

    @Test
    fun `trigger with new instruction`() = coroutineRule.runBlockingTest {
        val announcement = "turn up and down"
        sut.trigger(
            listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement).build())
        )

        verify(exactly = 1) {
            mockResourceLoader.load(
                match { it.url.contains(UrlUtils.encodePathSegment(announcement)) },
                any()
            )
        }
    }

    @Test
    fun `trigger with same instruction of different type`() = coroutineRule.runBlockingTest {
        val announcement = "turn up and down"
        sut.trigger(
            listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement).build())
        )
        captureRequestCallback().onFinish(mockk(), ExpectedFactory.createValue(mockk()))
        clearMocks(mockResourceLoader, answers = false)

        sut.trigger(
            listOf(VoiceInstructions.builder().announcement(announcement).build())
        )
        verify(exactly = 1) { mockResourceLoader.load(any(), any()) }
    }

    @Test
    fun `trigger with existing instruction`() = coroutineRule.runBlockingTest {
        val announcement = "turn up and down"
        sut.trigger(
            listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement).build())
        )
        captureRequestCallback().onFinish(mockk(), ExpectedFactory.createValue(mockk()))
        clearMocks(mockResourceLoader, answers = false)

        sut.trigger(
            listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement).build())
        )
        verify(exactly = 0) { mockResourceLoader.load(any(), any()) }
    }

    @Test
    fun `trigger with multiple new instructions`() = coroutineRule.runBlockingTest {
        val announcement1 = "turn up and down"
        val announcement2 = "dance and jump"
        sut.trigger(
            listOf(
                VoiceInstructions.builder().ssmlAnnouncement(announcement1).build(),
                VoiceInstructions.builder().announcement(announcement2).build(),
            )
        )

        verify(exactly = 1) {
            mockResourceLoader.load(
                match { it.url.contains(UrlUtils.encodePathSegment(announcement1)) },
                any()
            )
            mockResourceLoader.load(
                match { it.url.contains(UrlUtils.encodePathSegment(announcement2)) },
                any()
            )
        }
    }

    @Test
    fun `failed download does not save instruction`() = coroutineRule.runBlockingTest {
        val announcement = "turn up and down"
        sut.trigger(
            listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement).build())
        )
        captureRequestCallback().onFinish(mockk(), ExpectedFactory.createError(mockk()))
        clearMocks(mockResourceLoader, answers = false)

        sut.trigger(
            listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement).build())
        )

        verify(exactly = 1) { mockResourceLoader.load(any(), any()) }
    }

    @Test
    fun `cancel with no active downloads`() = coroutineRule.runBlockingTest {
        sut.cancel()

        verify(exactly = 0) { mockResourceLoader.cancel(any()) }
    }

    @Test
    fun `cancel with active downloads`() = coroutineRule.runBlockingTest {
        val id1 = 10L
        val id2 = 15L
        val id3 = 20L
        every { mockResourceLoader.load(any(), any()) } returnsMany listOf(id1, id2, id3)
        val announcement1 = "turn up and down"
        val announcement2 = "dance and jump"
        val announcement3 = "stop and watch"
        sut.trigger(
            listOf(
                VoiceInstructions.builder().ssmlAnnouncement(announcement1).build(),
                VoiceInstructions.builder().announcement(announcement2).build(),
            )
        )
        sut.trigger(listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement3).build()))

        sut.cancel()

        verify(exactly = 1) {
            mockResourceLoader.cancel(id1)
            mockResourceLoader.cancel(id2)
            mockResourceLoader.cancel(id3)
        }
    }

    @Test
    fun `cancel with finished downloads`() = coroutineRule.runBlockingTest {
        val id1 = 10L
        val id2 = 15L
        every { mockResourceLoader.load(any(), any()) } returnsMany listOf(id1, id2)
        val announcement1 = "turn up and down"
        val announcement2 = "dance and jump"
        sut.trigger(listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement1).build()))
        val callback = captureRequestCallback()
        sut.trigger(listOf(VoiceInstructions.builder().ssmlAnnouncement(announcement2).build()))
        callback.onFinish(mockk(), ExpectedFactory.createError(mockk()))

        sut.cancel()

        verify(exactly = 1) {
            mockResourceLoader.cancel(id2)
        }
        verify(exactly = 0) {
            mockResourceLoader.cancel(id1)
        }
    }

    private fun captureRequestCallback(): ResourceLoadCallback {
        val slot = CapturingSlot<ResourceLoadCallback>()
        verify { mockResourceLoader.load(any(), capture(slot)) }
        return slot.captured
    }

    private fun givenResourceLoaderResponse(
        request: ResourceLoadRequest,
        response: Expected<ResourceLoadError, ResourceLoadResult>
    ) {
        val loadCallbackSlot = slot<ResourceLoadCallback>()
        every { mockResourceLoader.load(any(), capture(loadCallbackSlot)) } answers {
            loadCallbackSlot.captured.onFinish(request, response)
            0L
        }
    }

    private fun Uri.getQueryParams() =
        queryParameterNames.fold(mutableMapOf<String, String?>()) { acc, key ->
            acc[key] = getQueryParameter(key)
            acc
        }
}
