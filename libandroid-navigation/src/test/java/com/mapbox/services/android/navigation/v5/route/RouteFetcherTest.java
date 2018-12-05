package com.mapbox.services.android.navigation.v5.route;

import android.content.Context;

import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RouteFetcherTest {

  @Test
  public void cancelRouteCall_cancelsWithNonNullNavigationRoute() {
    Context context = mock(Context.class);
    NavigationRoute navigationRoute = mock(NavigationRoute.class);
    RouteFetcher routeFetcher = new RouteFetcher(context, "pk.xx", navigationRoute);

    routeFetcher.cancelRouteCall();

    verify(navigationRoute).cancelCall();
  }
}
