package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.RerouteCallback
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test

class NativeRerouteControllerWrapperTest {

    private companion object {
        fun provideNativeRerouteControllerWrapper(
            accessToken: String = "pk.***",
            nativeRerouteController: RerouteControllerInterface = mockk(),
            nativeNavigator: MapboxNativeNavigator = mockk(),
        ): Pair<NativeRerouteControllerWrapper, MocksWrapper> {
            return NativeRerouteControllerWrapper(
                accessToken,
                nativeRerouteController,
                nativeNavigator,
            ) to MocksWrapper(
                nativeRerouteController,
                nativeNavigator,
            )
        }

        private class MocksWrapper(
            val nativeRerouteController: RerouteControllerInterface,
            val nativeNavigator: MapboxNativeNavigator,
        )
    }

    @Test
    fun `on reroute`() {
        data class Case(
            val description: String,
            val preAction: (NativeRerouteControllerWrapper) -> Unit,
            val postAction: (NativeRerouteControllerWrapper) -> Unit,
            val checks: (String, Expected<RerouteError, RerouteInfo>) -> Unit,
            val newUrl: String = "https:://any.url",
        )
        listOf(
            Case(
                "plain reroute",
                preAction = {},
                postAction = {},
                checks = { description, expected -> },
            ),
            run {
                val mockRerouteCallback: ((result: Expected<RerouteError, RerouteInfo>) -> Unit) =
                    mockk {
                        every { this@mockk.invoke(any()) } just runs
                    }
                Case(
                    "reroute invoke external reroute callback",
                    preAction = {
                        it.setRerouteCallbackListener(mockRerouteCallback)
                    },
                    postAction = {},
                    checks = { description, expected ->
                        verify(exactly = 1) { mockRerouteCallback.invoke(expected) }
                    },
                )
            },
            run {
                val newUrl = "https:://new-any-any.url"
                val mockNewRouteOptions: RouteOptions = mockk {
                    every { toUrl("pk.***") } returns mockk urlMock@{
                        every { this@urlMock.toString() } returns newUrl
                    }
                }
                val mockRerouteAdapter: RerouteOptionsAdapter = mockk {
                    every { onRouteOptions(any()) } returns mockNewRouteOptions
                }
                Case(
                    "reroute and reroute options adapter",
                    preAction = {
                        it.setRerouteOptionsAdapter(mockRerouteAdapter)
                    },
                    postAction = {},
                    checks = { description, expected ->
                        verify(exactly = 1) { mockRerouteAdapter.onRouteOptions(any()) }
                    },
                    newUrl,
                )
            },
        ).forEach { (description, preAction, postAction, checks, newUrl) ->
            mockkStatic(RouteOptions::class) {
                val mockkRouteOptions: RouteOptions = mockk {
                    every { toUrl("pk.***") } returns mockk urlMock@{
                        every { this@urlMock.toString() } returns newUrl
                    }
                }
                every { RouteOptions.fromUrl(any()) } returns mockkRouteOptions
                val (nativeRerouteControllerWrapper, mocksWrapper) =
                    provideNativeRerouteControllerWrapper()
                val mockResult: Expected<RerouteError, RerouteInfo> = mockk()
                every { mocksWrapper.nativeRerouteController.reroute(any(), any()) } answers {
                    secondArg<RerouteCallback>().run(mockResult)
                }
                val slotExpected = slot<Expected<RerouteError, RerouteInfo>>()
                val mockRerouteCallback = mockk<RerouteCallback>(relaxUnitFun = true) {
                    every { run(capture(slotExpected)) } just runs
                }

                preAction(nativeRerouteControllerWrapper)

                nativeRerouteControllerWrapper.reroute(newUrl, mockRerouteCallback)

                postAction(nativeRerouteControllerWrapper)

                checks(description, slotExpected.captured)

                assertEquals(description, mockResult, slotExpected.captured)
            }
        }
    }

    @Test
    fun `cancel reroute`() {
        val (nativeRerouteControllerWrapper, mocksWrapper) =
            provideNativeRerouteControllerWrapper()
        every { mocksWrapper.nativeRerouteController.cancel() } just runs

        nativeRerouteControllerWrapper.cancel()

        verify(exactly = 1) { mocksWrapper.nativeRerouteController.cancel() }
    }

    @Test
    fun addRerouteController() {
        val (nativeRerouteControllerWrapper, mocksWrapper) =
            provideNativeRerouteControllerWrapper()
        val rerouteObserver: RerouteObserver = mockk()
        every { mocksWrapper.nativeNavigator.addRerouteObserver(rerouteObserver) } just runs

        nativeRerouteControllerWrapper.addRerouteObserver(rerouteObserver)

        verify(exactly = 1) { mocksWrapper.nativeNavigator.addRerouteObserver(rerouteObserver) }
    }

    @Test
    fun forceReroute() {
        val (nativeRerouteControllerWrapper, mocksWrapper) =
            provideNativeRerouteControllerWrapper()
        every { mocksWrapper.nativeNavigator.getRerouteDetector().forceReroute() } just runs

        nativeRerouteControllerWrapper.forceReroute()

        verify(exactly = 1) { mocksWrapper.nativeNavigator.getRerouteDetector().forceReroute() }
    }
}
