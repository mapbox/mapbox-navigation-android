package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SnapToRouteTest extends BaseTest {

  @Test
  public void sanity() throws Exception {
    Snap snap = new SnapToRoute();

    assertNotNull(snap);
  }

  @Test
  public void getSnappedLocation_returnsProviderNameCorrectly() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Snap snap = new SnapToRoute();
    Location location = new Location("test");

    Location snappedLocation = snap.getSnappedLocation(location, routeProgress);

    assertTrue(snappedLocation.getProvider().equals("test"));
  }
}
