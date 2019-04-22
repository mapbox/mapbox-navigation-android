package com.mapbox.services.android.navigation.ui.v5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

/**
 * Serves as a launching point for the custom drop-in UI, {@link NavigationView}.
 * <p>
 * Demonstrates the proper setup and usage of the view, including all lifecycle methods.
 */
public class MapboxNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
  NavigationListener {

  private NavigationView navigationView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    navigationView = findViewById(R.id.navigationView);
    navigationView.onCreate(savedInstanceState);
    initialize();
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onBackPressed() {
    // If the navigation view didn't need to do anything, call super
    if (!navigationView.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    navigationView.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  public void onPause() {
    super.onPause();
    navigationView.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    navigationView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    extractRoute(options);
    extractConfiguration(options);
    options.navigationOptions(MapboxNavigationOptions.builder().build());
    navigationView.startNavigation(options.build());
  }

  @Override
  public void onCancelNavigation() {
    finishNavigation();
  }

  @Override
  public void onNavigationFinished() {
    finishNavigation();
  }

  @Override
  public void onNavigationRunning() {
    // Intentionally empty
  }

  private void initialize() {
    Parcelable position = getIntent().getParcelableExtra(NavigationConstants.NAVIGATION_VIEW_INITIAL_MAP_POSITION);
    if (position != null) {
      navigationView.initialize(this, (CameraPosition) position);
    } else {
      navigationView.initialize(this);
    }
  }

  private void extractRoute(NavigationViewOptions.Builder options) {
    DirectionsRoute route = NavigationLauncher.extractRoute(this);
    options.directionsRoute(route);
  }

  private void extractConfiguration(NavigationViewOptions.Builder options) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    options.shouldSimulateRoute(preferences.getBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, false));
    String offlinePath = preferences.getString(NavigationConstants.OFFLINE_PATH_KEY, "");
    if (!offlinePath.isEmpty()) {
      options.offlineRoutingTilesPath(offlinePath);
    }
    String offlineVersion = preferences.getString(NavigationConstants.OFFLINE_VERSION_KEY, "");
    if (!offlineVersion.isEmpty()) {
      options.offlineRoutingTilesVersion(offlineVersion);
    }
    String offlineMapDatabasePath = preferences.getString(NavigationConstants.MAP_DATABASE_PATH_KEY, "");
    if (!offlineMapDatabasePath.isEmpty()) {
      options.offlineMapDatabasePath(offlineMapDatabasePath);
    }
  }

  private void finishNavigation() {
    NavigationLauncher.cleanUpPreferences(this);
    finish();
  }
}
