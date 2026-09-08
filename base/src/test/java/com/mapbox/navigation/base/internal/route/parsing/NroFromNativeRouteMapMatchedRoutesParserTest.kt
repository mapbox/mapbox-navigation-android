@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing

import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.route.parsing.parser.mapmatching.NroFromNativeRouteMapMatchedRoutesParser
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigator.RouteInterface
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NroFromNativeRouteMapMatchedRoutesParserTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `native parse failure fails the whole parsing`() = runTest {
        val nnParser = nativeParserReturning(ExpectedFactory.createError("native error"))

        val result = createParser(nnParser).parseMapMatchedResponse(responseToParse())

        assertTrue(result.isFailure)
        assertTrue(
            "unexpected message: ${result.exceptionOrNull()?.message}",
            result.exceptionOrNull()?.message?.contains("native error") == true,
        )
    }

    @Test
    fun `response is parsed natively exactly once`() = runTest {
        val nnParser = nativeParserReturning(ExpectedFactory.createError("native boom"))

        createParser(nnParser).parseMapMatchedResponse(responseToParse())

        verify(exactly = 1) { nnParser.parseMapMatchedResponse(any<DataRef>(), any(), any()) }
        confirmVerified(nnParser)
    }

    @Test
    fun `response without matchings fails the whole parsing`() = runTest {
        val nnParser = nativeParserReturning(ExpectedFactory.createValue(emptyList()))

        val result = createParser(nnParser).parseMapMatchedResponse(responseToParse())

        assertTrue(result.isFailure)
        assertTrue(
            "unexpected message: ${result.exceptionOrNull()?.message}",
            result.exceptionOrNull()?.message?.contains("no routes returned") == true,
        )
    }

    private fun nativeParserReturning(result: Expected<String, List<RouteInterface>>) =
        mockk<SDKRouteParser> {
            every { parseMapMatchedResponse(any<DataRef>(), any(), any()) } returns result
        }

    private fun createParser(nnParser: SDKRouteParser) =
        NroFromNativeRouteMapMatchedRoutesParser(
            routeParsingTracking = noTracking(),
            parsingDispatcher = UnconfinedTestDispatcher(),
            nnParser = nnParser,
        )

    private fun responseToParse() = ResponseToParse(
        responseBody = "{}".toDataRefJava(),
        routeRequest = MAP_MATCHING_REQUEST,
        routerOrigin = RouterOrigin.ONLINE,
        responseOriginAPI = ResponseOriginAPI.MAP_MATCHING_API,
    )

    private companion object {
        const val MAP_MATCHING_REQUEST =
            "https://api.mapbox.com/matching/v5/mapbox/driving/" +
                "-117.17282%2C32.71204%3B-117.17288%2C32.71225" +
                "?access_token=***&steps=true&overview=full&geometries=polyline6"
    }
}
