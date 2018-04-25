package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.ui.v5.location.NavigationLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.location.NavigationLocationEngineCallback;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationLocationEngineTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() throws Exception {
    NavigationLocationEngineCallback mockCallback = mock(NavigationLocationEngineCallback.class);
    NavigationLocationEngine navigationLocationEngine = new NavigationLocationEngine(mockCallback);

    assertNotNull(navigationLocationEngine);
  }

  @Test
  public void onCreateIsCalled_locationEngineIsActivated() throws Exception {
    NavigationLocationEngineCallback mockCallback = mock(NavigationLocationEngineCallback.class);
    NavigationLocationEngine navigationLocationEngine = new NavigationLocationEngine(mockCallback);

    navigationLocationEngine.initializeLocationEngine(createMockContext(), true);
    LocationEngine locationEngine = navigationLocationEngine.obtainLocationEngine();

    assertTrue(locationEngine instanceof MockLocationEngine);
  }

  @NonNull
  private Context createMockContext() {
    Context mockContext = mock(Context.class);
    LocationManager mockLocationManager = mock(LocationManager.class);
    when(mockContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mockLocationManager);
    when(mockContext.getPackageManager()).thenReturn(mock(PackageManager.class));
    when(mockContext.getApplicationContext()).thenReturn(mock(Context.class));
    return mockContext;
  }

  private Location buildDefaultLocationUpdate(double lng, double lat) {
    return buildLocationUpdate(lng, lat, System.currentTimeMillis());
  }

  private Location buildLocationUpdate(double lng, double lat, long time) {
    Location location = mock(Location.class);
    when(location.getLongitude()).thenReturn(lng);
    when(location.getLatitude()).thenReturn(lat);
    when(location.getSpeed()).thenReturn(30f);
    when(location.getBearing()).thenReturn(100f);
    when(location.getAccuracy()).thenReturn(10f);
    when(location.getTime()).thenReturn(time);
    return location;
  }

  private RouteProgress buildDefaultRouteProgress(@Nullable Double stepDistanceRemaining) throws Exception {
    DirectionsRoute aRoute = buildDirectionsRoute();
    double stepDistanceRemainingFinal = stepDistanceRemaining == null ? 100 : stepDistanceRemaining;
    return buildRouteProgress(aRoute, stepDistanceRemainingFinal, 0, 0, 0, 0);
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }
}
