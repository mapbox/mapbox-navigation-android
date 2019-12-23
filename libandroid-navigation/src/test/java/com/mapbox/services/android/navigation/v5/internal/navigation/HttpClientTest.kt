package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.navigator.HttpCode
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HttpClientTest {

    companion object {
        private const val TEST_URL = "https://test.url.com/"
        private const val USER_AGENT = "testAgent"
        private const val SUCCESS_BODY = "Success test message"
        private const val FAILURE_BODY = "Error test message"
        private const val SUCCESS_CODE = 200
        private const val FAILURE_CODE = 401
        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ENCODING = "Accept-Encoding"
        private const val GZIP = "gzip"
    }

    private val httpClient = HttpClient(USER_AGENT)
    private val mockServer = MockWebServer()

    @Before
    fun setUp() {
        mockServer.start()
        mockServer.url(TEST_URL)
    }

    @After
    fun cleanUp() {
        mockServer.shutdown()
    }

    @Test
    fun `check success network response`() {
        val mockResponse = buildResponse(SUCCESS_CODE, SUCCESS_BODY)
        mockServer.enqueue(mockResponse)
        val response = httpClient.executeMockRequest()

        assertEquals(HttpCode.SUCCESS.name, response.code.name)
        assertEquals(SUCCESS_BODY.toByteArray().toList(), response.bytes)
    }

    @Test
    fun `check failure network response`() {
        val mockResponse = buildResponse(FAILURE_CODE, FAILURE_BODY)
        mockServer.enqueue(mockResponse)
        val response = httpClient.executeMockRequest()

        assertEquals(HttpCode.FAILURE.name, response.code.name)
        assertEquals(FAILURE_BODY.toByteArray().toList(), response.bytes)
    }

    @Test
    fun `check UserAgent header`() {
        mockServer.enqueue(MockResponse())

        val interceptor = Interceptor { chain ->
            val networkRequest = chain.request()
            val networkResponse = chain.proceed(networkRequest)

            assertEquals(USER_AGENT, networkRequest.header(HEADER_USER_AGENT))

            networkResponse
        }

        val httpClient = HttpClient(
            userAgent = USER_AGENT,
            clientBuilder = OkHttpClient.Builder().apply { networkInterceptors().add(interceptor) })

        httpClient.executeMockRequest()
    }

    @Test
    fun `check AcceptEncoding header`() {
        mockServer.enqueue(MockResponse())

        val interceptor = Interceptor { chain ->
            val networkRequest = chain.request()
            val networkResponse = chain.proceed(networkRequest)

            assertEquals(GZIP, networkRequest.header(HEADER_ENCODING))

            networkResponse
        }

        val httpClientWithGzip = HttpClient(
            userAgent = USER_AGENT,
            acceptGzipEncoding = true,
            clientBuilder = OkHttpClient.Builder().apply { networkInterceptors().add(interceptor) })

        httpClientWithGzip.executeMockRequest()
    }

    private fun buildResponse(code: Int, body: String) =
        MockResponse().apply {
            setResponseCode(code)
            setBody(body)
        }

    private fun HttpClient.executeMockRequest() = this.get(mockServer.url("/").toString())
}
