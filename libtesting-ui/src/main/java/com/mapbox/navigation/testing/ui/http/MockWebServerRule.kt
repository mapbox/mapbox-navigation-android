package com.mapbox.navigation.testing.ui.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.lang.StringBuilder

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

    private var previousPort: Int? = null

    fun stop() {
        previousPort = webServer.port
        webServer.shutdown()
    }

    suspend fun restart() {
        withContext(Dispatchers.IO) {
            webServer = MockWebServer()
            initDispatcher()
            webServer.start(previousPort!!)
        }
    }
}
