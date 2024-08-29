package com.mapbox.navigation.testing.ui.http

import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.navigation.testing.ui.utils.parameters
import java.net.URL

/**
 * SKU id prefixes
 *
 * @see https://github.com/mapbox/mapbox-sdk-common/blob/adfdc9e46f3f34984dd7a6329789aeeb09563d12/components/billing/src/mapbox/common/accounts_manager.cpp#L43..L77
 * @see https://github.com/mapbox/pricebook/blob/master/docs/skus.md
 */
private const val ACTIVE_GUIDANCE_SKU_PREFIX = "10j"
private const val FREE_DRIVE_SKU_PREFIX = "10k"
private const val SESSIONS_ENDPOINT_PATH = "/sdk-sessions"

val HttpServiceEvent.skuParameter: String?
    get() = url.parameters()["sku"]

val HttpServiceEvent.duration: Long?
    get() = url.parameters()["duration"]?.toLong()

val HttpServiceEvent.isActiveGuidanceSession: Boolean
    get() = skuParameter?.startsWith(ACTIVE_GUIDANCE_SKU_PREFIX) == true

val HttpServiceEvent.isFreeDriveSession: Boolean
    get() = skuParameter?.startsWith(FREE_DRIVE_SKU_PREFIX) == true

val HttpServiceEvent.isBillingEvent: Boolean
    get() = url.path.startsWith(SESSIONS_ENDPOINT_PATH) &&
        (isFreeDriveSession || isActiveGuidanceSession)

sealed class HttpServiceEvent {

    abstract val request: HttpRequest

    val url: URL
        get() = URL(request.url)

    data class Request(override val request: HttpRequest) : HttpServiceEvent()

    data class Download(
        val download: DownloadOptions,
        override val request: HttpRequest = download.request
    ) : HttpServiceEvent()

    data class Response(
        val response: HttpResponse,
        override val request: HttpRequest = response.request
    ) : HttpServiceEvent()
}
