package com.mapbox.navigation.core.accounts

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.BillingService
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.OnBillingServiceError
import com.mapbox.common.SKUIdentifier
import com.mapbox.navigation.utils.internal.LoggerProvider

internal object BillingServiceWrapper {
    fun triggerBillingEvent(
        accessToken: String,
        userAgent: String,
        skuIdentifier: SKUIdentifier,
        callback: OnBillingServiceError
    ) {
        LoggerProvider.logger.i(
            Tag("BillingTest"),
            Message("triggerBillingEvent: $skuIdentifier, $accessToken")
        )
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
        LoggerProvider.logger.i(
            Tag("BillingTest"),
            Message("beginBillingSession: $skuIdentifier, $accessToken, $validity")
        )
        BillingService.beginBillingSession(
            accessToken,
            userAgent,
            skuIdentifier,
            callback,
            validity
        )
    }

    fun pauseBillingSession(skuIdentifier: SKUIdentifier) {
        LoggerProvider.logger.i(
            Tag("BillingTest"),
            Message("pauseBillingSession: $skuIdentifier")
        )
        BillingService.pauseBillingSession(skuIdentifier)
    }

    fun resumeBillingSession(skuIdentifier: SKUIdentifier, callback: OnBillingServiceError) {
        LoggerProvider.logger.i(
            Tag("BillingTest"),
            Message("resumeBillingSession: $skuIdentifier")
        )
        BillingService.resumeBillingSession(skuIdentifier, callback)
    }

    fun stopBillingSession(skuIdentifier: SKUIdentifier) {
        LoggerProvider.logger.i(
            Tag("BillingTest"),
            Message("stopBillingSession: $skuIdentifier")
        )
        BillingService.stopBillingSession(skuIdentifier)
    }

    fun getSessionStatus(skuIdentifier: SKUIdentifier): BillingSessionStatus {
        return BillingService.getSessionStatus(skuIdentifier)
    }
}
