package com.mapbox.navigation.ui.maps.guidance.signboard

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponseData
import com.mapbox.common.UAComponents
import com.mapbox.navigation.ui.maps.guidance.signboard.api.SvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class SignboardProcessorTest {

    @Test
    fun `process action signboard availability result unavailable no banner view`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable no banner components`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard availability result unavailable empty component list`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponents: MutableList<BannerComponents> = mutableListOf()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponents
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

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
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

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
        val expected = SignboardResult.SignboardUnavailable
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action)

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
        val expected = SignboardResult.SignboardAvailable("https://abc.mapbox.com")
        val action = SignboardAction.CheckSignboardAvailability(bannerInstructions)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardAvailable

        assertEquals(expected.signboardUrl, result.signboardUrl)
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
        val action = SignboardAction.PrepareSignboardRequest("https://abc.mapbox.com")

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardRequest

        assertEquals(mockHttpRequest.url, result.request.url)
        assertEquals(mockHttpRequest.body?.isEmpty(), result.request.body?.isEmpty())
        assertEquals(mockHttpRequest.method, result.request.method)
        assertEquals(mockHttpRequest.headers, result.request.headers)
        assertEquals(mockHttpRequest.uaComponents, result.request.uaComponents)
    }

    @Test
    fun `process action signboard process response result unauthorized access`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(401L, ByteArray(0))
        val response: Expected<HttpRequestError, HttpResponseData> =
            createValue(mockHttpResponseData)
        val action = SignboardAction.ProcessSignboardResponse(response)
        val expected = SignboardResult.SignboardSvg.Failure(
            "Your token cannot access this " +
                "resource, contact support"
        )

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result resource missing`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(404L, ByteArray(0))
        val response: Expected<HttpRequestError, HttpResponseData> =
            createValue(mockHttpResponseData)
        val expected = SignboardResult.SignboardSvg.Failure("Resource is missing")
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result unknown error`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(500L, ByteArray(0))
        val response: Expected<HttpRequestError, HttpResponseData> =
            createValue(mockHttpResponseData)
        val expected = SignboardResult.SignboardSvg.Failure("Unknown error")
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result no data`() {
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(200L, ByteArray(0))
        val response: Expected<HttpRequestError, HttpResponseData> =
            createValue(mockHttpResponseData)
        val expected = SignboardResult.SignboardSvg.Empty
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Empty

        assertEquals(expected, result)
    }

    @Test
    fun `process action signboard process response result failure`() {
        val response: Expected<HttpRequestError, HttpResponseData> =
            createError(
                getMockHttpRequestError(
                    HttpRequestErrorType.CONNECTION_ERROR,
                    "Connection Error"
                )
            )
        val expected = SignboardResult.SignboardSvg.Failure("Connection Error")
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Failure

        assertEquals(expected.error, result.error)
    }

    @Test
    fun `process action signboard process response result success`() {
        val mockData = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val mockHttpResponseData: HttpResponseData = getMockHttpResponseData(200L, mockData)
        val response: Expected<HttpRequestError, HttpResponseData> =
            createValue(mockHttpResponseData)
        val expected = SignboardResult.SignboardSvg.Success(mockData)
        val action = SignboardAction.ProcessSignboardResponse(response)

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardSvg.Success

        assertEquals(expected.data, result.data)
    }

    @Test
    fun `process action signboard process bytearray to bitmap failure`() {
        val mockParser = mockk<SvgToBitmapParser>()
        val mockData = byteArrayOf(12, -12, 23, 65, -56, 74, 88, 90, -92, -11)
        val mockOptions = mockk<MapboxSignboardOptions>()
        val action = SignboardAction.ParseSvgToBitmap(mockData, mockParser, mockOptions)
        every {
            mockParser.parse(any(), any())
        } returns ExpectedFactory.createError("whatever")

        val result = SignboardProcessor.process(action) as SignboardResult.SignboardBitmap.Failure

        assertEquals("whatever", result.message)
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
            .subType(BannerComponents.SIGNBOARD)
            .text("some text")
            .imageUrl(null)
            .build()
    }

    private fun getComponentGuidanceViewTypeSignboardSubTypeImageUrl(): BannerComponents {
        return BannerComponents.builder()
            .type(BannerComponents.GUIDANCE_VIEW)
            .subType(BannerComponents.SIGNBOARD)
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
