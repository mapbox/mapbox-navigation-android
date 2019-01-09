package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductor;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class NavigationViewModelTest {

  @Test
  public void stopNavigation_progressListenersAreRemoved() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation);

    viewModel.stopNavigation();

    verify(navigation).removeProgressChangeListener(null);
  }

  @Test
  public void stopNavigation_milestoneListenersAreRemoved() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation);

    viewModel.stopNavigation();

    verify(navigation).removeMilestoneEventListener(null);
  }

  @Test
  public void updateRoute_navigationIsNotUpdatedWhenChangingConfigurations() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    DirectionsRoute route = mock(DirectionsRoute.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation);
    viewModel.onDestroy(true);

    viewModel.updateRoute(route);

    verify(navigation, times(0)).startNavigation(route);
  }

  @Test
  public void updateRoute_navigationIsUpdated() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    LocationEngineConductor conductor = mock(LocationEngineConductor.class);
    NavigationViewEventDispatcher dispatcher = mock(NavigationViewEventDispatcher.class);
    VoiceInstructionCache cache = mock(VoiceInstructionCache.class);
    DirectionsRoute route = mock(DirectionsRoute.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, conductor, dispatcher, cache);
    viewModel.isOffRoute.postValue(true);

    viewModel.updateRoute(route);

    verify(navigation).startNavigation(route);
  }
}