package com.mapbox.services.android.navigation.ui.v5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
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
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    setContentView(R.layout.activity_navigation);
    navigationView = findViewById(R.id.navigationView);
    navigationView.onCreate(savedInstanceState);
    navigationView.initialize(this);
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
    MapboxNavigationOptions.Builder navigationOptions = MapboxNavigationOptions.builder();
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    extractRoute(options);
    extractConfiguration(options, navigationOptions);
    options.navigationOptions(navigationOptions.build());
    navigationView.startNavigation(options.build());
  }

  @Override
  public void onCancelNavigation() {
    // Navigation canceled, finish the activity
    finish();
  }

  @Override
  public void onNavigationFinished() {
    // Navigation finished, finish the activity
    finish();
  }

  @Override
  public void onNavigationRunning() {
    // Intentionally empty
  }

  private void extractRoute(NavigationViewOptions.Builder options) {
    DirectionsRoute route = NavigationLauncher.extractRoute(this);
    options.directionsRoute(route);
  }

  private void extractConfiguration(NavigationViewOptions.Builder options,
                                    MapboxNavigationOptions.Builder navigationOptions) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    options.shouldSimulateRoute(preferences
      .getBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, false));
    options.directionsProfile(preferences
      .getString(NavigationConstants.NAVIGATION_VIEW_ROUTE_PROFILE_KEY, DirectionsCriteria.PROFILE_DRIVING_TRAFFIC));
    navigationOptions.enableOffRouteDetection(preferences
      .getBoolean(NavigationConstants.NAVIGATION_VIEW_OFF_ROUTE_ENABLED_KEY, true));
    navigationOptions.snapToRoute(preferences
      .getBoolean(NavigationConstants.NAVIGATION_VIEW_SNAP_ENABLED_KEY, true));
  }
}
