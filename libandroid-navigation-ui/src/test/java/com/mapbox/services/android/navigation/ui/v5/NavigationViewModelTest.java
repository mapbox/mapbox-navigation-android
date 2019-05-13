package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayer;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class NavigationViewModelTest {

  @Test
  public void stopNavigation_progressListenersAreRemoved() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
      mapOfflineManager);

    viewModel.stopNavigation();

    verify(navigation).removeProgressChangeListener(null);
  }

  @Test
  public void stopNavigation_milestoneListenersAreRemoved() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
      mapOfflineManager);

    viewModel.stopNavigation();

    verify(navigation).removeMilestoneEventListener(null);
  }

  @Test
  public void stopNavigation_mapOfflineManagerOnDestroyIsCalledIfNotNull() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
      mapOfflineManager);

    viewModel.onDestroy(false);

    verify(mapOfflineManager).onDestroy();
  }

  @Test
  public void stopNavigation_mapConnectivityControllerStateIsReset() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
      mapOfflineManager);
    Boolean defaultState = null;

    viewModel.onDestroy(false);

    verify(mockedConnectivityController).assign(eq(defaultState));
  }

  @Test
  public void updateRoute_navigationIsNotUpdatedWhenChangingConfigurations() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    DirectionsRoute route = mock(DirectionsRoute.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
      mapOfflineManager);
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
    SpeechPlayer speechPlayer = mock(SpeechPlayer.class);
    DirectionsRoute route = mock(DirectionsRoute.class);
    NavigationViewModel viewModel = new NavigationViewModel(
      application, navigation, conductor, dispatcher, cache, speechPlayer
    );
    viewModel.isOffRoute.postValue(true);

    viewModel.updateRoute(route);

    verify(navigation).startNavigation(route);
  }

  @Test
  public void isMuted_falseWithNullSpeechPlayer() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    LocationEngineConductor conductor = mock(LocationEngineConductor.class);
    NavigationViewEventDispatcher dispatcher = mock(NavigationViewEventDispatcher.class);
    VoiceInstructionCache cache = mock(VoiceInstructionCache.class);
    SpeechPlayer speechPlayer = null;
    NavigationViewModel viewModel = new NavigationViewModel(
      application, navigation, conductor, dispatcher, cache, speechPlayer
    );

    boolean isMuted = viewModel.isMuted();

    assertFalse(isMuted);
  }

  @Test
  public void isMuted_trueWithMutedSpeechPlayer() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    LocationEngineConductor conductor = mock(LocationEngineConductor.class);
    NavigationViewEventDispatcher dispatcher = mock(NavigationViewEventDispatcher.class);
    VoiceInstructionCache cache = mock(VoiceInstructionCache.class);
    SpeechPlayer speechPlayer = mock(SpeechPlayer.class);
    when(speechPlayer.isMuted()).thenReturn(true);
    NavigationViewModel viewModel = new NavigationViewModel(
      application, navigation, conductor, dispatcher, cache, speechPlayer
    );

    boolean isMuted = viewModel.isMuted();

    assertTrue(isMuted);
  }
}