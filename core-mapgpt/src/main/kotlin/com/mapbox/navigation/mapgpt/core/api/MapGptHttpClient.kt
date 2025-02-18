package com.mapbox.navigation.mapgpt.core.api

import com.mapbox.navigation.mapgpt.core.common.sharedGzipCompress
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface MapGptHttpClient : CoroutineScope {

    val customPingStatus: CustomPingStatus

    suspend fun preparePost(
        apiHost: String,
        path: String?,
        headers: Map<String, String>? = null,
        jsonBody: String,
        queries: Map<String, String>? = null,
        compressBody: Boolean = false,
        timeout: Duration? = null,
    ): HttpStatement

    suspend fun websocketConnection(
        accessToken: String,
        apiHost: String,
        reconnectSessionId: String?,
    ): ClientWebSocketSession

    fun shouldReconnectForCancellationReason(exception: CancellationException): Boolean

    fun close()
}

private const val HEADER_SESSION_ID = "Session-Id"
private const val HEADER_VALUE_GZIP = "gzip"

private suspend fun preparePost(
    ktorClient: HttpClient,
    apiHost: String,
    path: String?,
    headers: Map<String, String>?,
    jsonBody: String,
    queries: Map<String, String>?,
    compressBody: Boolean,
    timeout: Duration?,
): HttpStatement {
    val compressed = if (compressBody) sharedGzipCompress(jsonBody) else null
    val (requestHeaders, requestBody) = compressed?.let {
        val mutableHeaders = headers?.toMutableMap() ?: mutableMapOf()
        mutableHeaders[HttpHeaders.ContentEncoding] = HEADER_VALUE_GZIP
        mutableHeaders to compressed
    } ?: (headers to jsonBody)
    return ktorClient.preparePost {
        url {
            protocol = URLProtocol.HTTPS
            host = apiHost
            path?.let { appendPathSegments(it) }
            queries?.forEach {
                parameters.append(it.key, it.value)
            }
        }
        requestHeaders?.forEach {
            header(it.key, it.value)
        }
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        setBody(requestBody)
        timeout {
            timeout?.let {
                requestTimeoutMillis = it.inWholeMilliseconds
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun HttpClientConfig<out HttpClientEngineConfig>.httpClientConfig(
    defaultHttpTimeout: Duration,
) {
    expectSuccess = true
    install(ContentNegotiation) {
        val jsonConfig = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
        json(jsonConfig)
    }
    install(ContentEncoding) {
        gzip()
        deflate()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = defaultHttpTimeout.inWholeMilliseconds
    }
    install(WebSockets) {
        // The ping interval definition is set independently for the OkHttp engine used on Android.
        // Look for it in the platform-specific file.
        // We're disabling ping/pong on iOS due to https://youtrack.jetbrains.com/issue/KTOR-5540
        // and instead use a custom mechanism.
        // pingInterval = 20_000
    }
}

private suspend fun buildDefaultClientWebSocketSession(
    ktorClient: HttpClient,
    apiHost: String,
    accessToken: String,
    reconnectSessionId: String?,
): DefaultClientWebSocketSession {
    return ktorClient.webSocketSession(urlString = apiHost) {
        header(HttpHeaders.Authorization, accessToken)
        reconnectSessionId?.let { header(HEADER_SESSION_ID, it) }
    }
}

internal class MapGptHttpStreamClientImpl(
    defaultHttpTimeout: Duration = 10.seconds,
) : MapGptHttpClient {

    private val httpClientWrapper = HttpClientProvider.createHttpClient(true) {
        httpClientConfig(defaultHttpTimeout)
    }
    private val ktorClient: HttpClient = httpClientWrapper.client
    override val customPingStatus: CustomPingStatus = httpClientWrapper.customPingStatus

    override val coroutineContext: CoroutineContext = ktorClient.coroutineContext

    override suspend fun preparePost(
        apiHost: String,
        path: String?,
        headers: Map<String, String>?,
        jsonBody: String,
        queries: Map<String, String>?,
        compressBody: Boolean,
        timeout: Duration?,
    ): HttpStatement {
        return preparePost(
            ktorClient,
            apiHost,
            path,
            headers,
            jsonBody,
            queries,
            compressBody,
            timeout,
        )
    }

    override fun close() {
        ktorClient.close()
    }

    override suspend fun websocketConnection(
        accessToken: String,
        apiHost: String,
        reconnectSessionId: String?,
    ): DefaultClientWebSocketSession {
        return buildDefaultClientWebSocketSession(
            ktorClient,
            apiHost,
            accessToken,
            reconnectSessionId,
        )
    }

    override fun shouldReconnectForCancellationReason(exception: CancellationException): Boolean {
        return httpClientWrapper.shouldReconnectForCancellationReason(exception)
    }
}
