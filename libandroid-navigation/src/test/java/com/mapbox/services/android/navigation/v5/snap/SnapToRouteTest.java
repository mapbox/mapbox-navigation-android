package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.NavigationStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SnapToRouteTest {

  @Test
  public void sanity() {
    Snap snap = new SnapToRoute();

    assertNotNull(snap);
  }

  @Test
  public void getSnappedLocationWith_returnsRawLocationProvider() {
    NavigationStatus status = buildMockStatus();
    String provider = "location_provider";
    Location rawLocation = new Location(provider);
    SnapToRoute snap = new SnapToRoute();

    Location snappedLocation = snap.getSnappedLocationWith(status, rawLocation);

    assertEquals(provider, snappedLocation.getProvider());
  }

  @Test
  public void getSnappedLocationWith_returnsRawLocationSpeed() {
    NavigationStatus status = buildMockStatus();
    Location rawLocation = new Location("location_provider");
    float speed = 1.4f;
    rawLocation.setSpeed(speed);
    SnapToRoute snap = new SnapToRoute();

    Location snappedLocation = snap.getSnappedLocationWith(status, rawLocation);

    assertEquals(speed, snappedLocation.getSpeed());
  }

  @Test
  public void getSnappedLocationWith_returnsRawLocationAltitude() {
    NavigationStatus status = buildMockStatus();
    Location rawLocation = new Location("location_provider");
    double altitude = 25d;
    rawLocation.setAltitude(altitude);
    SnapToRoute snap = new SnapToRoute();

    Location snappedLocation = snap.getSnappedLocationWith(status, rawLocation);

    assertEquals(altitude, snappedLocation.getAltitude());
  }

  @Test
  public void getSnappedLocationWith_returnsRawLocationAccuracy() {
    NavigationStatus status = buildMockStatus();
    Location rawLocation = new Location("location_provider");
    float accuracy = 20f;
    rawLocation.setAccuracy(accuracy);
    SnapToRoute snap = new SnapToRoute();

    Location snappedLocation = snap.getSnappedLocationWith(status, rawLocation);

    assertEquals(accuracy, snappedLocation.getAccuracy());
  }

  private NavigationStatus buildMockStatus() {
    NavigationStatus status = mock(NavigationStatus.class, RETURNS_DEEP_STUBS);
    Point location = Point.fromLngLat(0.0, 0.0);
    when(status.getLocation().getCoordinate()).thenReturn(location);
    when(status.getLocation().getTime()).thenReturn(new Date());
    when(status.getLocation().getBearing()).thenReturn(0.0f);
    return status;
  }
}
