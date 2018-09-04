package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.navigator.Navigator;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@Ignore
public class SnapToRouteTest {

  @Test
  public void sanity() {
    Navigator navigator = mock(Navigator.class);
    Snap snap = new SnapToRoute(navigator);

    assertNotNull(snap);
  }

  @Test
  public void getSnappedLocation_returnsProviderNameCorrectly() {
    Navigator navigator = mock(Navigator.class);
    SnapToRoute snap = new SnapToRoute(navigator);
    Location location = new Location("test");

    Location snappedLocation = snap.getSnappedLocationWith(location, new Date());

    assertTrue(snappedLocation.getProvider().equals("test"));
  }
}
