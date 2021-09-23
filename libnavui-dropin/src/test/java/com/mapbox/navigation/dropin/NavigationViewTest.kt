package com.mapbox.navigation.dropin

import android.util.DisplayMetrics
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File

// @RunWith(RobolectricTestRunner::class)
// @Config(shadows = [ShadowLogger::class])
class NavigationViewTest {

    val ctx = mockk<FragmentActivity>(relaxUnitFun = true)
    val mockDisplayMetrics = mockk<DisplayMetrics>(relaxUnitFun = true)

    @Before
    fun setUp() {
        every { ctx.getString(-1) } returns "token"
        every {
            ctx.resources.getIdentifier(
                "mapbox_access_token",
                "string",
                "com.mapbox.maps"
            )
        } returns -1
        every { ctx.packageName } returns "com.mapbox.maps"
        every { ctx.filesDir } returns File("foobar")
        every { ctx.resources.displayMetrics } returns mockDisplayMetrics
        mockDisplayMetrics.density = 1f
    }

    @Ignore
    @Test
    fun addRouteProgressObserver() {
        // mockkStatic(MapboxLayoutDropInViewBinding::class)
        // mockkStatic(LayoutInflater::class)
        // val mockLifecycle = mockk<Lifecycle>(relaxed = true)
        // val binding = mockk<MapboxLayoutDropInViewBinding>()
        // val mockInflater = mockk<LayoutInflater>()
        // every { MapboxLayoutDropInViewBinding.inflate(any(), any()) } returns binding
        // every { LayoutInflater.from(ctx) } returns mockInflater
        // every { ctx.lifecycle } returns mockLifecycle
        // val observer = mockk<RouteProgressObserver>()
        //
        // val navigationView = NavigationView(
        //     ctx,
        //     null,
        //     "token",
        //     MapInitOptions(ctx),
        //     NavigationViewOptions.Builder().build()
        // )
        //
        // navigationView.addRouteProgressObserver(observer)
        //
        // assertEquals(1, navigationView.externalRouteProgressObservers.size)
        // unmockkStatic(LayoutInflater::class)
        // unmockkStatic(MapboxLayoutDropInViewBinding::class)
    }
}
