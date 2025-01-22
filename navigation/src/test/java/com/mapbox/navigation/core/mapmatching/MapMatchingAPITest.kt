@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.mapmatching

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.SdkInformation
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.fakes.TestUrlSkuTokenProvider
import com.mapbox.navigation.testing.http.CommonHttpRequestHandler
import com.mapbox.navigation.testing.http.createTestHttpService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

typealias MapMatchingHttpRequestHandler = (
    request: HttpRequest,
) -> TestHttpResponses.HttpServiceTestResponse

class MapMatchingAPITest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val nativeRouteParsingRule = NativeRouteParserRule()

    @Test
    fun `successful request response`() {
        val recordedResponse = TestHttpResponses.example
        val testAccessToken = "test-access-token"
        val testSdkInformation = SdkInformation("test-name", "test-version", "test-package")
        var httpRequest: HttpRequest? = null
        val testSkuValue = "test-sku-123"
        val api = createMapMatchingAPI(
            requestHandler = returnImmediately {
                httpRequest = it
                recordedResponse
            },
            accessTokenProvider = { testAccessToken },
            sdkInformation = testSdkInformation,
            skuTokenProvider = TestUrlSkuTokenProvider(testSkuValue),
        )
        val callback = TestMapMatchingAPICallback()

        api.requestMapMatching(
            recordedResponse.request,
            callback,
        )

        // request
        val requestUrl = httpRequest!!.url.toHttpUrl()
        assertEquals(
            requestUrl.queryParameter("access_token"),
            testAccessToken,
        )
        assertEquals(
            requestUrl.queryParameter("sku"),
            testSkuValue,
        )
        assertEquals(testSdkInformation, httpRequest!!.sdkInformation)

        // response
        val mapMatchingResult = callback.getSuccessfulResultOrThrowException()
        assertEquals(
            mapMatchingResult.matches.map { it.navigationRoute },
            mapMatchingResult.navigationRoutes,
        )
        assertEquals(1, mapMatchingResult.matches.size)
        assertEquals(
            0.927499,
            mapMatchingResult.matches.first().confidence,
            0.00000001,
        )
    }

    @Test
    fun `no routes found response`() {
        val recordedResponse = TestHttpResponses.noSegmentsFound
        val api = createMapMatchingAPI(
            requestHandler = returnImmediately {
                recordedResponse
            },
        )
        val callback = TestMapMatchingAPICallback()

        api.requestMapMatching(
            recordedResponse.request,
            callback,
        )

        // response
        val mapMatchingResult = callback.getFailureOrThrowException()
    }

    @Test
    fun `cancel request`() {
        var recordedCallback: HttpResponseCallback? = null
        var recordedRequest: HttpRequest? = null
        val api = createMapMatchingAPI(
            requestHandler = { id, request, callback ->
                recordedRequest = request
                recordedCallback = callback
            },
        )
        val callback = TestMapMatchingAPICallback()

        val requestId = api.requestMapMatching(
            TestHttpResponses.example.request,
            callback,
        )
        api.cancel(requestId)
        recordedCallback!!.run(
            HttpResponse(
                requestId,
                recordedRequest!!,
                TestHttpResponses.example.result,
            ),
        )

        callback.assertCancelled()
    }

    @Test
    fun `cancel request after successful response`() {
        val recordedResponse = TestHttpResponses.example
        val api = createMapMatchingAPI(
            requestHandler = returnImmediately {
                recordedResponse
            },
        )
        val callback = TestMapMatchingAPICallback()
        val requestId = api.requestMapMatching(
            recordedResponse.request,
            callback,
        )
        callback.getSuccessfulResultOrThrowException()

        api.cancel(requestId)

        callback.getSuccessfulResultOrThrowException()
    }

    @Test
    fun `cancel all after successful response`() {
        val recordedResponse = TestHttpResponses.example
        val api = createMapMatchingAPI(
            requestHandler = returnImmediately {
                recordedResponse
            },
        )
        val callback = TestMapMatchingAPICallback()
        api.requestMapMatching(
            recordedResponse.request,
            callback,
        )
        callback.getSuccessfulResultOrThrowException()

        api.cancelAll()

        callback.getSuccessfulResultOrThrowException()
    }

    @Test
    fun `cancel request while serialisation in progress`() {
        val testResponse = TestHttpResponses.example
        val serialisationDispatcher = StandardTestDispatcher()
        val api = createMapMatchingAPI(
            requestHandler = returnImmediately {
                testResponse
            },
            serialisationDispatcher = serialisationDispatcher,
        )

        val callback = TestMapMatchingAPICallback()
        val requestId = api.requestMapMatching(
            testResponse.request,
            callback,
        )
        api.cancel(requestId)
        serialisationDispatcher.scheduler.runCurrent()

        callback.assertCancelled()
    }

    @Test
    fun `destroy while request in progress`() {
        val api = createMapMatchingAPI(
            requestHandler = { id, request, callback ->
            },
        )
        val callback1 = TestMapMatchingAPICallback()
        val callback2 = TestMapMatchingAPICallback()
        val callback3 = TestMapMatchingAPICallback()

        api.requestMapMatching(
            TestHttpResponses.example.request,
            callback1,
        )
        api.requestMapMatching(
            TestHttpResponses.example.request,
            callback2,
        )
        api.requestMapMatching(
            TestHttpResponses.example.request,
            callback3,
        )
        api.cancelAll()

        callback1.assertCancelled()
        callback2.assertCancelled()
        callback3.assertCancelled()
    }

    @Test
    fun `wrong response body`() {
        val api = createMapMatchingAPI(
            requestHandler = { id, request, callback ->
                callback.run(
                    HttpResponse(
                        id,
                        request,
                        ExpectedFactory.createValue(
                            HttpResponseData(
                                HashMap(),
                                200,
                                "wrong".toByteArray(),
                            ),
                        ),
                    ),
                )
            },
        )

        val callback = TestMapMatchingAPICallback()
        api.requestMapMatching(
            TestHttpResponses.example.request,
            callback,
        )

        callback.getFailureOrThrowException()
    }

    @Test
    fun `waypoints are available on map matching route`() {
        val recordedResponse = TestHttpResponses.example

        val api = createMapMatchingAPI(
            requestHandler = returnImmediately {
                recordedResponse
            },
        )
        val callback = TestMapMatchingAPICallback()

        api.requestMapMatching(
            recordedResponse.request,
            callback,
        )

        val mapMatchingResult = callback.getSuccessfulResultOrThrowException()
        val waypoints = mapMatchingResult.navigationRoutes.first().waypoints!!
        assertEquals(
            listOf(
                Point.fromLngLat(
                    -117.172834,
                    32.712036,
                ),
                Point.fromLngLat(
                    -117.173344,
                    32.712546,
                ),
            ),
            waypoints.map { it.location() },
        )
        assertEquals(
            listOf(
                "Test 1",
                "Test 2",
            ),
            waypoints.map { it.name() },
        )
    }
}

internal fun createMapMatchingAPI(
    requestHandler: CommonHttpRequestHandler,
    accessTokenProvider: () -> String = { "testToken" },
    serialisationDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    sdkInformation: SdkInformation = SdkInfoProvider.sdkInformation(),
    skuTokenProvider: UrlSkuTokenProvider = TestUrlSkuTokenProvider(),
) = MapMatchingAPI(
    serialisationDispatcher = serialisationDispatcher,
    mainDispatcher = UnconfinedTestDispatcher(),
    httpServiceFactory = {
        createTestHttpService(
            requestHandler = requestHandler,
        )
    },
    sdkInformation = sdkInformation,
    getCurrentAccessToken = accessTokenProvider,
    skuTokenProvider,
)

fun returnImmediately(handler: MapMatchingHttpRequestHandler): CommonHttpRequestHandler {
    return { requestId, request, callback ->
        val response = handler(request)
        callback.run(
            HttpResponse(
                requestId,
                request,
                response.result,
            ),
        )
    }
}

class TestMapMatchingAPICallback : MapMatchingAPICallback {

    private var successfulResult: MapMatchingSuccessfulResult? = null
    private var failureResult: MapMatchingFailure? = null
    private var wasCancelled = false

    fun getSuccessfulResultOrThrowException(): MapMatchingSuccessfulResult {
        assertFalse("request has been cancelled", wasCancelled)
        assertNull("request has been failed", failureResult)
        assertNotNull("hasn't received successful result", successfulResult)
        return successfulResult!!
    }

    fun getFailureOrThrowException(): MapMatchingFailure {
        assertFalse("request has been cancelled", wasCancelled)
        assertNull("request has been successfully completed", successfulResult)
        assertNotNull("hasn't received failure result", failureResult)
        return failureResult!!
    }

    fun assertCancelled() {
        assertNull(successfulResult)
        assertNull(failureResult)
        assertTrue(wasCancelled)
    }

    override fun success(result: MapMatchingSuccessfulResult) {
        successfulResult = result
    }

    override fun failure(failure: MapMatchingFailure) {
        failureResult = failure
    }

    override fun onCancel() {
        wasCancelled = true
    }
}

object TestHttpResponses {

    val example = HttpServiceTestResponse(
        MapMatchingOptions.Builder()
            .coordinates(
                "-117.17282,32.71204" +
                    ";-117.17288,32.71225" +
                    ";-117.17293,32.71244" +
                    ";-117.17292,32.71256" +
                    ";-117.17298,32.712603" +
                    ";-117.17314,32.71259" +
                    ";-117.17334,32.71254",
            )
            .waypoints(listOf(0, 6))
            .build(),
        ExpectedFactory.createValue(
            HttpResponseData(
                HashMap(),
                200,
                resourceAsString(
                    "map_matching_example_response.json",
                ).toByteArray(),
            ),
        ),
    )

    val noSegmentsFound = HttpServiceTestResponse(
        MapMatchingOptions.Builder()
            .coordinates(
                "-71.443158%2C39.613564;-71.448504%2C39.596188",
            )
            .build(),
        ExpectedFactory.createValue(
            HttpResponseData(
                HashMap(),
                200,
                resourceAsString(
                    "map_mathing_no_segments_found_response.json",
                ).toByteArray(),
            ),
        ),
    )

    data class HttpServiceTestResponse(
        val request: MapMatchingOptions,
        val result: Expected<HttpRequestError, HttpResponseData>,
    )

    private fun resourceAsString(
        name: String,
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(
            "com.mapbox.navigation.core.mapmatching/$name",
        )
        return IOUtils.toString(inputStream, "UTF-8")
    }
}
