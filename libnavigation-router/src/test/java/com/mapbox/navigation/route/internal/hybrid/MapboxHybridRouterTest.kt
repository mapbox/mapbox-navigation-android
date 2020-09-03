package com.mapbox.navigation.route.internal.hybrid

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.utils.internal.NetworkStatus
import com.mapbox.navigation.utils.internal.NetworkStatusService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxHybridRouterTest {

    private lateinit var hybridRouter: MapboxHybridRouter
    private val onboardRouter: Router = mockk(relaxUnitFun = true)
    private val offboardRouter: Router = mockk(relaxUnitFun = true)
    private val context: Context = mockk(relaxUnitFun = true)
    private val connectivityManager: ConnectivityManager = mockk(relaxUnitFun = true)
    private val routerCallback: Router.Callback = mockk(relaxUnitFun = true)
    private val routerOptions: RouteOptions = provideDefaultRouteOptions()
    private val internalCallback = slot<Router.Callback>()
    private val networkStatusService: NetworkStatusService = mockk(relaxUnitFun = true)
    private val channel = Channel<NetworkStatus>(Channel.CONFLATED)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { context.registerReceiver(any(), any()) } returns Intent()
        every { onboardRouter.getRoute(routerOptions, capture(internalCallback)) } answers {}
        every { offboardRouter.getRoute(routerOptions, capture(internalCallback)) } answers {}
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { networkStatusService.getNetworkStatusChannel() } returns channel

        hybridRouter = MapboxHybridRouter(onboardRouter, offboardRouter, networkStatusService)
    }

    @Test
    fun whenNetworkConnectedOffboardRouterUsed() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
    }

    @Test
    fun offboardRouterCanceled() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)
        internalCallback.captured.onCanceled()

        verify(exactly = 1) { routerCallback.onCanceled() }
    }

    @Test
    fun whenNoNetworkConnectionOnboardRouterUsed() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
    }

    @Test
    fun onboardRouterCanceled() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)
        internalCallback.captured.onCanceled()

        verify(exactly = 1) { routerCallback.onCanceled() }
    }

    @Test
    fun whenOffboardRouterFailsOnboardRouterIsCalled() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalCallback.captured.onFailure(Throwable())
        internalCallback.captured.onResponse(emptyList())

        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { routerCallback.onResponse(any()) }
    }

    @Test
    fun whenOnboardRouterFailsOffboardRouterIsCalled() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalCallback.captured.onFailure(Throwable())
        internalCallback.captured.onResponse(emptyList())

        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, internalCallback.captured) }
        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, internalCallback.captured) }
        verify(exactly = 1) { routerCallback.onResponse(any()) }
    }

    @Test
    fun whenOffboardRouterFailsOnboardRouterIsCalledAndOffboardUsedAgain() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalCallback.captured.onFailure(Throwable())
        internalCallback.captured.onResponse(emptyList())

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalCallback.captured.onResponse(emptyList())

        verify(exactly = 2) { offboardRouter.getRoute(routerOptions, internalCallback.captured) }
        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, internalCallback.captured) }
        verify(exactly = 2) { routerCallback.onResponse(any()) }
    }

    @Test
    fun whenOnboardRouterFailsOffboardRouterIsCalledAndOnboardUsedAgain() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalCallback.captured.onFailure(Throwable())
        internalCallback.captured.onResponse(emptyList())

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalCallback.captured.onResponse(emptyList())

        verify(exactly = 2) { onboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 2) { routerCallback.onResponse(any()) }
    }

    @Test
    fun whenConnectionAppearedRoutersSwitched() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
    }

    @Test
    fun whenConnectionDisappearedRoutersSwitched() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
    }

    @Test
    fun networkStatusService_cleanup_calledOnChannelClose() = runBlocking {
        channel.close()

        hybridRouter.networkStatusJob.join()

        verify(exactly = 1) { networkStatusService.cleanup() }
    }

    @Test
    fun whenMultipleRoutesFetchedCallbacksHandledCorrectly() = runBlocking {
        enableNetworkConnection()

        val originalCallback: Router.Callback = mockk(relaxUnitFun = true)
        val additionalCallbackFirst: Router.Callback = mockk(relaxUnitFun = true)
        val additionalCallbackSecond: Router.Callback = mockk(relaxUnitFun = true)
        val additionalCallbackThird: Router.Callback = mockk(relaxUnitFun = true)

        val originalResult = listOf<DirectionsRoute>(
            DirectionsRoute.builder().distance(.0).duration(.0).build()
        )

        val additionalResultFirst = emptyList<DirectionsRoute>()
        val additionalResultSecond = emptyList<DirectionsRoute>()
        val additionalResultThird = emptyList<DirectionsRoute>()

        hybridRouter.getRoute(routerOptions, originalCallback)
        hybridRouter.getRoute(routerOptions, additionalCallbackFirst)
        hybridRouter.getRoute(routerOptions, additionalCallbackSecond)
        hybridRouter.getRoute(routerOptions, additionalCallbackThird)

        internalCallback.captured.onResponse(originalResult)
        internalCallback.captured.onResponse(additionalResultFirst)
        internalCallback.captured.onResponse(additionalResultSecond)
        internalCallback.captured.onResponse(additionalResultThird)

        verify(exactly = 1) { originalCallback.onResponse(originalResult) }
        verify(exactly = 1) { additionalCallbackFirst.onResponse(additionalResultFirst) }
        verify(exactly = 1) { additionalCallbackSecond.onResponse(additionalResultSecond) }
        verify(exactly = 1) { additionalCallbackThird.onResponse(additionalResultThird) }
    }

    private suspend fun enableNetworkConnection() = networkConnected(true)

    private suspend fun disableNetworkConnection() = networkConnected(false)

    private suspend fun networkConnected(networkConnected: Boolean) {
        hybridRouter.onNetworkStatusChanged(NetworkStatus(networkConnected))
    }

    private fun provideDefaultRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultParams()
            .apply {
                accessToken("")
                coordinates(Point.fromLngLat(.0, .0), null, Point.fromLngLat(.0, .0))
            }.build()
    }
}
