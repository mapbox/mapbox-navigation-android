package com.mapbox.navigation.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Handler
import androidx.core.content.ContextCompat
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocales
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class SystemLocaleWatcherTest {

    private val context = mockk<Context>(relaxed = true)
    private val navigator = mockk<MapboxNativeNavigator>(relaxed = true)
    private val handler = mockk<Handler>(relaxed = true)

    private val broadcastReceiverSlot = slot<BroadcastReceiver>()

    private lateinit var systemLocaleWatcher: SystemLocaleWatcher

    private val defaultLocaleTags = listOf("en", "fr")

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.registerReceiver(
                any(),
                capture(broadcastReceiverSlot),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

        mockkStatic(Context::inferDeviceLocales)
        every { context.inferDeviceLocales() } returns defaultLocaleTags.toLocales()

        val runnableSlot = slot<Runnable>()
        every { handler.postDelayed(capture(runnableSlot), any()) } answers {
            runnableSlot.captured.run()
            true
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(Context::inferDeviceLocales)
    }

    private fun createWatcher() {
        systemLocaleWatcher = SystemLocaleWatcher.create(context, navigator, handler)
    }

    @Test
    fun subscribesToLocaleChangeUpdatesOnInit() {
        createWatcher()
        verify(exactly = 1) {
            ContextCompat.registerReceiver(
                context,
                any(),
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    @Test
    fun updatesCachedLocaleOnLocaleChanges() {
        val newLocaleTags = listOf("en", "fr", "en")

        every {
            context.inferDeviceLocales()
        } returnsMany listOf(defaultLocaleTags.toLocales(), newLocaleTags.toLocales())

        createWatcher()

        broadcastReceiverSlot.captured.onReceive(context, mockk(relaxed = true))

        verify(exactly = 1) {
            handler.postDelayed(any(), 100L)
        }

        verify(exactly = 2) {
            context.inferDeviceLocales()
        }

        verify {
            navigator.setUserLanguages(defaultLocaleTags)
            navigator.setUserLanguages(newLocaleTags)
        }
    }

    @Test
    fun unsubscribesFromLocaleChangesUpdatesOnDestroy() {
        createWatcher()
        systemLocaleWatcher.destroy()

        verify(exactly = 1) {
            context.unregisterReceiver(broadcastReceiverSlot.captured)
        }
    }

    @Test
    fun setsUserLanguagesOnInit() {
        createWatcher()
        verify(exactly = 1) {
            navigator.setUserLanguages(defaultLocaleTags)
        }
    }

    @Test
    fun setsUserLanguageProviderOnNavigationRecreationEvent() {
        val navRecreationObserverSlot = slot<NativeNavigatorRecreationObserver>()
        every {
            navigator.addNativeNavigatorRecreationObserver(capture(navRecreationObserverSlot))
        } just Runs

        createWatcher()
        navRecreationObserverSlot.captured.onNativeNavigatorRecreated()
        verify(exactly = 2) {
            navigator.setUserLanguages(defaultLocaleTags)
        }
    }

    @Test
    fun setsOverriddenUserLanguagesWhenOverrideIsApplied() {
        createWatcher()

        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE", "fr-FR"))

        verify(exactly = 1) {
            navigator.setUserLanguages(listOf("de-DE", "fr-FR"))
        }
    }

    @Test
    fun restoresSystemUserLanguagesWhenOverrideIsCleared() {
        createWatcher()
        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))

        systemLocaleWatcher.setOverrideLanguages(null)

        verify(exactly = 2) {
            navigator.setUserLanguages(defaultLocaleTags)
        }
    }

    @Test
    fun keepsOverriddenUserLanguagesOnNavigationRecreationEvent() {
        val navRecreationObserverSlot = slot<NativeNavigatorRecreationObserver>()
        every {
            navigator.addNativeNavigatorRecreationObserver(capture(navRecreationObserverSlot))
        } just Runs

        createWatcher()
        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))
        navRecreationObserverSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 2) {
            navigator.setUserLanguages(listOf("de-DE"))
        }
    }

    @Test
    fun doesNotUpdateUserLanguagesWhenSameOverrideIsSetTwice() {
        createWatcher()
        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))

        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))

        verify(exactly = 1) {
            navigator.setUserLanguages(listOf("de-DE"))
        }
    }

    @Test
    fun unregistersLocaleChangeReceiverWhenOverrideIsApplied() {
        createWatcher()

        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))

        verify(exactly = 1) {
            context.unregisterReceiver(broadcastReceiverSlot.captured)
        }
    }

    @Test
    fun reregistersLocaleChangeReceiverWhenOverrideIsCleared() {
        createWatcher()
        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))

        systemLocaleWatcher.setOverrideLanguages(null)

        verify(exactly = 2) {
            ContextCompat.registerReceiver(
                context,
                any(),
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    @Test
    fun doesNotReregisterLocaleChangeReceiverAfterDestroy() {
        createWatcher()
        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))
        systemLocaleWatcher.destroy()

        systemLocaleWatcher.setOverrideLanguages(null)

        verify(exactly = 1) {
            ContextCompat.registerReceiver(
                context,
                any(),
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    @Test
    fun ignoresOverrideChangesAfterDestroy() {
        createWatcher()
        systemLocaleWatcher.destroy()

        systemLocaleWatcher.setOverrideLanguages(listOf("de-DE"))

        verify(exactly = 0) {
            navigator.setUserLanguages(listOf("de-DE"))
        }
    }

    private companion object {
        fun List<String>.toLocales() = map { Locale(it) }
    }
}
