package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.Navigator
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
        val mockedNavigator = mockk<Navigator>()
        val mockedCallback = mockk<OnOfflineRouteFoundCallback>(relaxed = true)
        val mockedResult = mockk<RouterResult>()
        every { mockedResult.json } returns "{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No suitable edges near location\", \"error_code\": 171}"
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback,
            mockedResult
        )
        val nullRoute: DirectionsRoute? = null

        theOfflineRouteRetrievalTask.onPostExecute(nullRoute)

        verify { mockedCallback.onError(any()) }
    }

    @Test
    fun checksErrorMessageIsWellFormedIfRouteIsNotFetched() {
        val mockedNavigator = mockk<Navigator>()
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
        val nullRoute: DirectionsRoute? = null

        theOfflineRouteRetrievalTask.onPostExecute(nullRoute)

        verify { mockedCallback.onError(eq(slot.captured)) }
        assertEquals(
            "Error occurred fetching offline route: No suitable edges near location - Code: 171",
            slot.captured.message
        )
    }

    @Test
    fun checksOnRouteFoundIsCalledIfRouteIsFetched() {
        val mockedNavigator = mockk<Navigator>()
        val mockedCallback = mockk<OnOfflineRouteFoundCallback>(relaxed = true)
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback
        )
        val aRoute = mockk<DirectionsRoute>()

        theOfflineRouteRetrievalTask.onPostExecute(aRoute)

        verify { mockedCallback.onRouteFound(eq(aRoute)) }
    }
}
