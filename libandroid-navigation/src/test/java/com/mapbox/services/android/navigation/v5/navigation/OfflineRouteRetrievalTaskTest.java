package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OfflineRouteRetrievalTaskTest {

  @Test
  public void checksOnErrorIsCalledIfRouteIsNotFetched() {
    Navigator mockedNavigator = mock(Navigator.class);
    OnOfflineRouteFoundCallback mockedCallback = mock(OnOfflineRouteFoundCallback.class);
    RouterResult mockedResult = mock(RouterResult.class);
    when(mockedResult.getJson()).thenReturn("{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No " +
      "suitable edges near location\", \"error_code\": 171}");
    OfflineRouteRetrievalTask theOfflineRouteRetrievalTask = new OfflineRouteRetrievalTask(mockedNavigator,
      mockedCallback, mockedResult);
    DirectionsRoute nullRoute = null;

    theOfflineRouteRetrievalTask.onPostExecute(nullRoute);

    verify(mockedCallback).onError(any(OfflineError.class));
  }

  @Test
  public void checksErrorMessageIsWellFormedIfRouteIsNotFetched() {
    Navigator mockedNavigator = mock(Navigator.class);
    OnOfflineRouteFoundCallback mockedCallback = mock(OnOfflineRouteFoundCallback.class);
    RouterResult mockedResult = mock(RouterResult.class);
    when(mockedResult.getJson()).thenReturn("{\"status\": \"Bad Request\", \"status_code\": 400, \"error\": \"No " +
      "suitable edges near location\", \"error_code\": 171}");
    OfflineRouteRetrievalTask theOfflineRouteRetrievalTask = new OfflineRouteRetrievalTask(mockedNavigator,
      mockedCallback, mockedResult);
    DirectionsRoute nullRoute = null;
    ArgumentCaptor<OfflineError> offlineError = ArgumentCaptor.forClass(OfflineError.class);

    theOfflineRouteRetrievalTask.onPostExecute(nullRoute);

    verify(mockedCallback).onError(offlineError.capture());
    assertEquals("Error occurred fetching offline route: No suitable edges near location - Code: 171",
      offlineError.getValue().getMessage());
  }

  @Test
  public void checksOnRouteFoundIsCalledIfRouteIsFetched() {
    Navigator mockedNavigator = mock(Navigator.class);
    OnOfflineRouteFoundCallback mockedCallback = mock(OnOfflineRouteFoundCallback.class);
    OfflineRouteRetrievalTask theOfflineRouteRetrievalTask = new OfflineRouteRetrievalTask(mockedNavigator,
      mockedCallback);
    DirectionsRoute aRoute = mock(DirectionsRoute.class);

    theOfflineRouteRetrievalTask.onPostExecute(aRoute);

    verify(mockedCallback).onRouteFound(eq(aRoute));
  }
}