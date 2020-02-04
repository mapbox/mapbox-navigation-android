package com.mapbox.navigation.core.accounts

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.accounts.v1.AccountsConstants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxNavigationAccountsTest {

    val ctx = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        ctx.applicationInfo.metaData = Bundle().also {
            it.putBoolean(AccountsConstants.KEY_META_DATA_MANAGE_SKU, false)
        }

        ctx.getSharedPreferences(
                AccountsConstants.MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
        ).edit().putString("com.mapbox.navigation.accounts.trips.skutoken", "myTestToken")
                .commit()

        ctx.getSharedPreferences(
                AccountsConstants.MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
        ).edit().putString("com.mapbox.navigation.accounts.mau.skutoken", "myTestToken")
                .commit()
    }

    @After
    fun tearDown() {
        ctx.applicationInfo.metaData = null
    }

    @Test(expected = IllegalStateException::class)
    fun obtainSkuToken_when_resourceUrl_empty() {
        val instance = MapboxNavigationAccounts.getInstance(ctx)

        val result = instance.obtainUrlWithSkuToken("", 4)

        assertEquals("", result)
    }

    @Test(expected = IllegalStateException::class)
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_and_querySize_lessThan_zero() {
        val instance = MapboxNavigationAccounts.getInstance(ctx)

        instance.obtainUrlWithSkuToken("http://www.mapbox.com", -1)
    }

    @Test
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_and_BillingModel_MAU() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.applicationInfo.metaData = Bundle().also {
            it.putBoolean(AccountsConstants.KEY_META_DATA_MANAGE_SKU, false)
        }
        val instance = MapboxNavigationAccounts.getInstance(ctx)

        val result = instance.obtainUrlWithSkuToken("http://www.mapbox.com", 5)

        assertNotNull(result.substringAfterLast(""))
    }

    @Test
    fun obtainSkuToken_when_resourceUrl_notNullOrEmpty_and_BillingModel_default() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val instance = MapboxNavigationAccounts.getInstance(ctx)

        val result = instance.obtainUrlWithSkuToken("http://www.mapbox.com", 5)

        assertNotNull(result.substringAfterLast(""))
    }
}
