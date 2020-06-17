package com.mapbox.navigation.ui;

import android.app.Application;

import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.ui.voice.SpeechPlayer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class NavigationViewModelTest {

  @Test
  public void stopNavigation_progressListenersAreRemoved() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewOptions mockNavigationViewOptions = mock(NavigationViewOptions.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
        mapOfflineManager, mockNavigationViewOptions);

    viewModel.stopNavigation();

    verify(navigation, times(1)).stopActiveGuidance();
  }

  @Test
  public void stopNavigation_mapOfflineManagerOnDestroyIsCalledIfNotNull() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewOptions mockNavigationViewOptions = mock(NavigationViewOptions.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, mockNavigationViewOptions);

    viewModel.onDestroy(false);

    verify(mapOfflineManager).onDestroy();
  }

  @Test
  public void stopNavigation_mapConnectivityControllerStateIsReset() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationViewOptions mockNavigationViewOptions = mock(NavigationViewOptions.class);
    MapConnectivityController mockedConnectivityController = mock(MapConnectivityController.class);
    MapOfflineManager mapOfflineManager = mock(MapOfflineManager.class);
    NavigationViewModel viewModel = new NavigationViewModel(application, navigation, mockedConnectivityController,
            mapOfflineManager, mockNavigationViewOptions);
    Boolean defaultState = null;

    viewModel.onDestroy(false);

    verify(mockedConnectivityController).assign(eq(defaultState));
  }

  @Test
  public void isMuted_falseWithNullSpeechPlayer() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationViewEventDispatcher dispatcher = mock(NavigationViewEventDispatcher.class);
    VoiceInstructionCache cache = mock(VoiceInstructionCache.class);
    SpeechPlayer speechPlayer = null;
    NavigationViewModel viewModel = new NavigationViewModel(
        application, navigation, dispatcher, cache, speechPlayer
    );

    boolean isMuted = viewModel.isMuted();

    assertFalse(isMuted);
  }

  @Test
  public void isMuted_trueWithMutedSpeechPlayer() {
    Application application = mock(Application.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationViewEventDispatcher dispatcher = mock(NavigationViewEventDispatcher.class);
    VoiceInstructionCache cache = mock(VoiceInstructionCache.class);
    SpeechPlayer speechPlayer = mock(SpeechPlayer.class);
    when(speechPlayer.isMuted()).thenReturn(true);
    NavigationViewModel viewModel = new NavigationViewModel(
        application, navigation, dispatcher, cache, speechPlayer
    );

    boolean isMuted = viewModel.isMuted();

    assertTrue(isMuted);
  }
}