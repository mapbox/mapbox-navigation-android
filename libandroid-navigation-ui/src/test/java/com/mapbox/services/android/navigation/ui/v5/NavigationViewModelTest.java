package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationViewModelTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() throws Exception {
    NavigationViewModel navigationViewModel = buildNavigationViewModel();

    assertNotNull(navigationViewModel);
  }

  @Test
  @Ignore
  public void onInitializeNavigation_MapboxNavigationNotNull() throws Exception {
    NavigationViewModel navigationViewModel = buildNavigationViewModel();
    NavigationViewOptions options = NavigationViewOptions.builder().shouldSimulateRoute(true).build();
    NavigationViewEventDispatcher dispatcher = mock(NavigationViewEventDispatcher.class);

    navigationViewModel.initializeNavigation(options, dispatcher);

    assertNotNull(navigationViewModel.getNavigation());
  }

  @NonNull
  private NavigationViewModel buildNavigationViewModel() {
    return new NavigationViewModel(createMockApplication(), ACCESS_TOKEN);
  }

  @NonNull
  private Application createMockApplication() {
    Configuration testConfiguration = new Configuration();
    testConfiguration.setLocale(Locale.US);
    Resources mockResources = mock(Resources.class);
    Application mockApplication = mock(Application.class);
    AudioManager mockAudioManager = mock(AudioManager.class);
    LocationManager mockLocationManager = mock(LocationManager.class);
    when(mockResources.getConfiguration()).thenReturn(testConfiguration);
    when(mockApplication.getResources()).thenReturn(mockResources);
    when(mockApplication.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager);
    when(mockApplication.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mockLocationManager);
    when(mockApplication.getPackageManager()).thenReturn(mock(PackageManager.class));
    when(mockApplication.getApplicationContext()).thenReturn(mock(Context.class));
    return mockApplication;
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
