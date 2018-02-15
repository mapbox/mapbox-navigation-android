package com.mapbox.services.android.navigation.ui.v5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

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
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
  }

  @Override
  public void onNavigationReady() {
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    if (!isRunning) {
      extractRoute(options);
      extractCoordinates(options);
    }
    extractConfiguration(options);
    extractLocale(options);
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
    if (coordinates.size() > 0) {
      options.origin(coordinates.get(NavigationConstants.NAVIGATION_VIEW_ORIGIN));
      options.destination(coordinates.get(NavigationConstants.NAVIGATION_VIEW_DESTINATION));
    }
  }

  private void extractConfiguration(NavigationViewOptions.Builder options) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    options.awsPoolId(preferences
      .getString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, null));
    options.shouldSimulateRoute(preferences
      .getBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, false));
  }

  private void extractLocale(NavigationViewOptions.Builder options) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    String country = preferences.getString(NavigationConstants.NAVIGATION_VIEW_LOCALE_COUNTRY, "");
    String language = preferences.getString(NavigationConstants.NAVIGATION_VIEW_LOCALE_LANGUAGE, "");
    int unitType = preferences.getInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE, NONE_SPECIFIED);

    Locale locale;
    if (!language.isEmpty()) {
      locale = new Locale(language, country);
    } else {
      locale = LocaleUtils.getDeviceLocale(this);
    }

    MapboxNavigationOptions navigationOptions = MapboxNavigationOptions.builder()
      .locale(locale)
      .unitType(unitType)
      .build();
    options.navigationOptions(navigationOptions);
  }
}
