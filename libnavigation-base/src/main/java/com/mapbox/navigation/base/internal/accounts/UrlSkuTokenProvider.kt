/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.base.internal.accounts

import java.net.URL

fun interface UrlSkuTokenProvider {
    fun obtainUrlWithSkuToken(resourceUrl: URL): URL
}
