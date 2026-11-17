package com.mapbox.navigation.ui.utils.internal.configuration

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Test

class NavigationConfigOwnerTest {

    private val initialConfiguration = mockk<Configuration>()
    private val registeredCallbacks = slot<ComponentCallbacks>()
    private val context = mockk<Context> {
        every { resources } returns mockk {
            every { configuration } returns initialConfiguration
        }
        every { registerComponentCallbacks(capture(registeredCallbacks)) } just Runs
        every { unregisterComponentCallbacks(any()) } just Runs
    }

    @Test
    fun `registers component callbacks on creation`() {
        NavigationConfigOwner(context)

        verify(exactly = 1) { context.registerComponentCallbacks(any()) }
        verify(exactly = 0) { context.unregisterComponentCallbacks(any()) }
    }

    @Test
    fun `release unregisters the registered component callbacks`() {
        val owner = NavigationConfigOwner(context)

        owner.release()

        verify(exactly = 1) { context.unregisterComponentCallbacks(any()) }
        verify(exactly = 1) { context.unregisterComponentCallbacks(registeredCallbacks.captured) }
    }

    @Test
    fun `config emits configuration changes`() = runBlocking {
        val owner = NavigationConfigOwner(context)
        assertSame(initialConfiguration, owner.config().first())

        val newConfiguration = mockk<Configuration>()
        registeredCallbacks.captured.onConfigurationChanged(newConfiguration)

        assertSame(newConfiguration, owner.config().first())
    }
}
