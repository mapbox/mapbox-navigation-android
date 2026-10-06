package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.common.dispatchers.SdkDispatchersTestRule
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GeoDeeplinkGeocodingTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    // IO work is queued until the test runs it, while the callers run eagerly on main, so both
    // requests are started before either reaches the IO dispatcher.
    private val ioDispatcher = StandardTestDispatcher()

    @get:Rule
    val sdkDispatchersRule = SdkDispatchersTestRule(
        dispatcher = ioDispatcher,
        mainDispatcher = UnconfinedTestDispatcher(ioDispatcher.scheduler),
    )

    private val responses = mutableMapOf<String, GeocodingResponse>()
    private val requests = mutableMapOf<String, MapboxGeocoding>()

    @Before
    fun setup() {
        mockkStatic(MapboxGeocoding::class)
        every { MapboxGeocoding.builder() } answers {
            val querySlot = slot<String>()
            val builder = mockk<MapboxGeocoding.Builder>()
            every { builder.accessToken(any()) } returns builder
            every { builder.query(capture(querySlot)) } returns builder
            every { builder.build() } answers { request(querySlot.captured) }
            builder
        }
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `a newer request cancels the older one and resolves its own query`() =
        runTest(sdkDispatchersRule.mainDispatcher) {
            // The token constructor keeps the test away from the native MapboxOptions.
            @Suppress("DEPRECATION")
            val geocoding = GeoDeeplinkGeocoding("token")

            val first = async { geocoding.requestPlaces(geoDeeplink("first"), null) }
            val second = async { geocoding.requestPlaces(geoDeeplink("second"), null) }
            advanceUntilIdle()

            // Before the fix, the first call executed the second call's request, returning the
            // second places to the first caller and failing the second with "Already executed".
            assertNull(first.await())
            assertSame(responses.getValue("second"), second.await())
            verify(exactly = 1) { requests.getValue("first").enqueueCall(any()) }
            verify(exactly = 1) { requests.getValue("second").enqueueCall(any()) }
        }

    private fun geoDeeplink(query: String): GeoDeeplink = mockk {
        every { point } returns null
        every { placeQuery } returns query
    }

    // Mirrors MapboxService: cancelCall() cancels the call even before it is enqueued, and a
    // cancelled call fails when it is enqueued.
    private fun request(query: String): MapboxGeocoding {
        val response = mockk<GeocodingResponse>()
        var cancelled = false
        val request = mockk<MapboxGeocoding>(relaxed = true) {
            every { cancelCall() } answers { cancelled = true }
            every { enqueueCall(any()) } answers {
                val callback = firstArg<Callback<GeocodingResponse>>()
                if (cancelled) {
                    callback.onFailure(mockk(), IOException("Canceled"))
                } else {
                    callback.onResponse(mockk(), Response.success(response))
                }
            }
        }
        responses[query] = response
        requests[query] = request
        return request
    }
}
