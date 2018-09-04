package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.Navigator;
import com.mapbox.services.android.navigation.v5.location.LocationValidator;

import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Ignore
public class NavigationLocationEngineListenerTest {

  @Test
  public void onConnected_engineRequestsUpdates() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = buildListener(locationEngine);

    listener.onConnected();

    verify(locationEngine).requestLocationUpdates();
  }

  @Test
  public void queueValidLocationUpdate_threadReceivesUpdate() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    NavigationLocationEngineListener listener = buildListener(thread);
    Location location = mock(Location.class);

    listener.onLocationChanged(location);

    verify(thread).updateLocation(location);
  }

  @Test
  public void queueInvalidLocationUpdate_navigatorReceivesUpdate() {
    Navigator navigator = mock(Navigator.class);
    NavigationLocationEngineListener listener = buildListener(navigator);

    listener.onLocationChanged(mock(Location.class));

    verify(navigator).updateLocation(any(FixLocation.class));
  }

  private NavigationLocationEngineListener buildListener(RouteProcessorBackgroundThread thread) {
    return new NavigationLocationEngineListener(thread, mock(Navigator.class), mock(LocationEngine.class),
      mock(LocationValidator.class));
  }

  private NavigationLocationEngineListener buildListener(Navigator navigator) {
    return new NavigationLocationEngineListener(mock(RouteProcessorBackgroundThread.class), navigator,
      mock(LocationEngine.class), mock(LocationValidator.class));
  }

  private NavigationLocationEngineListener buildListener(LocationEngine locationEngine) {
    return new NavigationLocationEngineListener(mock(RouteProcessorBackgroundThread.class), mock(Navigator.class),
      locationEngine, mock(LocationValidator.class));
  }
}