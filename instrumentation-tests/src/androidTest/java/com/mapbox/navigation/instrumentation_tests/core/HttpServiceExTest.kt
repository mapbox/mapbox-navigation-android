package com.mapbox.navigation.instrumentation_tests.core

import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpServiceFactory
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.utils.internal.request
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class HttpServiceExTest {

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    @Test
    fun successfulResponse() = sdkTest {
        mockWebServerRule.requestHandlers.add {
            MockResponse().setBody("ok").setResponseCode(200)
        }

        val httpClient = HttpServiceFactory.getInstance()

        val response = httpClient.request(
            HttpRequest.Builder()
                .headers(HashMap())
                .sdkInformation(SdkInfoProvider.sdkInformation())
                .url(mockWebServerRule.baseUrl)
                .build(),
        )
        assertNull(response.error)
        assertEquals(200, response.value?.code)
        assertEquals("ok", String(response.value?.data!!))
    }

    @Test
    fun httpError() = sdkTest {
        mockWebServerRule.requestHandlers.add {
            MockResponse().setBody("not authorized").setResponseCode(401)
        }

        val httpClient = HttpServiceFactory.getInstance()

        val response = httpClient.request(
            HttpRequest.Builder()
                .headers(HashMap())
                .sdkInformation(SdkInfoProvider.sdkInformation())
                .url(mockWebServerRule.baseUrl)
                .build(),
        )
        assertNull(response.error)
        assertEquals(401, response.value?.code)
        assertEquals("not authorized", String(response.value?.data!!))
    }

    @Test
    fun connectionError() = sdkTest {
        val httpClient = HttpServiceFactory.getInstance()
        mockWebServerRule.withoutWebServer {
            val response = httpClient.request(
                HttpRequest.Builder()
                    .headers(HashMap())
                    .sdkInformation(SdkInfoProvider.sdkInformation())
                    .url(mockWebServerRule.baseUrl)
                    .build(),
            )
            assertNull(response.value)
            assertEquals(HttpRequestErrorType.CONNECTION_ERROR, response.error?.type)
        }
    }

    @Test
    fun cancellation() = sdkTest {
        val latch = CountDownLatch(1)
        mockWebServerRule.requestHandlers.add {
            latch.await()
            MockResponse().setBody("ok").setResponseCode(200)
        }

        val httpClient = HttpServiceFactory.getInstance()
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            httpClient.request(
                HttpRequest.Builder()
                    .headers(HashMap())
                    .sdkInformation(SdkInfoProvider.sdkInformation())
                    .url(mockWebServerRule.baseUrl)
                    .build(),
            )
        }
        assertTrue(request.isActive)
        request.cancel()
        latch.countDown()
        delay(100)
    }
}
