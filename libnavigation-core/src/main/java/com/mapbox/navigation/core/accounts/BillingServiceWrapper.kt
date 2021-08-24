package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingService
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.OnBillingServiceError
import com.mapbox.common.SKUIdentifier

internal object BillingServiceWrapper {
    fun triggerBillingEvent(
        accessToken: String,
        userAgent: String,
        skuIdentifier: SKUIdentifier,
        callback: OnBillingServiceError
    ) {
        BillingService.triggerBillingEvent(
            accessToken,
            userAgent,
            skuIdentifier,
            callback
        )
    }

    fun beginBillingSession(
        accessToken: String,
        userAgent: String,
        skuIdentifier: SKUIdentifier,
        callback: OnBillingServiceError,
        validity: Long
    ) {
        BillingService.beginBillingSession(
            accessToken,
            userAgent,
            skuIdentifier,
            callback,
            validity
        )
    }

    fun pauseBillingSession(skuIdentifier: SKUIdentifier) {
        BillingService.pauseBillingSession(skuIdentifier)
    }

    fun resumeBillingSession(skuIdentifier: SKUIdentifier, callback: OnBillingServiceError) {
        BillingService.resumeBillingSession(skuIdentifier, callback)
    }

    fun stopBillingSession(skuIdentifier: SKUIdentifier) {
        BillingService.stopBillingSession(skuIdentifier)
    }

    fun getSessionStatus(skuIdentifier: SKUIdentifier): BillingSessionStatus {
        return BillingService.getSessionStatus(skuIdentifier)
    }
}
