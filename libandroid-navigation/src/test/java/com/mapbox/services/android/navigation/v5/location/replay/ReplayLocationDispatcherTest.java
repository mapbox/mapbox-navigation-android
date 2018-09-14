package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;
import android.os.Handler;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplayLocationDispatcherTest {

  @Test(expected = IllegalArgumentException.class)
  public void checksNonNullLocationListRequired() {
    List<Location> nullLocations = null;

    new ReplayLocationDispatcher(nullLocations);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checksNonEmptyLocationListRequired() {
    List<Location> empty = Collections.emptyList();

    new ReplayLocationDispatcher(empty);
  }

  @Test
  public void checksLocationDispatchedWhenIsNotLastLocation() {
    List<Location> anyLocations = new ArrayList<>(1);
    Location aLocation = createALocation();
    anyLocations.add(aLocation);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations);
    ReplayLocationListener aReplayLocationListener = mock(ReplayLocationListener.class);
    theReplayLocationDispatcher.addReplayLocationListener(aReplayLocationListener);

    theReplayLocationDispatcher.run();

    verify(aReplayLocationListener).onLocationReplay(eq(aLocation));
  }

  @Test
  public void checksNextDispatchScheduledWhenLocationsIsNotEmpty() {
    List<Location> anyLocations = new ArrayList<>(2);
    Location firstLocation = createALocation();
    when(firstLocation.getTime()).thenReturn(1000L);
    Location secondLocation = createALocation();
    when(secondLocation.getTime()).thenReturn(2000L);
    anyLocations.add(firstLocation);
    anyLocations.add(secondLocation);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);

    theReplayLocationDispatcher.run();

    verify(aHandler, times(1)).postDelayed(eq(theReplayLocationDispatcher), eq(1000L));
  }

  @Test
  public void checksNextDispatchNotScheduledWhenLocationsIsEmpty() {
    List<Location> anyLocations = new ArrayList<>(1);
    Location firstLocation = createALocation();
    anyLocations.add(firstLocation);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);

    theReplayLocationDispatcher.run();

    verify(aHandler, never()).postDelayed(any(Runnable.class), anyLong());
  }

  @Test
  public void checksStopDispatchingWhenLocationsIsEmpty() {
    List<Location> anyLocations = new ArrayList<>(1);
    Location firstLocation = createALocation();
    anyLocations.add(firstLocation);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);

    theReplayLocationDispatcher.run();

    verify(aHandler, times(1)).removeCallbacks(eq(theReplayLocationDispatcher));
  }

  @Test
  public void checksClearLocationsWhenStop() {
    List<Location> theLocations = mock(List.class);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(theLocations, aHandler);

    theReplayLocationDispatcher.stop();

    verify(theLocations, times(1)).clear();
  }

  @Test
  public void checksStopDispatchingWhenStop() {
    List<Location> anyLocations = mock(List.class);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);

    theReplayLocationDispatcher.stop();

    verify(aHandler, times(1)).removeCallbacks(eq(theReplayLocationDispatcher));
  }

  @Test
  public void checksStopDispatchingWhenPause() {
    List<Location> anyLocations = mock(List.class);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);

    theReplayLocationDispatcher.pause();

    verify(aHandler, times(1)).removeCallbacks(eq(theReplayLocationDispatcher));
  }

  @Test(expected = IllegalArgumentException.class)
  public void checksNonNullLocationListRequiredWhenUpdate() {
    List<Location> anyLocations = mock(List.class);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);
    List<Location> nullLocations = null;

    theReplayLocationDispatcher.update(nullLocations);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checksNonEmptyLocationListRequiredWhenUpdate() {
    List<Location> anyLocations = mock(List.class);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);
    List<Location> empty = Collections.emptyList();

    theReplayLocationDispatcher.update(empty);
  }

  @Test
  public void checksAddLocationsWhenAdd() {
    List<Location> anyLocations = mock(List.class);
    Handler aHandler = mock(Handler.class);
    ReplayLocationDispatcher theReplayLocationDispatcher = new ReplayLocationDispatcher(anyLocations, aHandler);
    List<Location> locationsToReplay = mock(List.class);

    theReplayLocationDispatcher.add(locationsToReplay);

    verify(anyLocations, times(1)).addAll(eq(locationsToReplay));
  }

  private Location createALocation() {
    Location location = mock(Location.class);
    return location;
  }
}