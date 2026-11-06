package com.mapbox.navigation.base.internal.utils

import androidx.annotation.RestrictTo
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Adds the Mapbox access token to URLs, and only to URLs that point at the Mapbox API.
 *
 * Image URLs such as junction views, signboards and rest area maps come from the route
 * response. A tampered response, or a route from a non-Mapbox router, can point them at any host,
 * so the token must never be attached by default.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MapboxApiUrlUtils {

    private const val MAPBOX_API_HOST = "api.mapbox.com"
    private const val ACCESS_TOKEN = "access_token"

    /**
     * Returns true when [url] parses and has the `https` scheme and the `api.mapbox.com` host.
     * Returns false for any other URL, including one that cannot be parsed.
     */
    @JvmStatic
    fun isMapboxApiEndpoint(url: String): Boolean =
        url.toHttpUrlOrNull()?.isMapboxApiEndpoint() == true

    /**
     * Returns [url] with its `access_token` query parameter set to [accessToken].
     *
     * The URL is returned unchanged when it is not a [isMapboxApiEndpoint] URL or when
     * [accessToken] is empty, so the token is never sent to another host.
     */
    @JvmStatic
    fun withMapboxAccessToken(url: String, accessToken: String): String {
        val httpUrl = url.toHttpUrlOrNull()
        if (accessToken.isEmpty() || httpUrl?.isMapboxApiEndpoint() != true) {
            return url
        }
        return httpUrl.newBuilder()
            .setQueryParameter(ACCESS_TOKEN, accessToken)
            .build()
            .toString()
    }

    private fun HttpUrl.isMapboxApiEndpoint() = isHttps && host == MAPBOX_API_HOST
}
