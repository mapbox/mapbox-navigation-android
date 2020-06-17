package com.mapbox.navigation.navigator

import com.mapbox.base.common.logger.Logger
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.ResultCallback
import com.mapbox.common.UserAgentComponents
import com.mapbox.navigation.navigator.NavigationOkHttpService.Companion.GZIP
import com.mapbox.navigation.navigator.NavigationOkHttpService.Companion.HEADER_ENCODING
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Locale

class NavigationOkHttpServiceTest {
    private val logger: Logger = mockk(relaxUnitFun = true)
    private val httpClientBuilder: OkHttpClient.Builder = mockk(relaxUnitFun = true)
    private val httpClient: OkHttpClient = mockk(relaxUnitFun = true)
    private val httpDispatcher: Dispatcher = mockk(relaxUnitFun = true)

    private val nativeRequest = HttpRequest(
        HttpMethod.GET,
        "https://api.mapbox.com/xyz",
        hashMapOf("User-Agent" to "MapboxNavigationNative/X.Y.Z MapboxNavigationNative"),
        true,
        UserAgentComponents.Builder().build()
    )
    private val nativeCallback: HttpResponseCallback = mockk(relaxUnitFun = true)

    private lateinit var httpService: NavigationOkHttpService

    @Before
    fun setup() {
        every { httpClientBuilder.build() } returns httpClient
        every { httpClientBuilder.addInterceptor(any()) } returns httpClientBuilder
        every { httpClient.dispatcher() } returns httpDispatcher
        httpService = NavigationOkHttpService(httpClientBuilder, logger)
    }

    @Test
    fun setMaxRequestsPerHost() {
        httpService.setMaxRequestsPerHost(5)
        verify { httpDispatcher.maxRequestsPerHost = 5 }
    }

    @Test
    fun supportsKeepCompression() {
        assertTrue(httpService.supportsKeepCompression())
    }

    @Test
    fun request_callEnqueued() {
        val call: Call = mockk(relaxUnitFun = true)
        every { httpClient.newCall(any()) } returns call

        val id = httpService.request(nativeRequest, nativeCallback)

        verify { call.enqueue(any()) }
        assertTrue(id > 0)
    }

    @Test
    fun request_builderData() {
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns mockk(relaxUnitFun = true)

        httpService.request(nativeRequest, nativeCallback)

        val request = requestSlot.captured
        assertEquals(nativeRequest.url, request.url().toString())
        assertEquals(nativeRequest.url.toLowerCase(Locale.US), request.tag())
        val headers = Headers.Builder().addAll(nativeRequest.headers.toHeaders()).let {
            it.add(HEADER_ENCODING, GZIP)
            it.build()
        }
        assertEquals(headers, request.headers())
    }

    @Test
    fun request_canceled() {
        val call: Call = mockk(relaxUnitFun = true)
        every { httpClient.newCall(any()) } returns call

        val id = httpService.request(nativeRequest, nativeCallback)
        val cancelRequestCallback: ResultCallback = mockk(relaxUnitFun = true)
        httpService.cancelRequest(id, cancelRequestCallback)

        verify { call.cancel() }
        verify(exactly = 1) { cancelRequestCallback.run(false) }
    }

    @Test
    fun request_onFailure() {
        val callbackSlot = slot<NavigationOkHttpService.HttpCallback>()
        val call: Call = mockk(relaxUnitFun = true)
        every { call.enqueue(capture(callbackSlot)) } just Runs
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns call

        httpService.request(nativeRequest, nativeCallback)
        val exceptionMessage = "exception"
        val exception = IOException(exceptionMessage)
        val nativeResponseSlot = slot<HttpResponse>()
        callbackSlot.captured.onFailure(call, exception)

        verify { nativeCallback.run(capture(nativeResponseSlot)) }
        assertTrue(nativeResponseSlot.captured.result.isError)
    }

    @Test
    fun request_onResponse_200() {
        val callbackSlot = slot<NavigationOkHttpService.HttpCallback>()
        val call: Call = mockk(relaxUnitFun = true)
        every { call.enqueue(capture(callbackSlot)) } just Runs
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns call

        httpService.request(nativeRequest, nativeCallback)
        val body = mockk<ResponseBody>(relaxed = true)
        val byteArray = ByteArray(0)
        every { body.bytes() } returns byteArray
        val headers = Headers.Builder().add("header", "value").build()
        val response: Response = Response.Builder()
            .request(requestSlot.captured)
            .protocol(Protocol.HTTP_2)
            .message("message")
            .code(200)
            .headers(headers)
            .body(body)
            .build()
        callbackSlot.captured.onResponse(call, response)

        val nativeResponseSlot = slot<HttpResponse>()
        verify { nativeCallback.run(capture(nativeResponseSlot)) }
        assertTrue(nativeResponseSlot.captured.result.isValue)
        assertEquals(200, nativeResponseSlot.captured.result.value!!.code)
        assertArrayEquals(byteArray, nativeResponseSlot.captured.result.value!!.data)
        assertEquals(headers.toHashMap(), nativeResponseSlot.captured.result.value!!.headers)
    }

    @Test
    fun request_onResponse_noBody() {
        val callbackSlot = slot<NavigationOkHttpService.HttpCallback>()
        val call: Call = mockk(relaxUnitFun = true)
        every { call.enqueue(capture(callbackSlot)) } just Runs
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns call

        httpService.request(nativeRequest, nativeCallback)
        val response: Response = Response.Builder()
            .request(requestSlot.captured)
            .protocol(Protocol.HTTP_2)
            .message("message")
            .code(200)
            .body(null)
            .build()
        callbackSlot.captured.onResponse(call, response)

        val nativeResponseSlot = slot<HttpResponse>()
        verify { nativeCallback.run(capture(nativeResponseSlot)) }
        assertTrue(nativeResponseSlot.captured.result.isError)
    }

    @Test
    fun request_onResponse_notModified() {
        val callbackSlot = slot<NavigationOkHttpService.HttpCallback>()
        val call: Call = mockk(relaxUnitFun = true)
        every { call.enqueue(capture(callbackSlot)) } just Runs
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns call

        httpService.request(nativeRequest, nativeCallback)
        val response: Response = Response.Builder()
            .request(requestSlot.captured)
            .protocol(Protocol.HTTP_2)
            .message("message")
            .code(304)
            .body(mockk(relaxed = true))
            .build()
        callbackSlot.captured.onResponse(call, response)

        val nativeResponseSlot = slot<HttpResponse>()
        verify { nativeCallback.run(capture(nativeResponseSlot)) }
        assertTrue(nativeResponseSlot.captured.result.isValue)
        assertEquals(304, nativeResponseSlot.captured.result.value!!.code)
    }

    @Test
    fun request_cancelIgnoredAfterExecuted() {
        val callbackSlot = slot<NavigationOkHttpService.HttpCallback>()
        val call: Call = mockk(relaxUnitFun = true)
        every { call.enqueue(capture(callbackSlot)) } just Runs
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns call

        val id = httpService.request(nativeRequest, nativeCallback)
        callbackSlot.captured.onFailure(call, IOException("exceptionMessage"))
        val cancelRequestCallback: ResultCallback = mockk(relaxUnitFun = true)
        httpService.cancelRequest(id, cancelRequestCallback)

        verify(exactly = 0) { call.cancel() }
        verify(exactly = 1) { cancelRequestCallback.run(true) }
    }

    @Test
    fun request_resultIgnoredAfterCancel() {
        val callbackSlot = slot<NavigationOkHttpService.HttpCallback>()
        val call: Call = mockk(relaxUnitFun = true)
        every { call.enqueue(capture(callbackSlot)) } just Runs
        val requestSlot = slot<Request>()
        every { httpClient.newCall(capture(requestSlot)) } returns call

        val id = httpService.request(nativeRequest, nativeCallback)
        val cancelRequestCallback: ResultCallback = mockk(relaxUnitFun = true)
        httpService.cancelRequest(id, cancelRequestCallback)
        callbackSlot.captured.onResponse(call, mockk(relaxed = true))

        verify(exactly = 0) { nativeCallback.run(any()) }
        verify(exactly = 1) { cancelRequestCallback.run(false) }
    }
}
