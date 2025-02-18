package com.mapbox.navigation.mapgpt.core.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import kotlinx.coroutines.CancellationException
import okhttp3.Protocol
import java.net.ProtocolException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal object HttpClientProvider {

    fun createHttpClient(
        chunkEncoding: Boolean,
        block: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit,
    ): HttpClientWrapper {
        return HttpClientWrapper(
            client = HttpClient(OkHttp) {
                engine {
                    config {
                        if (chunkEncoding) {
                            protocols(listOf(Protocol.HTTP_1_1))
                        }
                        connectTimeout(30, TimeUnit.SECONDS)
                        readTimeout(30, TimeUnit.SECONDS)
                        writeTimeout(30, TimeUnit.SECONDS)
                        pingInterval(20, TimeUnit.SECONDS)
                    }
                }
                HttpResponseValidator {
                    handleResponseExceptionWithRequest { cause, request ->
                        when (cause) {
                            is ProtocolException -> throw HttpClientException.Protocol(
                                request,
                                cause,
                            )

                            is UnknownHostException -> throw HttpClientException.Network(
                                request,
                                cause,
                            )

                            else -> throw HttpClientException.Other(request, cause)
                        }
                    }
                }
                block()
            },
            customPingStatus = CustomPingStatus.Disabled,
            shouldReconnectForCancellationReason = { false },
        )
    }
}

internal data class HttpClientWrapper(
    val client: HttpClient,
    val customPingStatus: CustomPingStatus,
    val shouldReconnectForCancellationReason: (exception: CancellationException) -> Boolean,
)

internal sealed class CustomPingStatus {
    object Disabled : CustomPingStatus()

    data class Enabled(val interval: Duration) : CustomPingStatus()
}
