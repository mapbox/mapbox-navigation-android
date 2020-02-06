package com.mapbox.navigation.ui;

import android.app.Application;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.ui.LocationEngineConductor;
import com.mapbox.navigation.ui.MapConnectivityController;
import com.mapbox.navigation.ui.MapOfflineManager;
import com.mapbox.navigation.ui.NavigationViewEventDispatcher;
import com.mapbox.navigation.ui.NavigationViewModel;
import com.mapbox.navigation.ui.NavigationViewRouter;
import com.mapbox.navigation.ui.VoiceInstructionCache;
import com.mapbox.navigation.ui.voice.SpeechPlayer;
import com.mapbox.navigation.ui.navigation.MapboxNavigation;

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
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, router);

    viewModel.stopNavigation();

    verify(navigation).removeProgressChangeListener(null);
  }

  @Test
  public void stopNavigation_milestoneListenersAreRemoved() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, router);

    viewModel.stopNavigation();

    verify(navigation).removeMilestoneEventListener(null);
  }

  @Test
  public void stopNavigation_mapOfflineManagerOnDestroyIsCalledIfNotNull() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, router);

    viewModel.onDestroy(false);

    verify(mapOfflineManager).onDestroy();
  }

  @Test
  public void stopNavigation_mapConnectivityControllerStateIsReset() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, router);
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
    NavigationViewRouter router = mock(NavigationViewRouter.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, router);
    viewModel.onDestroy(true);

    viewModel.updateRoute(route);

    verify(navigation, times(0)).startNavigation(route);
  }

  @Test
  public void navigationRouter_onDestroyInvokedWhenViewModelIsDestroyed() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewRouter mockedRouter = mock(NavigationViewRouter.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, mockedRouter);
    viewModel.onCleared();
    verify(mockedRouter).onDestroy();
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