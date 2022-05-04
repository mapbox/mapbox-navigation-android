package com.mapbox.navigation.ui.voice.api

import android.net.Uri
import androidx.core.net.toUri
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.testing.MockLoggerRule
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.ui.voice.testutils.Fixtures
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
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
internal class MapboxSpeechProviderTest {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    private lateinit var sut: MapboxSpeechProvider

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
        mockkObject(ThreadController)

        mockResourceLoader = mockk(relaxed = true)

        sut = MapboxSpeechProvider(
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
        val announcement = Fixtures.ssmlAnnouncement()
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

        sut.load(announcement)

        val loadRequestUri = requestCapture.captured.url.toUri()
        assertEquals("https", loadRequestUri.scheme)
        assertEquals("example.com", loadRequestUri.authority)
        assertEquals(
            "/voice/v1/speak/${UrlUtils.encodePathSegment(announcement.announcement)}",
            loadRequestUri.encodedPath
        )
        assertEquals(
            mapOf(
                "textType" to announcement.type,
                "language" to language,
                "access_token" to accessToken,
                "sku" to sku
            ),
            loadRequestUri.getQueryParams()
        )
    }

    @Test
    fun `load should return Expected with non empty audio BLOB on success`() =
        runBlocking {
            val announcement = Fixtures.textAnnouncement()
            val blob = byteArrayOf(12, 23, 34)
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                Fixtures.resourceData(blob),
                ResourceLoadStatus.AVAILABLE
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(announcement)

            assertEquals(blob, result.value)
        }

    @Test
    fun `load should return Expected with an Error on AVAILABLE loader response with empty BLOB`() =
        runBlocking {
            val expectedError = "No data available."
            val announcement = Fixtures.textAnnouncement()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                Fixtures.resourceData(byteArrayOf()),
                ResourceLoadStatus.AVAILABLE
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(announcement)

            assertEquals(expectedError, result.error!!.localizedMessage)
        }

    @Test
    fun `load should return Expected with an Error on UNAUTHORIZED loader response`() =
        runBlocking {
            val expectedError = "Your token cannot access this resource."
            val announcement = Fixtures.textAnnouncement()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                null,
                ResourceLoadStatus.UNAUTHORIZED
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(announcement)

            assertEquals(expectedError, result.error!!.localizedMessage)
        }

    @Test
    fun `load should return Expected with an Error on NOT_FOUND loader response`() =
        runBlocking {
            val expectedError = "Resource is missing."
            val announcement = Fixtures.textAnnouncement()
            val loadRequest = ResourceLoadRequest("https://some.url")
            val loadResult = Fixtures.resourceLoadResult(
                null,
                ResourceLoadStatus.NOT_FOUND
            )
            givenResourceLoaderResponse(loadRequest, ExpectedFactory.createValue(loadResult))

            val result = sut.load(announcement)

            assertEquals(expectedError, result.error!!.localizedMessage)
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
