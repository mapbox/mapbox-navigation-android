package com.mapbox.navigation.core.internal.accounts

import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(NavSDKRobolectricTestRunner::class)
class MapboxNavigationAccountsTest {

    @Test
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_querySize_zero() {
        mockkObject(TokenGeneratorProvider)
        every { TokenGeneratorProvider.getNavigationTokenGenerator() } returns mockk {
            every { getSKUToken() } returns "12345"
        }

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?sku=12345"),
            result
        )
        unmockkObject(TokenGeneratorProvider)
    }

    @Test
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_querySize_not_zero() {
        mockkObject(TokenGeneratorProvider)
        every { TokenGeneratorProvider.getNavigationTokenGenerator() } returns mockk {
            every { getSKUToken() } returns "12345"
        }

        val result = MapboxNavigationAccounts.obtainUrlWithSkuToken(
            URL("https://www.mapbox.com/some/params/?query=test")
        )

        assertEquals(
            URL("https://www.mapbox.com/some/params/?query=test&sku=12345"),
            result
        )
        unmockkObject(TokenGeneratorProvider)
    }

    @Test
    fun obtainSkuId_is_08() {
        assertEquals("08", MapboxNavigationAccounts.obtainSkuId())
    }
}
