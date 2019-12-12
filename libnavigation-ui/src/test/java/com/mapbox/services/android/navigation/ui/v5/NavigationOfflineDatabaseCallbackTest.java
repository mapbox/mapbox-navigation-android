package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationOfflineDatabaseCallbackTest {

  @Test
  public void checksMapOfflineManagerProgressChangeListenerIsAddedWhenOnComplete() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapOfflineManager mockedMapOfflineManager = mock(MapOfflineManager.class);
    NavigationOfflineDatabaseCallback theNavigationOfflineDatabaseCallback =
      new NavigationOfflineDatabaseCallback(mockedNavigation, mockedMapOfflineManager);

    theNavigationOfflineDatabaseCallback.onComplete();

    verify(mockedNavigation).addProgressChangeListener(eq(mockedMapOfflineManager));
  }

  @Test
  public void checksMapOfflineManagerOnDestroyIsCalledWhenOnDestroy() {
    MapboxNavigation mockedNavigation = mock(MapboxNavigation.class);
    MapOfflineManager mockedMapOfflineManager = mock(MapOfflineManager.class);
    NavigationOfflineDatabaseCallback theNavigationOfflineDatabaseCallback =
      new NavigationOfflineDatabaseCallback(mockedNavigation, mockedMapOfflineManager);

    theNavigationOfflineDatabaseCallback.onDestroy();

    verify(mockedMapOfflineManager).onDestroy();
  }
}