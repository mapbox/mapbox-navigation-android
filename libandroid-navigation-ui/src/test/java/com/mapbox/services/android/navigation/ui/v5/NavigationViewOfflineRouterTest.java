package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.v5.navigation.MapboxOfflineRouter;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.OfflineRoute;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class NavigationViewOfflineRouterTest {

  @Test
  public void configure_offlineRouterIsConfigured() {
    MapboxOfflineRouter offlineRouter = mock(MapboxOfflineRouter.class);
    NavigationViewRouter viewRouter = mock(NavigationViewRouter.class);
    NavigationViewOfflineRouter viewOfflineRouter = new NavigationViewOfflineRouter(offlineRouter, viewRouter);

    viewOfflineRouter.configure("some_tile_version");

    verify(offlineRouter).configure(eq("some_tile_version"), any(OfflineRouterConfiguredCallback.class));
  }

  @Test
  public void findRouteWith_notConfiguredIsIgnored() {
    MapboxOfflineRouter offlineRouter = mock(MapboxOfflineRouter.class);
    NavigationViewRouter viewRouter = mock(NavigationViewRouter.class);
    NavigationViewOfflineRouter viewOfflineRouter = new NavigationViewOfflineRouter(offlineRouter, viewRouter);

    viewOfflineRouter.findRouteWith(mock(NavigationRoute.Builder.class));

    verifyZeroInteractions(offlineRouter);
  }

  @Test
  public void findRouteWith_offlineRouteIsCalledWhenConfigured() {
    MapboxOfflineRouter offlineRouter = mock(MapboxOfflineRouter.class);
    NavigationViewRouter viewRouter = mock(NavigationViewRouter.class);
    NavigationViewOfflineRouter viewOfflineRouter = new NavigationViewOfflineRouter(offlineRouter, viewRouter);
    viewOfflineRouter.setIsConfigured(true);

    viewOfflineRouter.findRouteWith(mock(NavigationRoute.Builder.class));

    verify(offlineRouter).findRoute(any(OfflineRoute.class), any(OfflineRouteFoundCallback.class));
  }
}