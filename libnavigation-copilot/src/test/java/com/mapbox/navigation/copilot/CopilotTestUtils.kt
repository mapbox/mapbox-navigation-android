package com.mapbox.navigation.copilot

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

internal object CopilotTestUtils {

    fun prepareLifecycleOwnerMockk(): LifecycleOwner {
        mockkStatic(MapboxNavigationApp::class)
        every { MapboxNavigationApp.isSetup() } returns true
        val mockedLifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
        val mockedLifecycle = mockk<Lifecycle>(relaxed = true)
        every { mockedLifecycleOwner.lifecycle } returns mockedLifecycle
        every { MapboxNavigationApp.lifecycleOwner } returns mockedLifecycleOwner
        return mockedLifecycleOwner
    }
}
