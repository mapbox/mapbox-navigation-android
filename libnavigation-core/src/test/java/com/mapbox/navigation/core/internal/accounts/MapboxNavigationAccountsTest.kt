package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.navigation.base.internal.accounts.SkuIdProviderImpl
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.core.accounts.BillingServiceProvider
import com.mapbox.navigation.core.accounts.BillingServiceProxy
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class MapboxNavigationAccountsTest {

    private lateinit var billingService: BillingServiceProxy
    private lateinit var urlSkuTokenProvider: UrlSkuTokenProvider

    @Before
    fun setup() {
        billingService = mockk<BillingServiceProxy>()
        val skuIdProvider = SkuIdProviderImpl()
        urlSkuTokenProvider = MapboxNavigationAccounts(skuIdProvider, billingService)

        mockkObject(BillingServiceProvider)
        every { BillingServiceProvider.getInstance() } returns billingService
    }

    @Test
    fun `obtainSkuToken when resourceUrl notNullOrEmpty querySize zero sessionActiveGuidance`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns "12345"

        val result = urlSkuTokenProvider.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/"),
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?sku=12345"),
            result,
        )
    }

    @Test
    fun `obtainSkuToken when resourceUrl not empty and querySize not zero sessionActiveGuidance`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns "12345"

        val result = urlSkuTokenProvider.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test"),
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test&sku=12345"),
            result,
        )
    }

    @Test
    fun `obtainSkuToken when resourceUrl notNullOrEmpty but token blank then sku is not added`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        } returns BillingSessionStatus.NO_SESSION

        val result = urlSkuTokenProvider.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test"),
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test"),
            result,
        )
    }

    @Test
    fun `obtainSkuToken when free drive session`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE
        every {
            billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        } returns "12345"

        val result = urlSkuTokenProvider.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/"),
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?sku=12345"),
            result,
        )
    }

    @Test
    fun `do not append sku token when active guidance session paused`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.SESSION_PAUSED
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        } returns BillingSessionStatus.NO_SESSION

        val result = urlSkuTokenProvider.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/"),
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/"),
            result,
        )
    }

    @Test
    fun `do not append sku token when free drive session paused`() {
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_FDTRIP)
        } returns BillingSessionStatus.SESSION_PAUSED

        val result = urlSkuTokenProvider.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/"),
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/"),
            result,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(BillingServiceProvider)
    }
}
