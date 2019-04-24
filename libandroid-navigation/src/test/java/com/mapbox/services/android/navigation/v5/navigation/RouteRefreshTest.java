package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouteRefreshTest {

  @Test
  public void refresh_invalidOptionsSendErrorCallback() {
    RouteOptions options = mock(RouteOptions.class);
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(options);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RefreshCallback refreshCallback = mock(RefreshCallback.class);
    String accessToken = "some_access_token";
    RouteRefresh refresh = new RouteRefresh(accessToken);

    refresh.refresh(routeProgress, refreshCallback);

    verify(refreshCallback).onError(any(RefreshError.class));
  }

  @Test
  public void refresh_validOptionsDoNotSendError() {
    RouteOptions options = mock(RouteOptions.class);
    when(options.requestUuid()).thenReturn("some_uuid");
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeIndex()).thenReturn("1");
    when(route.routeOptions()).thenReturn(options);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RefreshCallback refreshCallback = mock(RefreshCallback.class);
    String accessToken = "some_access_token";
    RouteRefresh refresh = new RouteRefresh(accessToken);

    refresh.refresh(routeProgress, refreshCallback);

    verify(refreshCallback, times(0)).onError(any(RefreshError.class));
  }
}