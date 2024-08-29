package com.mapbox.navigation.testing.fakes

import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URL

class TestUrlSkuTokenProvider(
    val skuValue: String = "test-sku"
): UrlSkuTokenProvider {
    override fun obtainUrlWithSkuToken(resourceUrl: URL): URL {
        return resourceUrl.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("sku", skuValue)
            ?.build()
            ?.toUrl()
            ?: resourceUrl
    }
}
