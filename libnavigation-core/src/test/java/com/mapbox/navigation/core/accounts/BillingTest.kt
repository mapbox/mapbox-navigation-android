package com.mapbox.navigation.core.accounts

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import com.mapbox.android.accounts.v1.AccountsConstants
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BillingTest {

    private val appContext: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val nameNotFoundException: NameNotFoundException = mockk(relaxed = true)
    private val applicationInfo: ApplicationInfo = mockk()
    private val metadata: Bundle = mockk()

    @Before
    fun setUp() {
        every { appContext.packageManager } returns packageManager
    }

    @After
    fun cleanUp() {
        Billing.INSTANCE = null
    }

    @Test
    fun verify_default_billing_type() {
        every { metadata.getBoolean(any()) } returns false
        every { metadata.getBoolean(any(), any()) } returns false
        applicationInfo.metaData = metadata
        every { packageManager.getApplicationInfo(any(), any()) } returns applicationInfo

        assertEquals(Billing.BillingModel.MAU, Billing.getInstance(appContext).getBillingType())
    }

    @Test
    fun verify_mau_billing_type() {
        every { metadata.getBoolean(any()) } returns false
        every { metadata.getBoolean(any(), any()) } returns false
        applicationInfo.metaData = metadata
        every { packageManager.getApplicationInfo(any(), any()) } returns applicationInfo

        assertEquals(Billing.BillingModel.MAU, Billing.getInstance(appContext).getBillingType())
    }

    @Test
    fun verify_none_billing_type() {
        every { metadata.getBoolean(any()) } returns AccountsConstants.DEFAULT_TOKEN_MANAGE_SKU
        every { metadata.getBoolean(any(), any()) } returns AccountsConstants.DEFAULT_TOKEN_MANAGE_SKU
        applicationInfo.metaData = metadata
        every { packageManager.getApplicationInfo(any(), any()) } returns applicationInfo

        assertEquals(Billing.BillingModel.NO_SKU, Billing.getInstance(appContext).getBillingType())
    }

    @Test
    fun verify_billing_type_when_packageNameNotFound() {
        every { packageManager.getApplicationInfo(any(), any()) } throws nameNotFoundException
        val billingType = Billing.getInstance(appContext).getBillingType()
        assertEquals(Billing.BillingModel.MAU, billingType)
    }
}
