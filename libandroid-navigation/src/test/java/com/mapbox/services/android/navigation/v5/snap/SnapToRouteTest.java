package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.navigator.NavigationStatus;

import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@Ignore
public class SnapToRouteTest {

  @Test
  public void sanity() {
    Snap snap = new SnapToRoute();

    assertNotNull(snap);
  }

  @Test
  public void getSnappedLocation_returnsProviderNameCorrectly() {
    // TODO mock final class
    NavigationStatus status = mock(NavigationStatus.class);
    SnapToRoute snap = new SnapToRoute();
    Location location = new Location("test");

    Location snappedLocation = snap.getSnappedLocationWith(location, status);

    assertTrue(snappedLocation.getProvider().equals("test"));
  }
}
