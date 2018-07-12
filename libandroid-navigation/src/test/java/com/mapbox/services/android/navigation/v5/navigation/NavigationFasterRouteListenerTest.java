package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class NavigationFasterRouteListenerTest {

  @Test
  public void onResponseReceived_fasterRouteIsSentToDispatcher() {
    NavigationEventDispatcher eventDispatcher = mock(NavigationEventDispatcher.class);
    FasterRoute fasterRoute = buildFasterRouteThatReturns(true);
    NavigationFasterRouteListener listener = new NavigationFasterRouteListener(eventDispatcher, fasterRoute);
    DirectionsResponse response = buildDirectionsResponse();
    RouteProgress routeProgress = mock(RouteProgress.class);

    listener.onResponseReceived(response, routeProgress);

    verify(eventDispatcher).onFasterRouteEvent(any(DirectionsRoute.class));
  }

  @Test
  public void onResponseReceived_slowerRouteIsNotSentToDispatcher() {
    NavigationEventDispatcher eventDispatcher = mock(NavigationEventDispatcher.class);
    FasterRoute fasterRoute = buildFasterRouteThatReturns(false);
    NavigationFasterRouteListener listener = new NavigationFasterRouteListener(eventDispatcher, fasterRoute);
    DirectionsResponse response = buildDirectionsResponse();
    RouteProgress routeProgress = mock(RouteProgress.class);

    listener.onResponseReceived(response, routeProgress);

    verifyZeroInteractions(eventDispatcher);
  }

  @NonNull
  private FasterRoute buildFasterRouteThatReturns(boolean isFaster) {
    FasterRoute fasterRoute = mock(FasterRoute.class);
    when(fasterRoute.isFasterRoute(any(DirectionsResponse.class), any(RouteProgress.class))).thenReturn(isFaster);
    return fasterRoute;
  }

  @NonNull
  private DirectionsResponse buildDirectionsResponse() {
    DirectionsResponse response = mock(DirectionsResponse.class);
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(mock(DirectionsRoute.class));
    when(response.routes()).thenReturn(routes);
    return response;
  }
}