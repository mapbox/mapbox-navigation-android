package com.mapbox.services.android.navigation.testapp.test;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;

public class TestNavigationActivity extends AppCompatActivity implements NavigationListener,
  OnNavigationReadyCallback {

  private static final String DEFAULT_EMPTY_STRING = "";
  private static final String TEST_ROUTE_JSON = "test_route_json";
  private NavigationView navigationView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    navigationView = findViewById(R.id.navigationView);
    navigationView.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      navigationView.initialize(this);
    }
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
    if (isRunning) {
      DirectionsRoute route = retrieveRouteForRotation();
      if (route != null) {
        navigationView.startNavigation(buildTestNavigationViewOptions(route));
      }
    }
  }

  @Override
  public void onCancelNavigation() {
    finish();
  }

  @Override
  public void onNavigationFinished() {
    finish();
  }

  @Override
  public void onNavigationRunning() {
    // No-impl
  }

  private NavigationViewOptions buildTestNavigationViewOptions(DirectionsRoute route) {
    return NavigationViewOptions.builder()
      .directionsRoute(route)
      .shouldSimulateRoute(true)
      .build();
  }

  @Nullable
  private DirectionsRoute retrieveRouteForRotation() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    String testRouteJson = preferences.getString(TEST_ROUTE_JSON, DEFAULT_EMPTY_STRING);
    if (TextUtils.isEmpty(testRouteJson)) {
      return null;
    }
    return DirectionsRoute.fromJson(testRouteJson);
  }
}
