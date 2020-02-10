package com.mapbox.navigation.core.accounts

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import com.mapbox.android.accounts.v1.AccountsConstants
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingTest {

    private val appContext: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val nameNotFoundException: NameNotFoundException = mockk(relaxed = true)
    private val applicationInfo: ApplicationInfo = mockk()
    private val metadata: Bundle = mockk()

    @Test
    fun verifyDefaultBillingType() {
        every { appContext.packageManager } returns packageManager
        every { metadata.getBoolean(any()) } returns false
        every { metadata.getBoolean(any(), any()) } returns false
        applicationInfo.metaData = metadata
        every { packageManager.getApplicationInfo(any(), any()) } returns applicationInfo

        val billingType = Billing.getInstance(appContext).getBillingType()

        assertEquals(Billing.BillingModel.MAU, billingType)

        Billing.INSTANCE = null
    }

    @Test
    fun verifyMauBillingType() {
        every { appContext.packageManager } returns packageManager
        every { metadata.getBoolean(any()) } returns false
        every { metadata.getBoolean(any(), any()) } returns false
        applicationInfo.metaData = metadata
        every { packageManager.getApplicationInfo(any(), any()) } returns applicationInfo

        val billingType = Billing.getInstance(appContext).getBillingType()

        assertEquals(Billing.BillingModel.MAU, billingType)

        Billing.INSTANCE = null
    }

    @Test
    fun verifyNoneBillingType() {
        every { appContext.packageManager } returns packageManager
        every { metadata.getBoolean(any()) } returns AccountsConstants.DEFAULT_TOKEN_MANAGE_SKU
        every { metadata.getBoolean(any(), any()) } returns AccountsConstants.DEFAULT_TOKEN_MANAGE_SKU
        applicationInfo.metaData = metadata
        every { packageManager.getApplicationInfo(any(), any()) } returns applicationInfo

        val billingType = Billing.getInstance(appContext).getBillingType()

        assertEquals(Billing.BillingModel.NO_SKU, billingType)

        Billing.INSTANCE = null
    }

    @Test
    fun verifyBillingTypeWhenPackageNameNotFound() {
        every { appContext.packageManager } returns packageManager
        every { packageManager.getApplicationInfo(any(), any()) } throws nameNotFoundException

        val billingType = Billing.getInstance(appContext).getBillingType()

        assertEquals(Billing.BillingModel.MAU, billingType)

        Billing.INSTANCE = null
    }
}
