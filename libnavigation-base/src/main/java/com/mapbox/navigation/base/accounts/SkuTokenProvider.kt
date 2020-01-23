package com.mapbox.navigation.base.accounts

interface SkuTokenProvider {
    fun obtainSkuToken(resourceUrl: String? = null, querySize: Int = -1): String
}