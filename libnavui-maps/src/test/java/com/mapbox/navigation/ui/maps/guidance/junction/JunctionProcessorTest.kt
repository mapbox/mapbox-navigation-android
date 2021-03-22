package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponseData
import com.mapbox.common.UAComponents
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class JunctionProcessorTest {

    @Test
    fun `process action junction availability result unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action junction availability result unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action junction availability result unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable no subType component`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable no image url`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSignboardSubType())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = JunctionResult.JunctionUnavailable
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result available`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf()
        bannerComponentsList.add(getComponentGuidanceViewTypeSignboardSubTypeImageUrl())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentsList
        val expected = JunctionResult.JunctionAvailable("https://abc.mapbox.com")
        val action = JunctionAction.CheckJunctionAvailability(bannerInstructions)

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionAvailable

        assertEquals(expected.junctionUrl, result.junctionUrl)
    }

    @Test
    fun `process action signboard process http request result http request`() {
        val mockHttpRequest = HttpRequest.Builder()
            .headers(hashMapOf(Pair("User-Agent", "MapboxJava/")))
            .body(byteArrayOf())
            .method(HttpMethod.GET)
            .url("https://abc.mapbox.com")
            .uaComponents(
                UAComponents.Builder()
                    .sdkIdentifierComponent("mapbox-navigation-ui-android")
                    .build()
            )
            .build()
        val action = JunctionAction.PrepareJunctionRequest("https://abc.mapbox.com")

        val result = JunctionProcessor.process(action) as JunctionResult.JunctionRequest

        assertEquals(mockHttpRequest.url, result.request.url)
        assertEquals(mockHttpRequest.body.isEmpty(), result.request.body.isEmpty())
        assertEquals(mockHttpRequest.method, result.request.method)
        assertEquals(mockHttpRequest.headers, result.request.headers)
        assertEquals(mockHttpRequest.uaComponents, result.request.uaComponents)
    }

    @Test
    fun `process action signboard process response result unauthorized access`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(401L, ByteArray(0))
        val response: Expected<HttpResponseData?, HttpRequestError?> =
            ExpectedFactory.createValue(mockHttpResponseData)
        val action = JunctionAction.ProcessJunctionResponse(response)
        val expected = JunctionResult.Junction.Failure(
            "Your token cannot access this " +
                "resource, contact support"
        )

        val result = JunctionProcessor.process(action) as JunctionResult.Junction.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result resource missing`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(404L, ByteArray(0))
        val response: Expected<HttpResponseData?, HttpRequestError?> =
            ExpectedFactory.createValue(mockHttpResponseData)
        val expected = JunctionResult.Junction.Failure("Resource is missing")
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.Junction.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result unknown error`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(500L, ByteArray(0))
        val response: Expected<HttpResponseData?, HttpRequestError?> =
            ExpectedFactory.createValue(mockHttpResponseData)
        val expected = JunctionResult.Junction.Failure("Unknown error")
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.Junction.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result no data`() {
        val response: Expected<HttpResponseData?, HttpRequestError?> = ExpectedFactory.createValue()
        val expected = JunctionResult.Junction.Empty
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.Junction.Empty

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard process response result failure`() {
        val response: Expected<HttpResponseData?, HttpRequestError?> =
            ExpectedFactory.createError(
                getMockHttpRequestError(
                    HttpRequestErrorType.CONNECTION_ERROR,
                    "Connection Error"
                )
            )
        val expected = JunctionResult.Junction.Failure("Connection Error")
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.Junction.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result success`() {
        val mockData = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(200L, mockData)
        val response: Expected<HttpResponseData?, HttpRequestError?> =
            ExpectedFactory.createValue(mockHttpResponseData)
        val expected = JunctionResult.Junction.Success(mockData)
        val action = JunctionAction.ProcessJunctionResponse(response)

        val result = JunctionProcessor.process(action) as JunctionResult.Junction.Success

        assertEquals(expected.data, result.data)
    }

    private fun getComponentGuidanceViewType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSignboardSubType(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.JCT)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSignboardSubTypeImageUrl(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.JCT)
            .text("some text")
            .imageUrl("https://abc.mapbox.com")
            .build()
    }

    private fun getMockHttpResponseData(code: Long, data: ByteArray): HttpResponseData {
        val httpResponseData: HttpResponseData = mockk()
        every { httpResponseData.code } returns code
        every { httpResponseData.data } returns data
        return httpResponseData
    }

    private fun getMockHttpRequestError(
        errorType: HttpRequestErrorType,
        msg: String
    ): HttpRequestError {
        val error: HttpRequestError = mockk()
        every { error.type } returns errorType
        every { error.message } returns msg
        return error
    }
}
