package com.mapbox.services.android.navigation.ui.v5;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.HashMap;

/**
 * Serves as a launching point for the custom drop-in UI, {@link NavigationView}.
 * <p>
 * Demonstrates the proper setup and usage of the view, including all lifecycle methods.
 */
public class WaypointNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener, ProgressChangeListener {

  private NavigationView navigationView;
  private boolean dropoffDialogShown;

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
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    navigationView.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  public void onNavigationReady() {
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    options.progressChangeListener(this);
    extractRoute(options);
    extractCoordinates(options);
    extractConfiguration(options);
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

    MapboxNavigationOptions navigationOptions = MapboxNavigationOptions.builder()
      .unitType(preferences.getInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE,
        NavigationUnitType.TYPE_IMPERIAL))
      .build();
    options.navigationOptions(navigationOptions);
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (RouteUtils.isArrivalEvent(routeProgress)) {

      if (!dropoffDialogShown) {
        showDropoffDialog();
        dropoffDialogShown = true;
      }
    }
  }

  private void showDropoffDialog() {
    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    alertDialog.setMessage("Do you want to navigate to next destination?");
    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Start Next", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {

      }
    });
    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {

      }
    });

    alertDialog.show();
  }
}
