package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import androidx.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationNotificationProviderTest {

  @Test
  public void updateNavigationNotification() {
    NavigationNotification notification = mock(NavigationNotification.class);
    MapboxNavigation mapboxNavigation = buildNavigationWithNotificationOptions(notification);
    Context context = mock(Context.class);
    NavigationNotificationProvider provider = new NavigationNotificationProvider(context, mapboxNavigation);

    RouteProgress routeProgress = mock(RouteProgress.class);
    provider.updateNavigationNotification(routeProgress);

    verify(notification).updateNotification(eq(routeProgress));
  }

  @Test
  public void updateNavigationNotification_doesNotUpdateAfterShutdown() {
    NavigationNotification notification = mock(NavigationNotification.class);
    MapboxNavigation mapboxNavigation = buildNavigationWithNotificationOptions(notification);
    Context context = mock(Context.class);
    NavigationNotificationProvider provider = new NavigationNotificationProvider(context, mapboxNavigation);
    RouteProgress routeProgress = mock(RouteProgress.class);

    provider.shutdown(context);
    provider.updateNavigationNotification(routeProgress);

    verify(notification, times(0)).updateNotification(routeProgress);
  }

  @Test
  public void onShutdown_onNavigationStoppedIsCalled() {
    NavigationNotification notification = mock(NavigationNotification.class);
    MapboxNavigation mapboxNavigation = buildNavigationWithNotificationOptions(notification);
    Context context = mock(Context.class);
    NavigationNotificationProvider provider = new NavigationNotificationProvider(context, mapboxNavigation);

    provider.shutdown(context);

    verify(notification).onNavigationStopped(context);
  }

  @NonNull
  private MapboxNavigation buildNavigationWithNotificationOptions(NavigationNotification notification) {
    MapboxNavigation mapboxNavigation = mock(MapboxNavigation.class);
    MapboxNavigationOptions options = mock(MapboxNavigationOptions.class);
    when(options.navigationNotification()).thenReturn(notification);
    when(mapboxNavigation.options()).thenReturn(options);
    return mapboxNavigation;
  }
}