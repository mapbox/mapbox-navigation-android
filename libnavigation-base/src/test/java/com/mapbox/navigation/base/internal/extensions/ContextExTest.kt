package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.testing.NavSDKRobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.util.Locale

@RunWith(NavSDKRobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.N])
class ContextExTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Config(qualifiers = "ja")
    @Test
    fun inferDeviceLocale() {
        val locale = Locale("en")
        context.resources.configuration.setLocales(LocaleList(locale))

        val result = context.inferDeviceLocale()

        assertEquals(locale, result)
    }

    @Config(qualifiers = "ja")
    @Test
    fun inferDeviceLocaleWhenLocalListEmpty() {
        val locale = Locale("ja")
        context.resources.configuration.setLocales(LocaleList.getEmptyLocaleList())

        val result = context.inferDeviceLocale()

        assertEquals(locale, result)
    }

    @Config(qualifiers = "ja")
    @Test
    fun inferDeviceLocaleWhenSDKVersionBelowN() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 16)
        val expectedLocale = Locale("ja")

        val result = context.inferDeviceLocale()

        assertEquals(expectedLocale, result)
    }
}
