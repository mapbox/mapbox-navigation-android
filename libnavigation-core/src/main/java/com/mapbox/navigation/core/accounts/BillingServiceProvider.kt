package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingServiceFactory
import com.mapbox.common.BillingServiceInterface

internal object BillingServiceProvider {
    fun getInstance(): BillingServiceInterface = BillingServiceFactory.getInstance()
}
