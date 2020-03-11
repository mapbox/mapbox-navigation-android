package com.mapbox.navigation.ui;

import android.location.Location;

import com.mapbox.navigation.base.trip.model.RouteProgress;

import org.junit.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationViewModelProgressObserverTest {

  @Test
  public void checksNavigationViewModelRouteProgressIsUpdatedWhenOnProgressChange() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    NavigationViewModelProgressObserver theNavigationViewModelProgressObserver =
      new NavigationViewModelProgressObserver(mockedNavigationViewModel);
    RouteProgress theRouteProgress = mock(RouteProgress.class);

    theNavigationViewModelProgressObserver.onRouteProgressChanged(theRouteProgress);

    verify(mockedNavigationViewModel).updateRouteProgress(eq(theRouteProgress));
  }

  @Test
  @SuppressWarnings("unchecked assignment")
  public void checksNavigationViewModelLocationIsUpdatedWhenOnProgressChange() {
    NavigationViewModel mockedNavigationViewModel = mock(NavigationViewModel.class);
    NavigationViewModelProgressObserver theNavigationViewModelProgressChangeListener =
      new NavigationViewModelProgressObserver(mockedNavigationViewModel);
    Location theLocation = mock(Location.class);
    List<? extends Location> keyPoints = mock(List.class);

    theNavigationViewModelProgressChangeListener.onEnhancedLocationChanged(theLocation, keyPoints);

    verify(mockedNavigationViewModel).updateLocation(eq(theLocation));
  }
}