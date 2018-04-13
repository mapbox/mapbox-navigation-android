package com.mapbox.services.android.navigation.ui.v5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.HashMap;
import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType.NONE_SPECIFIED;

/**
 * Serves as a launching point for the custom drop-in UI, {@link NavigationView}.
 * <p>
 * Demonstrates the proper setup and usage of the view, including all lifecycle methods.
 */
public class NavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener {

  private static final String EMPTY_STRING = "";

  private NavigationView navigationView;
  private boolean isRunning;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    navigationView = findViewById(R.id.navigationView);
    navigationView.onCreate(savedInstanceState);
    navigationView.getNavigationAsync(this);
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
    outState.putBoolean(NavigationConstants.NAVIGATION_VIEW_RUNNING, isRunning);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    navigationView.onRestoreInstanceState(savedInstanceState);
    isRunning = savedInstanceState.getBoolean(NavigationConstants.NAVIGATION_VIEW_RUNNING);
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
  public void onNavigationReady() {
    MapboxNavigationOptions.Builder navigationOptions = MapboxNavigationOptions.builder();
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    if (!isRunning) {
      extractRoute(options);
      extractCoordinates(options);
    }
    extractConfiguration(options, navigationOptions);
    extractLocale(navigationOptions);
    extractUnitType(navigationOptions);

    options.navigationOptions(navigationOptions.build());
    navigationView.startNavigation(options.build());
    isRunning = true;
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
    options.directionsRoute(NavigationLauncher.extractRoute(this));
  }

  private void extractCoordinates(NavigationViewOptions.Builder options) {
    HashMap<String, Point> coordinates = NavigationLauncher.extractCoordinates(this);
    if (options.build().directionsRoute() == null && coordinates.size() > 0) {
      options.origin(coordinates.get(NavigationConstants.NAVIGATION_VIEW_ORIGIN));
      options.destination(coordinates.get(NavigationConstants.NAVIGATION_VIEW_DESTINATION));
    }
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

  private void extractLocale(MapboxNavigationOptions.Builder navigationOptions) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    String country = preferences.getString(NavigationConstants.NAVIGATION_VIEW_LOCALE_COUNTRY, EMPTY_STRING);
    String language = preferences.getString(NavigationConstants.NAVIGATION_VIEW_LOCALE_LANGUAGE, EMPTY_STRING);

    Locale locale;
    if (!language.isEmpty()) {
      locale = new Locale(language, country);
    } else {
      locale = LocaleUtils.getDeviceLocale(this);
    }

    navigationOptions.locale(locale);
  }

  private void extractUnitType(MapboxNavigationOptions.Builder navigationOptions) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    int unitType = preferences.getInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE, NONE_SPECIFIED);
    navigationOptions.unitType(unitType);
  }
}
