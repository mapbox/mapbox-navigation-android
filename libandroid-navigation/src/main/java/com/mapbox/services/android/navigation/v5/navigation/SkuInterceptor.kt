package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.services.android.navigation.v5.accounts.MapboxNavigationAccounts
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

import java.io.IOException

internal class SkuInterceptor(private val context: Context) : Interceptor {

    private val SKU_KEY = "sku"

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val skuToken = MapboxNavigationAccounts.getInstance(context).obtainSkuToken()
        val url = request.url().newBuilder().addQueryParameter(SKU_KEY, skuToken).build()
        request = request.newBuilder().url(url).build()
        return chain.proceed(request)
    }
}
