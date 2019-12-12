package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationViewModelProgressChangeListenerTest {

  @Test
  public void checksNavigationViewModelRouteProgressIsUpdatedWhenOnProgressChange() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    NavigationViewModelProgressChangeListener theNavigationViewModelProgressChangeListener =
      new NavigationViewModelProgressChangeListener(mockedNavigationViewModel);
    Location anyLocation = mock(Location.class);
    RouteProgress theRouteProgress = mock(RouteProgress.class);

    theNavigationViewModelProgressChangeListener.onProgressChange(anyLocation, theRouteProgress);

    verify(mockedNavigationViewModel).updateRouteProgress(eq(theRouteProgress));
  }

  @Test
  public void checksNavigationViewModelLocationIsUpdatedWhenOnProgressChange() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    NavigationViewModelProgressChangeListener theNavigationViewModelProgressChangeListener =
      new NavigationViewModelProgressChangeListener(mockedNavigationViewModel);
    Location theLocation = mock(Location.class);
    RouteProgress anyRouteProgress = mock(RouteProgress.class);

    theNavigationViewModelProgressChangeListener.onProgressChange(theLocation, anyRouteProgress);

    verify(mockedNavigationViewModel).updateLocation(eq(theLocation));
  }
}