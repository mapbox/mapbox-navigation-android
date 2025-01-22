package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createRouteOptions
import junit.framework.TestCase.assertEquals
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Rule
import org.junit.Test

class RerouteOptionsAdapterWrapperTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @Test
    fun `url is adopted`() {
        val adapter = createAdapterWrapper(
            createVoiceInstructionEnableAdapter(),
        )
        val originalRouteOptions = createRouteOptions()
            .toBuilder()
            .voiceInstructions(false)
            .build()
            .toUrl("test-token")
            .toString()

        val modified = adapter.modifyRouteRequestOptions(originalRouteOptions)

        assertEquals(
            "true",
            modified.toHttpUrlOrNull()?.queryParameter("voice_instructions"),
        )
    }

    @Test
    fun `request url isn't changed`() {
        val adapter = createAdapterWrapper(
            createPassThroughAdapter(),
        )
        val originalRouteOptions = createRouteOptions().toUrl("***").toString()

        val modified = adapter.modifyRouteRequestOptions(originalRouteOptions)

        assertEquals(originalRouteOptions, modified)
    }

    @Test
    fun `unparsable url isn't changed`() {
        val adapter = createAdapterWrapper(
            createVoiceInstructionEnableAdapter(),
        )
        val originalRouteOptions = "wrong url"

        val modified = adapter.modifyRouteRequestOptions(originalRouteOptions)

        assertEquals(originalRouteOptions, modified)
    }

    @Test
    fun `unparsable route options aren't changed`() {
        val adapter = createAdapterWrapper(
            createVoiceInstructionEnableAdapter(),
        )
        val originalRouteOptions = "https://api.mapbox.com/directions/v5/mapbox/driving"

        val modified = adapter.modifyRouteRequestOptions(originalRouteOptions)

        assertEquals(originalRouteOptions, modified)
    }
}

private fun createAdapterWrapper(
    wrappedAdapter: RerouteOptionsAdapter = createPassThroughAdapter(),
) =
    NativeMapboxRerouteController.RerouteOptionsAdapterWrapper(
        wrappedAdapter,
    )

private fun createPassThroughAdapter() = object : RerouteOptionsAdapter {
    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
        return routeOptions
    }
}

private fun createVoiceInstructionEnableAdapter() = object : RerouteOptionsAdapter {
    override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
        return routeOptions.toBuilder().voiceInstructions(true).build()
    }
}
