package com.mapbox.navigation.mapgpt.core.api

import io.ktor.client.request.HttpRequest

/**
 * Represents exceptions thrown during by clients from the [HttpClientProvider].
 * Categorizes exceptions into [Protocol], [Network], and [Other] for refined error handling in a
 * Kotlin multiplatform context, accommodating the platform's limitation on generic exception types.
 */
sealed class HttpClientException(val request: HttpRequest, cause: Throwable?) : Exception(cause) {
    /**
     * Represents errors related to HTTP or other communication protocols.
     * Includes issues like HTTP status errors (e.g., 400 Bad Request, 500 Server Error),
     * malformed requests not adhering to protocol specifications, and SSL/TLS negotiation failures.
     */
    class Protocol(request: HttpRequest, cause: Throwable?) : HttpClientException(request, cause)

    /**
     * Signifies errors due to network connectivity issues.
     * Encompasses problems such as no internet connection, network interface failures,
     * DNS lookup failures, and timeouts due to unreachable hosts.
     */
    class Network(request: HttpRequest, cause: Throwable?) : HttpClientException(request, cause)

    /**
     * Catches all other exceptions not classified as [Protocol] or [Network].
     * Includes unexpected errors such as internal library faults, resource exhaustion,
     * or unclassified [Protocol] or [Network] errors.
     */
    class Other(request: HttpRequest, cause: Throwable?) : HttpClientException(request, cause)
}
