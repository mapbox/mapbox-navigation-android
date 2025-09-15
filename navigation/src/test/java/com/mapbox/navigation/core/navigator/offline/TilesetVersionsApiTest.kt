package com.mapbox.navigation.core.navigator.offline

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.MapboxOptions
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.HttpException
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.navigator.offline.TilesetVersionsApi.RouteTileVersionsResponse
import com.mapbox.navigation.testing.BlockingSamCallback
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import javax.net.ssl.HttpsURLConnection

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TilesetVersionsApiTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var httpService: HttpServiceInterface
    private lateinit var tilesetVersionsApi: TilesetVersionsApi

    private val mockkSdkInformation = mockk<SdkInformation>()

    @Before
    fun setUp() {
        httpService = mockk(relaxed = true)
        tilesetVersionsApi = TilesetVersionsApi(httpService)

        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns "test_access_token"

        mockkObject(SdkInfoProvider)
        every { SdkInfoProvider.sdkInformation() } returns mockkSdkInformation
    }

    @After
    fun tearDown() {
        unmockkStatic(MapboxOptions::class)
        unmockkObject(SdkInfoProvider)
    }

    @Test
    fun `getRouteTileVersions should build correct URL`() {
        val baseUri = "https://api.mapbox.com"
        val dataset = "test-dataset"
        val profile = "driving"
        val expectedUrl = "https://api.mapbox.com/route-tiles/v2/test-dataset/driving/versions" +
            "?access_token=test_access_token"

        val requestSlot = slot<HttpRequest>()
        every {
            httpService.request(capture(requestSlot), any())
        } returns 1L

        tilesetVersionsApi.getRouteTileVersions(baseUri, dataset, profile) { }

        verify { httpService.request(capture(requestSlot), any()) }
        assertEquals(expectedUrl, requestSlot.captured.url)
        assertEquals(HttpMethod.GET, requestSlot.captured.method)
    }

    @Test
    fun `getRouteTileVersions should return success response on HTTP 200`() {
        val responseJson = """
            {
                "availableVersions": ["2025_07_25-12_01_23", "2025_07_24-11_30_15"],
                "blockedVersions": ["2025_07_20-10_00_00"]
            }
        """.trimIndent()

        mockSuccessfulHttpServiceResponseJson(responseJson)

        val result = tilesetVersionsApi.getRouteTileVersionsBlocking()
        assertTrue(result.isValue)

        assertEquals(
            listOf("2025_07_25-12_01_23", "2025_07_24-11_30_15"),
            result.value!!.availableVersions,
        )
        assertEquals(setOf("2025_07_20-10_00_00"), result.value!!.blockedVersions)
    }

    @Test
    fun `getRouteTileVersions should return error on HTTP 400`() {
        val errorMessage = "Bad Request"

        mockHttpServiceResponse(
            mockHttpResponse(
                400,
                errorMessage.toByteArray(),
            ),
        )

        val result = tilesetVersionsApi.getRouteTileVersionsBlocking()
        assertTrue(result.isError)

        assertTrue(result.error is HttpException)
        assertEquals(400, (result.error as HttpException).httpCode)
        assertEquals(errorMessage, result.error!!.message)
    }

    @Test
    fun `getRouteTileVersions should return error on network failure`() {
        val error = HttpRequestError(
            HttpRequestErrorType.CONNECTION_ERROR,
            "Network error",
        )

        mockHttpServiceResponse(mockHttpResponse(error))

        val result = tilesetVersionsApi.getRouteTileVersionsBlocking()
        assertTrue(result.isError)
        assertTrue(result.error is IOException)
        assertEquals(error.toString(), result.error?.message)
    }

    @Test
    fun `getRouteTileVersions should return error on JSON parsing failure`() {
        val incorrectJson = "{ invalid json }"
        mockSuccessfulHttpServiceResponseJson(incorrectJson)

        val result = tilesetVersionsApi.getRouteTileVersionsBlocking()
        assertTrue(result.isError)

        assertTrue(result.error is RuntimeException)
        assertEquals("Cannot parse route tile versions data", result.error?.message)
    }

    @Test
    fun `getRouteTileVersions should handle empty response`() {
        mockSuccessfulHttpServiceResponseJson(EMPTY_JSON)

        val result = tilesetVersionsApi.getRouteTileVersionsBlocking()
        assertTrue(result.isValue)

        assertEquals(emptyList<String>(), result.value?.availableVersions)
        assertEquals(emptySet<String>(), result.value?.blockedVersions)
    }

    @Test
    fun `getRouteTileVersions should handle null availableVersions`() {
        val responseJson = """
            {
                "blockedVersions": ["2025_07_20-10_00_00"]
            }
        """.trimIndent()

        mockSuccessfulHttpServiceResponseJson(responseJson)

        val result = tilesetVersionsApi.getRouteTileVersionsBlocking()
        assertTrue(result.isValue)

        assertEquals(emptyList<String>(), result.value?.availableVersions)
        assertEquals(setOf("2025_07_20-10_00_00"), result.value?.blockedVersions)
    }

    @Test
    fun `getRouteTileVersions should return cancellable`() {
        val httpResponse = mockHttpResponse(
            HttpsURLConnection.HTTP_OK,
            EMPTY_JSON.toByteArray(),
        )

        val requestId = 123L
        val httpServiceResponseSlot = slot<HttpResponseCallback>()
        every {
            httpService.request(any(), capture(httpServiceResponseSlot))
        } answers {
            httpServiceResponseSlot.captured.run(httpResponse)
            requestId
        }

        val cancelable = tilesetVersionsApi.getRouteTileVersions(
            "",
            "",
            "",
            mockk(relaxed = true),
        )
        cancelable.cancel()

        verify {
            httpService.cancelRequest(requestId, any())
        }
    }

    @Test
    fun `getRouteTileVersions should include SDK information in request`() {
        val httpResponse = mockHttpResponse(
            HttpsURLConnection.HTTP_OK,
            EMPTY_JSON.toByteArray(),
        )

        val httpServiceResponseSlot = slot<HttpResponseCallback>()
        val requestSlot = slot<HttpRequest>()
        every {
            httpService.request(capture(requestSlot), capture(httpServiceResponseSlot))
        } answers {
            httpServiceResponseSlot.captured.run(httpResponse)
            1L
        }

        tilesetVersionsApi.getRouteTileVersionsBlocking()

        verify { SdkInfoProvider.sdkInformation() }
        assertEquals(mockkSdkInformation, requestSlot.captured.sdkInformation)
    }

    private fun mockHttpServiceResponse(
        response: HttpResponse,
    ): CapturingSlot<HttpResponseCallback> {
        val httpServiceResponseSlot = slot<HttpResponseCallback>()
        every {
            httpService.request(any(), capture(httpServiceResponseSlot))
        } answers {
            httpServiceResponseSlot.captured.run(response)
            1L
        }
        return httpServiceResponseSlot
    }

    private companion object {

        const val EMPTY_JSON = "{}"

        fun TilesetVersionsApi.getRouteTileVersionsBlocking(
            baseUri: String = "https://api.mapbox.com",
            dataset: String = "test-dataset",
            profile: String = "driving",
        ): Expected<Throwable, RouteTileVersionsResponse> {
            val callback = BlockingSamCallback<Expected<Throwable, RouteTileVersionsResponse>>()
            getRouteTileVersions(
                baseUri = baseUri,
                dataset = dataset,
                profile = profile,
                callback = callback,
            )
            return callback.getResultBlocking()
        }
    }

    private fun mockSuccessfulHttpServiceResponseJson(
        json: String,
    ): CapturingSlot<HttpResponseCallback> {
        return mockHttpServiceResponse(
            mockHttpResponse(
                HttpsURLConnection.HTTP_OK,
                json.toByteArray(),
            ),
        )
    }

    private fun mockHttpResponse(
        result: Expected<HttpRequestError, HttpResponseData>,
    ): HttpResponse {
        return mockk<HttpResponse> {
            every { this@mockk.result } returns result
        }
    }

    private fun mockHttpResponse(
        code: Int,
        data: ByteArray,
    ): HttpResponse {
        return mockHttpResponse(ExpectedFactory.createValue(mockHttpResponseData(code, data)))
    }

    private fun mockHttpResponse(error: HttpRequestError): HttpResponse {
        return mockHttpResponse(ExpectedFactory.createError(error))
    }

    private fun mockHttpResponseData(
        code: Int,
        data: ByteArray,
    ): HttpResponseData {
        return mockk<HttpResponseData>(relaxed = true) {
            every { this@mockk.data } returns data
            every { this@mockk.code } returns code
        }
    }
}
