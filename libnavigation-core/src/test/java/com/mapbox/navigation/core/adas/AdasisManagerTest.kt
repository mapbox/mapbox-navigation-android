package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import com.mapbox.navigator.ADASISv2MessageCallback
import com.mapbox.navigator.GraphAccessorInterface
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class AdasisManagerTest {

    private val nativeAdasisMessageCallbackSlot = slot<ADASISv2MessageCallback>()
    private val navigatorRecreationObserverSlot = slot<NativeNavigatorRecreationObserver>()

    private val graphAccessor: GraphAccessorInterface = mockk(relaxed = true)
    private val navigator: MapboxNativeNavigator = mockk(relaxed = true)
    private lateinit var adasisManager: AdasisManager

    @Before
    fun setUp() {
        every { navigator.graphAccessor } returns graphAccessor
        every {
            navigator.setNativeNavigatorRecreationObserver(
                capture(navigatorRecreationObserverSlot),
            )
        } returns Unit
        every {
            navigator.setAdasisMessageCallback(capture(nativeAdasisMessageCallbackSlot), any())
        } answers {
            nativeAdasisMessageCallbackSlot.captured.run(TEST_MESSAGE_BUFFER)
        }

        adasisManager = AdasisManager(navigator)
    }

    @Test
    fun `registers for navigator recreation events on init`() {
        verify(exactly = 1) {
            navigator.setNativeNavigatorRecreationObserver(
                navigatorRecreationObserverSlot.captured,
            )
        }
    }

    @Test
    fun `doesn't add message observer on init`() {
        verify(exactly = 0) {
            navigator.setAdasisMessageCallback(any(), any())
        }
    }

    @Test
    fun `test setAdasisMessageCallback`() {
        val callback = mockk<AdasisV2MessageObserver>(relaxed = true)
        adasisManager.setAdasisMessageObserver(TEST_ADASIS_CONFIG, callback)

        verify(exactly = 1) {
            navigator.setAdasisMessageCallback(
                nativeAdasisMessageCallbackSlot.captured,
                TEST_ADASIS_CONFIG.toNativeAdasisConfig(),
            )
        }

        verify(exactly = 1) {
            callback.onMessage(eq(TEST_MESSAGE_BUFFER))
        }
    }

    @Test
    fun `forwards resetAdasisMessageCallback to native navigator`() {
        adasisManager.resetAdasisMessageObserver()

        verify(exactly = 1) {
            navigator.resetAdasisMessageCallback()
        }
    }

    @Test
    fun `forwards getAdasisEdgeAttributes call to native navigator`() {
        val edgeId = 123L

        adasisManager.getAdasisEdgeAttributes(edgeId)

        verify(exactly = 1) {
            graphAccessor.getAdasAttributes(edgeId)
        }
    }

    @Test
    fun `re-adds message observer when navigator recreated if observer was registered`() {
        adasisManager.setAdasisMessageObserver(TEST_ADASIS_CONFIG, mockk(relaxed = true))

        clearMocks(navigator)

        navigatorRecreationObserverSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 1) {
            navigator.setAdasisMessageCallback(
                nativeAdasisMessageCallbackSlot.captured,
                TEST_ADASIS_CONFIG.toNativeAdasisConfig(),
            )
        }
    }

    @Test
    fun `doesn't add message observer when navigator recreated if observer was not registered`() {
        clearMocks(navigator)

        navigatorRecreationObserverSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 0) {
            navigator.setAdasisMessageCallback(any(), any())
        }
    }

    @Test
    fun `doesn't add message observer when navigator recreated if observer was reset`() {
        val callback = mockk<AdasisV2MessageObserver>(relaxed = true)
        adasisManager.setAdasisMessageObserver(TEST_ADASIS_CONFIG, callback)
        adasisManager.resetAdasisMessageObserver()

        clearMocks(navigator)

        navigatorRecreationObserverSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 0) {
            navigator.setAdasisMessageCallback(any(), any())
        }
    }

    private companion object {

        val TEST_MESSAGE_BUFFER = listOf<Byte>(1, 2, 3)

        val TEST_DATA_SENDING_CONFIG = AdasisDataSendingConfig.Builder(
            AdasisMessageBinaryFormat.FlatBuffers,
        ).build()

        val TEST_ADASIS_CONFIG = AdasisConfig.Builder(TEST_DATA_SENDING_CONFIG).build()
    }
}
