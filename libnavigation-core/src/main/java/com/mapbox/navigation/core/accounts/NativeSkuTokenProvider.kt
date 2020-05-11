package com.mapbox.navigation.core.accounts

import android.content.Context
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigator.SkuToken

internal class NativeSkuTokenProvider(private val context: Context) : SkuToken() {
    override fun get(): String = MapboxNavigationAccounts.getInstance(context).obtainSkuToken()
}
