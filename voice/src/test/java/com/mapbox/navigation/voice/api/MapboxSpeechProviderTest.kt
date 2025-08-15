package com.mapbox.navigation.voice.api

import android.net.Uri
import androidx.core.net.toUri
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxOptions
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.UrlUtils
import com.mapbox.navigation.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.voice.options.VoiceGender
import com.mapbox.navigation.voice.testutils.Fixtures
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class MapboxSpeechProviderTest {

    private lateinit var sut: MapboxSpeechProvider

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()
    private var accessToken = "access_token"
    private val language = "en"
    private val apiOptions = MapboxSpeechApiOptions.Builder()
        .baseUri("https://example.com")
        .gender(VoiceGender.FEMALE)
        .build()
    private val sku = "SKU"

    private lateinit var mockResourceLoader: ResourceLoader
    private var stubSkuTokenProvider: UrlSkuTokenProvider = UrlSkuTokenProvider {
        URL("$it&sku=$sku")
    }

    @Before
    fun setUp() {
        mockkObject(InternalJobControlFactory)
        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns accessToken
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(mockk(), coroutineRule.createTestScope())
        mockkObject(ThreadController)

        mockResourceLoader = mockk(relaxed = true)

        sut = MapboxSpeechProvider(
            language = language,
            urlSkuTokenProvider = stubSkuTokenProvider,
            options = apiOptions,
            resourceLoader = mockResourceLoader,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
        unmockkStatic(MapboxOptions::class)
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
                ExpectedFactory.createValue(loadResult),
            )
            1L
        }

        sut.load(instructions)

        val loadRequest = requestCapture.captured
        val loadRequestUri = Uri.parse(loadRequest.url)
        assertEquals("https", loadRequestUri.scheme)
        assertEquals("example.com", loadRequestUri.authority)
        assertEquals(
            "/voice/v1/speak/${UrlUtils.encodePathSegment(instructions.ssmlAnnouncement()!!)}",
            loadRequestUri.encodedPath,
        )
        assertEquals(
            mapOf(
                "textType" to "ssml",
                "language" to language,
                "access_token" to accessToken,
                "gender" to apiOptions.gender,
                "sku" to sku,
            ),
            loadRequestUri.getQueryParams(),
        )
        assertEquals(
            ResourceLoadFlags.ACCEPT_EXPIRED,
            loadRequest.flags,
        )
        clearAllMocks(answers = false)

        // change access token
        every { MapboxOptions.accessToken } returns "new.token"
        val requestCapture2 = slot<ResourceLoadRequest>()
        every {
            mockResourceLoader.load(capture(requestCapture2), any())
        } answers {
            secondArg<ResourceLoadCallback>().onFinish(
                mockk(),
                mockk(),
            )
            1
        }

        sut.load(instructions)

        val loadRequest2 = requestCapture2.captured
        val loadRequestUri2 = loadRequest2.url.toUri()
        assertEquals(
            "new.token",
            loadRequestUri2.getQueryParams()["access_token"],
        )
    }

    @Test
    fun `load should allow network`() = runBlocking {
        val instructions = Fixtures.ssmlInstructions()
        val requestCapture = slot<ResourceLoadRequest>()
        val callbackCapture = slot<ResourceLoadCallback>()

        val loadResult = Fixtures.resourceLoadResult(null, ResourceLoadStatus.NOT_FOUND)
        every {
            mockResourceLoader.load(capture(requestCapture), capture(callbackCapture))
        } answers {
            callbackCapture.captured.onFinish(
                requestCapture.captured,
                ExpectedFactory.createValue(loadResult),
            )
            1L
        }

        sut.load(instructions)

        val loadRequest = requestCapture.captured
        assertEquals(
            NetworkRestriction.NONE,
            loadRequest.networkRestriction,
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
                ResourceLoadStatus.AVAILABLE,
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(instructions)

            assertTrue(blob.contentEquals(result.value!!.readBytes()))
        }

    @Test
    fun `load should return Expected with an Error on UNAUTHORIZED loader response`() =
        runBlocking {
            val expectedError = "Your token cannot access this resource."
            val instructions = Fixtures.textInstructions()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                null,
                ResourceLoadStatus.UNAUTHORIZED,
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
                ResourceLoadStatus.NOT_FOUND,
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(instructions)

            assertEquals(expectedError, result.error!!.localizedMessage)
        }

    @Test
    fun `request is cancelled when scope is cancelled`() = coroutineRule.runBlockingTest {
        val instructions = Fixtures.textInstructions()
        val job = launch { sut.load(instructions) }

        job.cancel()

        verify(exactly = 1) { mockResourceLoader.cancel(any()) }
    }

    private fun givenResourceLoaderResponse(
        request: ResourceLoadRequest,
        response: Expected<ResourceLoadError, ResourceLoadResult>,
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
