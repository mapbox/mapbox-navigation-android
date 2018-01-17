package com.mapbox.services.android.navigation.ui.v5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import java.util.HashMap;

/**
 * Serves as a launching point for the custom drop-in UI, {@link NavigationView}.
 * <p>
 * Demonstrates the proper setup and usage of the view, including all lifecycle methods.
 */
public class EmbeddedNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener {

  private NavigationView navigationView;
  private TextView currentSpeedWidget;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_embedded_navigation);
    navigationView = findViewById(R.id.navigationView);
    navigationView.onCreate(savedInstanceState);

    currentSpeedWidget = findViewById(R.id.speed_limit);
    setSpeedWidgetAnchor(R.id.summaryBottomSheet);

    navigationView.getNavigationAsync(this);
  }

  private void setSpeedWidgetAnchor(@IdRes int res) {
    CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) currentSpeedWidget.getLayoutParams();
    layoutParams.setAnchorId(res);
    currentSpeedWidget.setLayoutParams(layoutParams);
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
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
  }

  @Override
  public void onNavigationReady() {
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    extractRoute(options);
    extractCoordinates(options);
    extractConfiguration(options);
    setBottomSheetCallback(options);

    navigationView.startNavigation(options.build());
  }

  private void setBottomSheetCallback(NavigationViewOptions.Builder options) {
    options.bottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {
        switch (newState) {
          case BottomSheetBehavior.STATE_HIDDEN:
            setSpeedWidgetAnchor(R.id.recenterBtn);
            break;
          case BottomSheetBehavior.STATE_EXPANDED:
          case BottomSheetBehavior.STATE_COLLAPSED:
            setSpeedWidgetAnchor(R.id.summaryBottomSheet);
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {

      }
    });
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
}
