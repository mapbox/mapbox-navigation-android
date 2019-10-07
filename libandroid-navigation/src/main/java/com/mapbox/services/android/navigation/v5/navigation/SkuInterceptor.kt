package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.services.android.navigation.v5.internal.accounts.MapboxNavigationAccounts
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class SkuInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val SKU_KEY = "sku"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val skuToken = MapboxNavigationAccounts.getInstance(context).obtainSkuToken()
        val url = request.url().newBuilder().addQueryParameter(SKU_KEY, skuToken).build()
        request = request.newBuilder().url(url).build()
        return chain.proceed(request)
    }
}
