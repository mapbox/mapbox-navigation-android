package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouteRefresherTest {

  @Test
  public void checksShouldRefreshIfIntervalHasPassed() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresh mockedRouteRefresh = mock(RouteRefresh.class);
    long aRefreshIntervalInMilliseconds = 1;
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.refreshIntervalInMilliseconds()).thenReturn(aRefreshIntervalInMilliseconds);
    when(mockedMapboxNavigationOptions.enableRefreshRoute()).thenReturn(true);
    RouteRefresher theRouteRefresher = new RouteRefresher(mockedMapboxNavigation, mockedRouteRefresh);
    Date aDate = new Date();
    Date aDatePassedTheInterval = new Date(aDate.getTime() + aRefreshIntervalInMilliseconds + 1);

    boolean shouldRefresh = theRouteRefresher.check(aDatePassedTheInterval);

    assertTrue(shouldRefresh);
  }

  @Test
  public void checksShouldNotRefreshIfIntervalHasNotPassed() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresh mockedRouteRefresh = mock(RouteRefresh.class);
    long aRefreshIntervalInMilliseconds = 1;
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.refreshIntervalInMilliseconds()).thenReturn(aRefreshIntervalInMilliseconds);
    RouteRefresher theRouteRefresher = new RouteRefresher(mockedMapboxNavigation, mockedRouteRefresh);
    when(mockedMapboxNavigationOptions.enableRefreshRoute()).thenReturn(true);
    Date aDate = new Date();
    Date aDatePassedTheInterval = new Date(aDate.getTime() + aRefreshIntervalInMilliseconds);

    boolean shouldRefresh = theRouteRefresher.check(aDatePassedTheInterval);

    assertFalse(shouldRefresh);
  }

  @Test
  public void checksShouldNotRefreshIfCurrentlyChecking() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresh mockedRouteRefresh = mock(RouteRefresh.class);
    long aRefreshIntervalInMilliseconds = 1;
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.refreshIntervalInMilliseconds()).thenReturn(aRefreshIntervalInMilliseconds);
    when(mockedMapboxNavigationOptions.enableRefreshRoute()).thenReturn(true);
    RouteRefresher theRouteRefresher = new RouteRefresher(mockedMapboxNavigation, mockedRouteRefresh);
    Date aDate = new Date();
    Date aDatePassedTheInterval = new Date(aDate.getTime() + aRefreshIntervalInMilliseconds + 1);

    theRouteRefresher.updateIsChecking(true);
    boolean shouldRefresh = theRouteRefresher.check(aDatePassedTheInterval);

    assertFalse(shouldRefresh);
  }

  @Test
  public void checksShouldNotRefreshIfRouteRefreshIsNotEnabled() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresh mockedRouteRefresh = mock(RouteRefresh.class);
    long aRefreshIntervalInMilliseconds = 1;
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.refreshIntervalInMilliseconds()).thenReturn(aRefreshIntervalInMilliseconds);
    when(mockedMapboxNavigationOptions.enableRefreshRoute()).thenReturn(false);
    RouteRefresher theRouteRefresher = new RouteRefresher(mockedMapboxNavigation, mockedRouteRefresh);
    Date aDate = new Date();
    Date aDatePassedTheInterval = new Date(aDate.getTime() + aRefreshIntervalInMilliseconds + 1);

    theRouteRefresher.updateIsChecking(true);
    boolean shouldRefresh = theRouteRefresher.check(aDatePassedTheInterval);

    assertFalse(shouldRefresh);
  }

  @Test
  public void checksUpdateIsCheckingWhenRefresh() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresh mockedRouteRefresh = mock(RouteRefresh.class);
    long aRefreshIntervalInMilliseconds = 1;
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.refreshIntervalInMilliseconds()).thenReturn(aRefreshIntervalInMilliseconds);
    when(mockedMapboxNavigationOptions.enableRefreshRoute()).thenReturn(true);
    RouteRefresher theRouteRefresher = new RouteRefresher(mockedMapboxNavigation, mockedRouteRefresh);
    Date aDate = new Date();
    Date aDatePassedTheInterval = new Date(aDate.getTime() + aRefreshIntervalInMilliseconds + 2);
    RouteProgress mockedRouteProgress = mock(RouteProgress.class);

    theRouteRefresher.refresh(mockedRouteProgress);
    boolean shouldRefresh = theRouteRefresher.check(aDatePassedTheInterval);

    assertFalse(shouldRefresh);
  }

  @Test
  public void checksRouteRefreshIsCalledWhenRefresh() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    RouteRefresh mockedRouteRefresh = mock(RouteRefresh.class);
    long aRefreshIntervalInMilliseconds = 1;
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.refreshIntervalInMilliseconds()).thenReturn(aRefreshIntervalInMilliseconds);
    when(mockedMapboxNavigationOptions.enableRefreshRoute()).thenReturn(true);
    RouteRefresher theRouteRefresher = new RouteRefresher(mockedMapboxNavigation, mockedRouteRefresh);
    RouteProgress mockedRouteProgress = mock(RouteProgress.class);

    theRouteRefresher.refresh(mockedRouteProgress);

    verify(mockedRouteRefresh).refresh(eq(mockedRouteProgress), any(RouteRefresherCallback.class));
  }
}