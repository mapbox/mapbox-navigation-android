package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingService
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.OnBillingServiceError
import com.mapbox.common.SdkInformation
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.common.UserSKUIdentifier

// Used for testing - BillingService methods are not mockable
internal class BillingServiceProxy(
    private val billingService: BillingService,
) {

    fun triggerUserBillingEvent(
        sdkInformation: SdkInformation?,
        skuIdentifier: UserSKUIdentifier?,
        callback: OnBillingServiceError?,
    ) {
        billingService.triggerUserBillingEvent(sdkInformation, skuIdentifier, callback)
    }

    fun beginBillingSession(
        sdkInformation: SdkInformation?,
        skuIdentifier: SessionSKUIdentifier?,
        callback: OnBillingServiceError?,
        validity: Long,
    ) {
        billingService.beginBillingSession(sdkInformation, skuIdentifier, callback, validity)
    }

    fun pauseBillingSession(skuIdentifier: SessionSKUIdentifier?) {
        billingService.pauseBillingSession(skuIdentifier)
    }

    fun resumeBillingSession(
        skuIdentifier: SessionSKUIdentifier?,
        callback: OnBillingServiceError?,
    ) {
        billingService.resumeBillingSession(skuIdentifier, callback)
    }

    fun stopBillingSession(skuIdentifier: SessionSKUIdentifier?) {
        billingService.stopBillingSession(skuIdentifier)
    }

    fun getSessionSKUTokenIfValid(skuIdentifier: SessionSKUIdentifier?): String? {
        return billingService.getSessionSKUTokenIfValid(skuIdentifier)
    }

    fun getSessionStatus(skuIdentifier: SessionSKUIdentifier?): BillingSessionStatus {
        return billingService.getSessionStatus(skuIdentifier)
    }
}
