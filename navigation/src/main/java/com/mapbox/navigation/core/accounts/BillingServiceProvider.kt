package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingServiceFactory

internal object BillingServiceProvider {
    fun getInstance(): BillingServiceProxy =
        BillingServiceProxy(BillingServiceFactory.getInstance())
}
