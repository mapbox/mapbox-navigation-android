package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
    NavigationTelemetry navigationTelemetry = mock(NavigationTelemetry.class);
    RouteRetrievalInfo.Builder builder = mock(RouteRetrievalInfo.Builder.class);
    when(builder.route(any(DirectionsRoute.class))).thenReturn(builder);
    when(builder.numberOfRoutes(anyInt())).thenReturn(builder);
    when(builder.isOffline(anyBoolean())).thenReturn(builder);
    OfflineRouteRetrievalTask theOfflineRouteRetrievalTask = new OfflineRouteRetrievalTask(mockedNavigator,
      mockedCallback, navigationTelemetry, builder);
    DirectionsRoute aRoute = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(aRoute.routeOptions()).thenReturn(routeOptions);
    when(routeOptions.profile()).thenReturn("");

    theOfflineRouteRetrievalTask.onPostExecute(aRoute);

    verify(mockedCallback).onRouteFound(eq(aRoute));
  }

  @Test
  public void routeRetrievalEventSent() {
    Navigator mockedNavigator = mock(Navigator.class);
    OnOfflineRouteFoundCallback mockedCallback = mock(OnOfflineRouteFoundCallback.class);
    NavigationTelemetry navigationTelemetry = mock(NavigationTelemetry.class);
    RouteRetrievalInfo.Builder builder = mock(RouteRetrievalInfo.Builder.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    when(builder.route(any(DirectionsRoute.class))).thenReturn(builder);
    when(builder.numberOfRoutes(anyInt())).thenReturn(builder);
    when(builder.isOffline(anyBoolean())).thenReturn(builder);
    OfflineRouteRetrievalTask offlineRouteRetrievalTask = new OfflineRouteRetrievalTask(
      mockedNavigator, mockedCallback, navigationTelemetry, builder);

    offlineRouteRetrievalTask.onPostExecute(directionsRoute);

    verify(navigationTelemetry).routeRetrievalEvent(builder);
  }
}