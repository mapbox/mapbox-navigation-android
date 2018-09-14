package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ReplayJsonRouteLocationMapperTest {

  private static final double DELTA = 1e-15;

  @Test(expected = IllegalArgumentException.class)
  public void checksNonNullLocationListRequired() {
    List<ReplayLocationDto> nullLocations = null;

    new ReplayJsonRouteLocationMapper(nullLocations);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checksNonEmptyLocationListRequired() {
    List<ReplayLocationDto> empty = Collections.emptyList();

    new ReplayJsonRouteLocationMapper(empty);
  }

  @Test
  public void checksProviderMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals("ReplayLocation", theLocation.getProvider());
  }

  @Test
  public void checksLongitudeMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setLongitude(2.0);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(2.0, theLocation.getLongitude(), DELTA);
  }

  @Test
  public void checksAccuracyMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setHorizontalAccuracyMeters(3.0f);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(3.0f, theLocation.getAccuracy(), DELTA);
  }

  @Test
  public void checksBearingMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setBearing(180.0);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(180f, theLocation.getBearing(), DELTA);
  }

  @Test
  @Config(sdk = 26)
  public void checksVerticalAccuracyMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setVerticalAccuracyMeters(8.0f);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(8.0, theLocation.getVerticalAccuracyMeters(), DELTA);
  }

  @Test(expected = NoSuchMethodError.class)
  @Config(sdk = 25)
  public void checksVerticalAccuracyNotMappedForBelowOreo() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setVerticalAccuracyMeters(8.0f);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    theLocation.getVerticalAccuracyMeters();
  }

  @Test
  public void checksSpeedMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setSpeed(65.0);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(65.0f, theLocation.getSpeed(), DELTA);
  }

  @Test
  public void checksLatitudeMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setLatitude(7.0);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(7.0, theLocation.getLatitude(), DELTA);
  }

  @Test
  public void checksAltitudeMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    aReplayLocation.setAltitude(9.0);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    assertEquals(9.0, theLocation.getAltitude(), DELTA);
  }

  @Test
  public void checksTimeMapping() {
    List<ReplayLocationDto> anyReplayLocations = new ArrayList<>(1);
    ReplayLocationDto aReplayLocation = new ReplayLocationDto();
    Date aDate = new Date();
    aReplayLocation.setDate(aDate);
    anyReplayLocations.add(aReplayLocation);
    ReplayJsonRouteLocationMapper theReplayJsonRouteLocationMapper = new ReplayJsonRouteLocationMapper(anyReplayLocations);

    List<Location> locations = theReplayJsonRouteLocationMapper.toLocations();

    Location theLocation = locations.get(0);
    long timeFromDate = aDate.getTime();
    assertEquals(timeFromDate, theLocation.getTime(), DELTA);
  }
}