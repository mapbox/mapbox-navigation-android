package com.mapbox.navigation.route.hybrid

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxHybridRouterGenerationTest {

    private lateinit var hybridRouter: MapboxHybridRouter

    private val onboardRouter: MapboxOnboardRouter = mockk()
    private val offboardRouter: MapboxOffboardRouter = mockk(relaxed = true)
    private val context: Context = mockk()
    private val connectivityManager: ConnectivityManager = mockk()
    private val intent: Intent = mockk()

    @Before
    fun setUp() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } answers { connectivityManager }
        every { context.registerReceiver(any(), any()) } answers { intent }
        hybridRouter = MapboxHybridRouter(onboardRouter, offboardRouter, context)
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(hybridRouter)
    }
}
