package com.mapbox.navigation.route.internal.hybrid

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.ConnectivityHandler
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxHybridRouterTest {

    private lateinit var hybridRouter: MapboxHybridRouter
    private val onboardRouter: Router = mockk(relaxUnitFun = true)
    private val offboardRouter: Router = mockk(relaxUnitFun = true)
    private val context: Context = mockk(relaxUnitFun = true)
    private val connectivityManager: ConnectivityManager = mockk(relaxUnitFun = true)
    private val routerCallback: RouterCallback = mockk(relaxUnitFun = true)
    private val routerOptions: RouteOptions = provideDefaultRouteOptions()
    private val internalOffboardCallback = slot<RouterCallback>()
    private val internalOnboardCallback = slot<RouterCallback>()
    private val networkStatusService: ConnectivityHandler = mockk(relaxUnitFun = true)
    private val channel = Channel<Boolean>(Channel.CONFLATED)
    private val internalOffboardRefreshCallback = slot<RouteRefreshCallback>()
    private val internalOnboardRefreshCallback = slot<RouteRefreshCallback>()

    @Before
    fun setUp() {
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockk(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { context.registerReceiver(any(), any()) } returns Intent()
        every { onboardRouter.getRoute(routerOptions, capture(internalOnboardCallback)) } returns 1L
        every {
            offboardRouter.getRoute(
                routerOptions,
                capture(internalOffboardCallback)
            )
        } returns 2L
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { networkStatusService.getNetworkStatusChannel() } returns channel
        every {
            offboardRouter.getRouteRefresh(
                any(),
                any(),
                capture(internalOffboardRefreshCallback)
            )
        } returns 3L
        every {
            onboardRouter.getRouteRefresh(
                any(),
                any(),
                capture(internalOnboardRefreshCallback)
            )
        } returns 4L

        hybridRouter = MapboxHybridRouter(
            onboardRouter,
            offboardRouter,
            networkStatusService
        )
    }

    @Test
    fun whenNetworkConnectedOffboardRouterUsed() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
    }

    @Test
    fun `offboardRouterCanceled and request list is clear`() = runBlocking {
        enableNetworkConnection()

        val id = hybridRouter.getRoute(routerOptions, routerCallback)
        internalOffboardCallback.captured.onCanceled(routerOptions, RouterOrigin.Offboard)

        verify(exactly = 1) { routerCallback.onCanceled(routerOptions, RouterOrigin.Offboard) }

        hybridRouter.cancelRouteRequest(id)
        verify(exactly = 0) { offboardRouter.cancelRouteRequest(any()) }
    }

    @Test
    fun whenNoNetworkConnectionOnboardRouterUsed() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
    }

    @Test
    fun `onboardRouterCanceled and request list is clear`() = runBlocking {
        disableNetworkConnection()

        val id = hybridRouter.getRoute(routerOptions, routerCallback)
        internalOnboardCallback.captured.onCanceled(routerOptions, RouterOrigin.Onboard)

        verify(exactly = 1) { routerCallback.onCanceled(routerOptions, RouterOrigin.Onboard) }

        hybridRouter.cancelRouteRequest(id)
        verify(exactly = 0) { onboardRouter.cancelRouteRequest(any()) }
    }

    @Test
    fun `whenOffboardRouterFailsOnboardRouterIsCalled and request list is clear`() = runBlocking {
        enableNetworkConnection()

        val id = hybridRouter.getRoute(routerOptions, routerCallback)

        internalOffboardCallback.captured.onFailure(listOf(mockk(relaxed = true)), routerOptions)
        internalOnboardCallback.captured.onRoutesReady(emptyList(), RouterOrigin.Onboard)

        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { routerCallback.onRoutesReady(any(), RouterOrigin.Onboard) }

        hybridRouter.cancelRouteRequest(id)
        verify(exactly = 0) { offboardRouter.cancelRouteRequest(any()) }
        verify(exactly = 0) { onboardRouter.cancelRouteRequest(any()) }
    }

    @Test
    fun `whenOnboardRouterFailsOffboardRouterIsCalled and request list is clear`() = runBlocking {
        disableNetworkConnection()

        val id = hybridRouter.getRoute(routerOptions, routerCallback)

        internalOnboardCallback.captured.onFailure(listOf(mockk(relaxed = true)), routerOptions)
        internalOffboardCallback.captured.onRoutesReady(emptyList(), RouterOrigin.Offboard)

        verify(exactly = 1) {
            onboardRouter.getRoute(
                routerOptions,
                internalOnboardCallback.captured
            )
        }
        verify(exactly = 1) {
            offboardRouter.getRoute(
                routerOptions,
                internalOffboardCallback.captured
            )
        }
        verify(exactly = 1) { routerCallback.onRoutesReady(any(), RouterOrigin.Offboard) }

        hybridRouter.cancelRouteRequest(id)
        verify(exactly = 0) { offboardRouter.cancelRouteRequest(any()) }
        verify(exactly = 0) { onboardRouter.cancelRouteRequest(any()) }
    }

    @Test
    fun whenOffboardRouterFailsOnboardRouterIsCalledAndOffboardUsedAgain() = runBlocking {
        enableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalOffboardCallback.captured.onFailure(listOf(mockk(relaxed = true)), routerOptions)
        internalOnboardCallback.captured.onRoutesReady(emptyList(), RouterOrigin.Onboard)

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalOffboardCallback.captured.onRoutesReady(emptyList(), RouterOrigin.Offboard)

        verify(exactly = 2) { offboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { onboardRouter.getRoute(routerOptions, any()) }
        verify(ordering = Ordering.SEQUENCE) {
            routerCallback.onRoutesReady(any(), RouterOrigin.Onboard)
            routerCallback.onRoutesReady(any(), RouterOrigin.Offboard)
        }
        verify(exactly = 2) { routerCallback.onRoutesReady(any(), any()) }
    }

    @Test
    fun whenOnboardRouterFailsOffboardRouterIsCalledAndOnboardUsedAgain() = runBlocking {
        disableNetworkConnection()

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalOnboardCallback.captured.onFailure(listOf(mockk(relaxed = true)), routerOptions)
        internalOffboardCallback.captured.onRoutesReady(emptyList(), RouterOrigin.Offboard)

        hybridRouter.getRoute(routerOptions, routerCallback)

        internalOnboardCallback.captured.onRoutesReady(emptyList(), RouterOrigin.Onboard)

        verify(exactly = 2) { onboardRouter.getRoute(routerOptions, any()) }
        verify(exactly = 1) { offboardRouter.getRoute(routerOptions, any()) }
        verify(ordering = Ordering.SEQUENCE) {
            routerCallback.onRoutesReady(any(), RouterOrigin.Offboard)
            routerCallback.onRoutesReady(any(), RouterOrigin.Onboard)
        }
        verify(exactly = 2) { routerCallback.onRoutesReady(any(), any()) }
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
    fun `route request failure and request list is clear`() = runBlocking {
        enableNetworkConnection()

        val id = hybridRouter.getRoute(routerOptions, routerCallback)

        val url1 = mockk<URL>()
        val throwable1 = mockk<Throwable>()
        val routerFailurePrimary = RouterFailure(
            url = url1,
            message = "message1",
            routerOrigin = RouterOrigin.Offboard,
            code = 1,
            throwable = throwable1
        )

        val url2 = mockk<URL>()
        val throwable2 = mockk<Throwable>()
        val routerFailureFallback = RouterFailure(
            url = url2,
            message = "message2",
            routerOrigin = RouterOrigin.Onboard,
            code = 2,
            throwable = throwable2
        )

        internalOffboardCallback.captured.onFailure(listOf(routerFailurePrimary), routerOptions)
        internalOnboardCallback.captured.onFailure(listOf(routerFailureFallback), routerOptions)

        val expected = listOf(
            RouterFailure(
                url = url1,
                routerOrigin = RouterOrigin.Offboard,
                message = "Primary router " +
                    "(${offboardRouter::class.java.canonicalName}), " +
                    "origin ${RouterOrigin.Offboard}, " +
                    "failed with: message1",
                code = 1,
                throwable = throwable1
            ),
            RouterFailure(
                url = url2,
                routerOrigin = RouterOrigin.Onboard,
                message = "Fallback router " +
                    "(${onboardRouter::class.java.canonicalName}), " +
                    "origin ${RouterOrigin.Onboard}, " +
                    "failed with: message2",
                code = 2,
                throwable = throwable2
            )
        )
        verify(exactly = 1) { routerCallback.onFailure(expected, routerOptions) }

        hybridRouter.cancelRouteRequest(id)
        verify(exactly = 0) { offboardRouter.cancelRouteRequest(any()) }
        verify(exactly = 0) { onboardRouter.cancelRouteRequest(any()) }
    }

    @Test
    fun `cancel a specific refresh request when multiple are running`() = runBlocking {
        enableNetworkConnection()

        var offboardRequestIds = 0
        every { offboardRouter.getRoute(any(), any()) } answers {
            (++offboardRequestIds).toLong()
        }
        val firstId = hybridRouter.getRoute(routerOptions, routerCallback)
        val secondId = hybridRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 0) { offboardRouter.cancelRouteRequest(any()) }
        verify(exactly = 0) { onboardRouter.cancelRouteRequest(any()) }

        disableNetworkConnection()

        var onboardRequestIds = 11
        every { onboardRouter.getRoute(any(), any()) } answers {
            (++onboardRequestIds).toLong()
        }
        val thirdId = hybridRouter.getRoute(routerOptions, routerCallback)
        val forthId = hybridRouter.getRoute(routerOptions, routerCallback)

        hybridRouter.cancelRouteRequest(firstId)
        hybridRouter.cancelRouteRequest(thirdId)
        hybridRouter.cancelRouteRequest(secondId)
        hybridRouter.cancelRouteRequest(forthId)

        verifySequence {
            offboardRouter.getRoute(routerOptions, any())
            offboardRouter.getRoute(routerOptions, any())
            onboardRouter.getRoute(routerOptions, any())
            onboardRouter.getRoute(routerOptions, any())

            offboardRouter.cancelRouteRequest(1L)
            onboardRouter.cancelRouteRequest(12L)
            offboardRouter.cancelRouteRequest(2L)
            onboardRouter.cancelRouteRequest(13L)
        }
    }

    @Test
    fun `offboard route refresh is successful and request list is clear`() = runBlocking {
        enableNetworkConnection()
        val original = mockk<DirectionsRoute>()
        val resulting = mockk<DirectionsRoute>()
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)

        val id = hybridRouter.getRouteRefresh(original, 0, callback)
        internalOffboardRefreshCallback.captured.onRefresh(resulting)

        verify(exactly = 1) { offboardRouter.getRouteRefresh(original, 0, any()) }
        verify(exactly = 1) { callback.onRefresh(resulting) }

        hybridRouter.cancelRouteRefreshRequest(id)
        verify(exactly = 0) { offboardRouter.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `route refresh fails and request list is clear`() = runBlocking {
        enableNetworkConnection()
        val original = mockk<DirectionsRoute> {
            every { routeOptions() } returns mockk {
                every { requestUuid() } returns "uuid"
            }
        }
        val error = mockk<RouteRefreshError> {
            every { message } returns "mock"
            every { throwable } returns mockk(relaxed = true)
        }
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)

        val id = hybridRouter.getRouteRefresh(original, 0, callback)
        internalOffboardRefreshCallback.captured.onError(error)
        internalOnboardRefreshCallback.captured.onError(error)

        verify(exactly = 1) { offboardRouter.getRouteRefresh(original, 0, any()) }
        verify(exactly = 1) { callback.onError(any()) }

        hybridRouter.cancelRouteRefreshRequest(id)
        verify(exactly = 0) { offboardRouter.cancelRouteRefreshRequest(any()) }
        verify(exactly = 0) { onboardRouter.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `onboard route refresh is successful and request list is clear`() = runBlocking {
        disableNetworkConnection()
        val original = mockk<DirectionsRoute>()
        val resulting = mockk<DirectionsRoute>()
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)

        val id = hybridRouter.getRouteRefresh(original, 0, callback)
        internalOnboardRefreshCallback.captured.onRefresh(resulting)

        verify(exactly = 1) { onboardRouter.getRouteRefresh(original, 0, any()) }
        verify(exactly = 1) { callback.onRefresh(resulting) }

        hybridRouter.cancelRouteRefreshRequest(id)
        verify(exactly = 0) { onboardRouter.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `when network connection is re-established, offboard is used for refresh again`() =
        runBlocking {
            disableNetworkConnection()
            enableNetworkConnection()
            val original = mockk<DirectionsRoute>()
            val resulting = mockk<DirectionsRoute>()
            val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)

            hybridRouter.getRouteRefresh(original, 0, callback)
            internalOffboardRefreshCallback.captured.onRefresh(resulting)

            verify(exactly = 1) { offboardRouter.getRouteRefresh(original, 0, any()) }
            verify(exactly = 1) { callback.onRefresh(resulting) }
        }

    @Test
    fun `cancel all`() = runBlocking {
        var requestIds = 100
        every { offboardRouter.getRoute(any(), any()) } answers {
            (++requestIds).toLong()
        }
        every { onboardRouter.getRoute(any(), any()) } answers {
            (++requestIds).toLong()
        }
        every { offboardRouter.getRouteRefresh(any(), any(), any()) } answers {
            (++requestIds).toLong()
        }
        every { onboardRouter.getRouteRefresh(any(), any(), any()) } answers {
            (++requestIds).toLong()
        }

        enableNetworkConnection()
        hybridRouter.getRoute(mockk(relaxed = true), mockk(relaxed = true))
        hybridRouter.getRoute(mockk(relaxed = true), mockk(relaxed = true))
        hybridRouter.getRouteRefresh(mockk(relaxed = true), 0, mockk(relaxed = true))

        disableNetworkConnection()
        hybridRouter.getRoute(mockk(relaxed = true), mockk(relaxed = true))
        hybridRouter.getRoute(mockk(relaxed = true), mockk(relaxed = true))
        hybridRouter.getRouteRefresh(mockk(relaxed = true), 0, mockk(relaxed = true))

        hybridRouter.cancelAll()

        verify { offboardRouter.cancelAll() }
        verify { onboardRouter.cancelAll() }
    }

    @After
    fun cleanUp() {
        unmockkObject(LoggerProvider)
    }

    private suspend fun enableNetworkConnection() = networkConnected(true)

    private suspend fun disableNetworkConnection() = networkConnected(false)

    private suspend fun networkConnected(networkConnected: Boolean) {
        hybridRouter.onNetworkStatusChanged(networkConnected)
    }

    private fun provideDefaultRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .apply {
                coordinates(Point.fromLngLat(.0, .0), null, Point.fromLngLat(.0, .0))
            }.build()
    }
}
