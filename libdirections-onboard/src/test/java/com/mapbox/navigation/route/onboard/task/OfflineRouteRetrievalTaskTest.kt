package com.mapbox.navigation.route.onboard.task

import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OnOfflineRouteFoundCallback
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigator.RouterResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineRouteRetrievalTaskTest {

    @Test
    fun checksOnErrorIsCalledIfRouteIsNotFetched() {
        val mockedNavigator = mockk<MapboxNativeNavigator>()
        val mockedCallback = mockk<OnOfflineRouteFoundCallback>(relaxed = true)
        val mockedResult = mockk<RouterResult>()
        every { mockedResult.json } returns "{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No suitable edges near location\", \"error_code\": 171}"
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback,
            mockedResult
        )
        val nullRoute = null

        theOfflineRouteRetrievalTask.onPostExecute(nullRoute)

        verify { mockedCallback.onError(any()) }
    }

    @Test
    fun checksErrorMessageIsWellFormedIfRouteIsNotFetched() {
        val mockedNavigator = mockk<MapboxNativeNavigator>()
        val mockedCallback = mockk<OnOfflineRouteFoundCallback>(relaxed = true)
        val slot = slot<OfflineError>()
        every { mockedCallback.onError(capture(slot)) } answers {}
        val mockedResult = mockk<RouterResult>()
        every { mockedResult.json } returns "{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No suitable edges near location\", \"error_code\": 171}"
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback,
            mockedResult
        )
        val nullRoute = null

        theOfflineRouteRetrievalTask.onPostExecute(nullRoute)

        verify { mockedCallback.onError(eq(slot.captured)) }
        assertEquals(
            "Error occurred fetching offline route: No suitable edges near location - Code: 171",
            slot.captured.message
        )
    }

    @Test
    fun checksOnRouteFoundIsCalledIfRouteIsFetched() {
        val mockedNavigator = mockk<MapboxNativeNavigator>()
        val mockedCallback = mockk<OnOfflineRouteFoundCallback>(relaxed = true)
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            null,
            mockedCallback
        )
        val routes = listOf<Route>(mockk())

        theOfflineRouteRetrievalTask.onPostExecute(routes)

        verify { mockedCallback.onRouteFound(eq(routes)) }
    }
}
