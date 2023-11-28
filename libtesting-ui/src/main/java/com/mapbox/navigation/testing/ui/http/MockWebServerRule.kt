package com.mapbox.navigation.testing.ui.http

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assume.assumeTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.lang.StringBuilder
import kotlin.time.Duration

/**
 * Creates and initializes a [MockWebServer] for each test.
 *
 * All request that should be mocked need to have a base url equal to [baseUrl]
 * and have a [MockRequestHandler] that can serve the request added to the [requestHandlers] list.
 */
class MockWebServerRule : TestWatcher() {

    var webServer = MockWebServer()
        private set

    /**
     * @see [MockWebServer.url]
     */
    val baseUrl = webServer.url("").toString().dropLast(1) // drop the last `/`, RouteOptions::toUrl() will add it

    /**
     * Add [MockRequestHandler]s to this list for each request that should be handled.
     *
     * All mocked request should target [baseUrl].
     *
     * Cleared after each test.
     */
    val requestHandlers = mutableListOf<MockRequestHandler>()

    init {
        initDispatcher()
    }

    private fun initDispatcher() {
        webServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestHandlers.forEach {
                    it.handle(request)?.run { return this }
                }

                val formattedHandlersBuilder = StringBuilder()
                requestHandlers.forEach {
                    formattedHandlersBuilder.append("$it\n|")
                }
                return MockResponse().setResponseCode(500)
                    .setBody(
                        """Request url:
                          |${request.path}
                          |is not handled.
                          |
                          |Available handlers:
                          |$formattedHandlersBuilder
                        """.trimMargin()
                    )
            }
        }
    }

    override fun starting(description: Description?) {
        // no need to start the webServer, already started implicitly when fetching baseUrl
        // webServer.start()
    }

    override fun finished(description: Description?) {
        requestHandlers.clear()
        webServer.shutdown()
    }

    suspend fun withoutWebServer(block: suspend () -> Unit) {
        val previousPort = webServer.port
        webServer.shutdown()
        try {
            block()
        } finally {
            withContext(Dispatchers.IO) {
                val serverRestarted = retryStarting(previousPort)
                assumeTrue("Mock web server could not be restarted", serverRestarted)
                initDispatcher()
            }
        }
    }

    private suspend fun retryStarting(port: Int): Boolean {
        return withTimeoutOrNull(30_000) {
            while (true) {
                try {
                    webServer = MockWebServer()
                    webServer.start(port)
                    break
                } catch (t: Throwable) {
                    Log.e("MockWebServerRule", "error starting mock web server", t)
                }
                delay(500)
            }
            true
        } ?: false
    }
}
