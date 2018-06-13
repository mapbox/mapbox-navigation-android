package com.mapbox.services.android.navigation.v5.location.gpx;

import android.animation.TimeAnimator;
import android.location.Location;
import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GpxTimeListenerTest {

  @Test
  public void sanity() {
    GpxTimeListener parser = buildGpxTimeListener();

    assertNotNull(parser);
  }

  @Test
  public void onInitialization_firstLocationUpdateIsSent() {
    Location firstLocation = mock(Location.class);
    when(firstLocation.getTime()).thenReturn(0L);
    GpxLocationListener listener = mock(GpxLocationListener.class);

    buildGpxTimeListener(firstLocation, listener);

    verify(listener).onLocationUpdate(firstLocation);
  }

  @Test
  public void onTimeUpdate_locationIsSentWhenTimePassed() {
    Location firstLocation = mock(Location.class);
    when(firstLocation.getTime()).thenReturn(1L);
    Location secondLocation = mock(Location.class);
    when(secondLocation.getTime()).thenReturn(1L);
    List<Location> gpxLocations = new ArrayList<>();
    gpxLocations.add(firstLocation);
    gpxLocations.add(secondLocation);
    GpxLocationListener listener = mock(GpxLocationListener.class);
    GpxTimeListener timeListener = buildGpxTimeListener(gpxLocations, listener);

    timeListener.onTimeUpdate(mock(TimeAnimator.class), 3L, 0L);

    verify(listener).onLocationUpdate(secondLocation);
  }

  @Test
  public void onTimeUpdate_locationIsNotSentWhenTimeHasNotPassed() {
    Location firstLocation = mock(Location.class);
    when(firstLocation.getTime()).thenReturn(0L);
    Location secondLocation = mock(Location.class);
    when(secondLocation.getTime()).thenReturn(0L);
    List<Location> gpxLocations = new ArrayList<>();
    gpxLocations.add(firstLocation);
    gpxLocations.add(secondLocation);
    GpxLocationListener listener = mock(GpxLocationListener.class);
    GpxTimeListener timeListener = buildGpxTimeListener(gpxLocations, listener);

    timeListener.onTimeUpdate(mock(TimeAnimator.class), 0L, 0L);

    verify(listener, times(0)).onLocationUpdate(secondLocation);
  }

  @NonNull
  private GpxTimeListener buildGpxTimeListener(Location location, GpxLocationListener locationListener) {
    List<GpxLocationListener> listeners = new ArrayList<>(1);
    listeners.add(locationListener);
    List gpxLocations = mock(List.class);
    when(gpxLocations.remove(0)).thenReturn(location);
    return new GpxTimeListener(listeners, gpxLocations);
  }

  @NonNull
  private GpxTimeListener buildGpxTimeListener(List gpxLocations, GpxLocationListener locationListener) {
    List<GpxLocationListener> listeners = new ArrayList<>(1);
    listeners.add(locationListener);
    return new GpxTimeListener(listeners, gpxLocations);
  }

  @NonNull
  private GpxTimeListener buildGpxTimeListener() {
    List listeners = new ArrayList();
    List gpxLocations = mock(List.class);
    Location firstLocation = mock(Location.class);
    when(firstLocation.getTime()).thenReturn(0L);
    when(gpxLocations.remove(0)).thenReturn(firstLocation);
    return new GpxTimeListener(listeners, gpxLocations);
  }
}
