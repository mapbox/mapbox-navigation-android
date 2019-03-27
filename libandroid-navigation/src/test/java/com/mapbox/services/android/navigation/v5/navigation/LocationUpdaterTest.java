package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Looper;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEventDispatcher;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocationUpdaterTest {

  @Test
  public void updateLocationEngine_previousUpdatesRemoved() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngineRequest locationEngineRequest = mock(LocationEngineRequest.class);
    LocationUpdater locationUpdater = new LocationUpdater(thread, mock(NavigationEventDispatcher.class),
      locationEngine, locationEngineRequest);

    locationUpdater.updateLocationEngine(mock(LocationEngine.class));

    verify(locationEngine).removeLocationUpdates(any(LocationEngineCallback.class));
  }

  @Test
  public void updateLocationEngineRequest_previousUpdatesRemoved() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngineRequest locationEngineRequest = mock(LocationEngineRequest.class);
    LocationUpdater locationUpdater = new LocationUpdater(thread, mock(NavigationEventDispatcher.class),
      locationEngine, locationEngineRequest);

    locationUpdater.updateLocationEngineRequest(mock(LocationEngineRequest.class));

    verify(locationEngine).removeLocationUpdates(any(LocationEngineCallback.class));
  }

  @Test
  public void removeLocationUpdates_updatesRemoved() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngineRequest locationEngineRequest = mock(LocationEngineRequest.class);
    LocationUpdater locationUpdater = new LocationUpdater(thread, mock(NavigationEventDispatcher.class),
      locationEngine, locationEngineRequest);

    locationUpdater.removeLocationUpdates();

    verify(locationEngine).removeLocationUpdates(any(LocationEngineCallback.class));
  }

  @Test
  public void updateLocationEngine_newUpdatesRequested() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngineRequest locationEngineRequest = mock(LocationEngineRequest.class);
    LocationUpdater locationUpdater = new LocationUpdater(thread, mock(NavigationEventDispatcher.class),
      locationEngine, locationEngineRequest);

    locationUpdater.updateLocationEngine(mock(LocationEngine.class));

    verify(locationEngine).requestLocationUpdates(any(LocationEngineRequest.class),
      any(LocationEngineCallback.class), eq((Looper) null));
  }

  @Test
  public void updateLocationEngineRequest_newUpdatesRequested() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngineRequest locationEngineRequest = mock(LocationEngineRequest.class);
    LocationUpdater locationUpdater = new LocationUpdater(thread, mock(NavigationEventDispatcher.class),
      locationEngine, locationEngineRequest);

    locationUpdater.updateLocationEngineRequest(mock(LocationEngineRequest.class));

    verify(locationEngine, times(2)).requestLocationUpdates(any(LocationEngineRequest.class),
      any(LocationEngineCallback.class), eq((Looper) null));
  }

  @Test
  public void onSuccess_currentLocationEngineCallbackUpdatesCorrectly() {
    LocationEngineResult result = mock(LocationEngineResult.class);
    Location location = mock(Location.class);
    when(result.getLastLocation()).thenReturn(location);
    LocationUpdater locationUpdater = mock(LocationUpdater.class);
    LocationUpdater.CurrentLocationEngineCallback callback = new LocationUpdater.CurrentLocationEngineCallback(
      locationUpdater
    );

    callback.onSuccess(result);

    verify(locationUpdater).onLocationChanged(location);
  }

  @Test
  public void onSuccess_dispatcherReceivesLocation() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    NavigationEventDispatcher dispatcher = mock(NavigationEventDispatcher.class);
    LocationEngine locationEngine = mock(LocationEngine.class);
    LocationEngineRequest locationEngineRequest = mock(LocationEngineRequest.class);
    Location location = mock(Location.class);
    LocationUpdater locationUpdater = new LocationUpdater(thread, dispatcher, locationEngine, locationEngineRequest);

    locationUpdater.onLocationChanged(location);

    verify(dispatcher).onLocationUpdate(eq(location));
  }
}