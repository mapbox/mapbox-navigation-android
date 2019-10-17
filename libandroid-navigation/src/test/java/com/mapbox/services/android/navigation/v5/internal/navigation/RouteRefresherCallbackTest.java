package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.navigation.DirectionsRouteType;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.RefreshError;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RouteRefresherCallbackTest {

  @Test
  public void checksStartNavigationWithRefreshedRouteIsCalledWhenOnRefresh() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresher mockedRouteRefresher = mock(RouteRefresher.class);
    RouteRefresherCallback theRouteRefresherCallback = new RouteRefresherCallback(mockedMapboxNavigation,
      mockedRouteRefresher);
    DirectionsRoute anyRoute = mock(DirectionsRoute.class);

    theRouteRefresherCallback.onRefresh(anyRoute);

    verify(mockedMapboxNavigation).startNavigation(eq(anyRoute), ArgumentMatchers.eq(DirectionsRouteType.FRESH_ROUTE));
  }

  @Test
  public void checksUpdateLastRefreshDateIsCalledWhenOnRefresh() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresher mockedRouteRefresher = mock(RouteRefresher.class);
    RouteRefresherCallback theRouteRefresherCallback = new RouteRefresherCallback(mockedMapboxNavigation,
      mockedRouteRefresher);
    DirectionsRoute anyRoute = mock(DirectionsRoute.class);

    theRouteRefresherCallback.onRefresh(anyRoute);

    verify(mockedRouteRefresher).updateLastRefresh(any(Date.class));
  }

  @Test
  public void checksUpdateIsNotCheckingAfterOnRefresh() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresher mockedRouteRefresher = mock(RouteRefresher.class);
    RouteRefresherCallback theRouteRefresherCallback = new RouteRefresherCallback(mockedMapboxNavigation,
      mockedRouteRefresher);
    DirectionsRoute anyRoute = mock(DirectionsRoute.class);

    theRouteRefresherCallback.onRefresh(anyRoute);

    verify(mockedRouteRefresher).updateIsChecking(eq(false));
  }

  @Test
  public void checksUpdateIsNotCheckingIfOnError() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresher mockedRouteRefresher = mock(RouteRefresher.class);
    RouteRefresherCallback theRouteRefresherCallback = new RouteRefresherCallback(mockedMapboxNavigation,
      mockedRouteRefresher);
    RefreshError anyRefreshError = mock(RefreshError.class);

    theRouteRefresherCallback.onError(anyRefreshError);

    verify(mockedRouteRefresher).updateIsChecking(eq(false));
  }
}