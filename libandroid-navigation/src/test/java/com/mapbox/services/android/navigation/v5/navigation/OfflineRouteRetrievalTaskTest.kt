package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouterResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class OfflineRouteRetrievalTaskTest {

    @Test
    fun checksOnErrorIsCalledIfRouteIsNotFetched() {
        val mockedNavigator = mock(Navigator::class.java)
        val mockedCallback = mock(OnOfflineRouteFoundCallback::class.java)
        val mockedResult = mock(RouterResult::class.java)
        `when`(mockedResult.json).thenReturn("{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No " + "suitable edges near location\", \"error_code\": 171}")
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback, mockedResult
        )
        val nullRoute: DirectionsRoute? = null

        theOfflineRouteRetrievalTask.onPostExecute(nullRoute)

        verify(mockedCallback).onError(any(OfflineError::class.java))
    }

    @Test
    fun checksErrorMessageIsWellFormedIfRouteIsNotFetched() {
        val mockedNavigator = mock(Navigator::class.java)
        val mockedCallback = mock(OnOfflineRouteFoundCallback::class.java)
        val mockedResult = mock(RouterResult::class.java)
        `when`(mockedResult.json).thenReturn("{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No " + "suitable edges near location\", \"error_code\": 171}")
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback, mockedResult
        )
        val nullRoute: DirectionsRoute? = null
        val offlineError = ArgumentCaptor.forClass(OfflineError::class.java)

        theOfflineRouteRetrievalTask.onPostExecute(nullRoute)

        verify(mockedCallback).onError(offlineError.capture())
        assertEquals(
            "Error occurred fetching offline route: No suitable edges near location - Code: 171",
            offlineError.value.message
        )
    }

    @Test
    fun checksOnRouteFoundIsCalledIfRouteIsFetched() {
        val mockedNavigator = mock(Navigator::class.java)
        val mockedCallback = mock(OnOfflineRouteFoundCallback::class.java)
        val theOfflineRouteRetrievalTask = OfflineRouteRetrievalTask(
            mockedNavigator,
            mockedCallback
        )
        val aRoute = mock(DirectionsRoute::class.java)

        theOfflineRouteRetrievalTask.onPostExecute(aRoute)

        verify(mockedCallback).onRouteFound(eq(aRoute))
    }
}
