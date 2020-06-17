package com.mapbox.navigation.utils.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class NetworkStatusServiceTest {

    @Test
    fun cleanup() {
        val ctx = mockk<Context>(relaxUnitFun = true)
        val connectivityManager = mockk<ConnectivityManager>()
        val receiverIntent = mockk<Intent>()
        val receiverSlot = slot<BroadcastReceiver>()

        every { ctx.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { ctx.registerReceiver(capture(receiverSlot), any()) } returns receiverIntent

        NetworkStatusService(ctx).cleanup()

        verify(exactly = 1) { ctx.unregisterReceiver(receiverSlot.captured) }
    }
}
