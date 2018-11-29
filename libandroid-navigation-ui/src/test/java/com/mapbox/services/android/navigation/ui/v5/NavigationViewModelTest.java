package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
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
}