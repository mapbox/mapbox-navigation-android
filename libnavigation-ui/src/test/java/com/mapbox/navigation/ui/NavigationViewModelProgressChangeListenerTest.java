package com.mapbox.navigation.ui;

import android.location.Location;

import com.mapbox.navigation.base.trip.model.RouteProgress;

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
    RouteProgress theRouteProgress = mock(RouteProgress.class);

    theNavigationViewModelProgressChangeListener.onRouteProgressChanged(theRouteProgress);

    verify(mockedNavigationViewModel).updateRouteProgress(eq(theRouteProgress));
  }

  @Test
  public void checksNavigationViewModelLocationIsUpdatedWhenOnProgressChange() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    NavigationViewModelProgressChangeListener theNavigationViewModelProgressChangeListener =
            new NavigationViewModelProgressChangeListener(mockedNavigationViewModel);
    Location theLocation = mock(Location.class);

    theNavigationViewModelProgressChangeListener.onRawLocationChanged(theLocation);

    verify(mockedNavigationViewModel).updateLocation(eq(theLocation));
  }
}