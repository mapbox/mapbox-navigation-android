package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class WaypointNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
  NavigationListener, RouteListener, ProgressChangeListener {

  private NavigationView navigationView;
  private boolean dropoffDialogShown;
  private Location lastKnownLocation;

  private List<Point> points = new ArrayList<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    points.add(Point.fromLngLat(-77.04012393951416, 38.9111117447887));
    points.add(Point.fromLngLat(-77.03847169876099, 38.91113678979344));
    points.add(Point.fromLngLat(-77.03848242759705, 38.91040213277608));
    points.add(Point.fromLngLat(-77.03850388526917, 38.909650771013034));
    points.add(Point.fromLngLat(-77.03651905059814, 38.90894949285854));
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
    fetchRoute(points.remove(0), points.remove(0));
  }

  @Override
  public void onCancelNavigation() {
    // Navigation canceled, finish the activity
    finish();
  }

  @Override
  public void onNavigationFinished() {
    // Intentionally empty
  }

  @Override
  public void onNavigationRunning() {
    // Intentionally empty
  }

  @Override
  public boolean allowRerouteFrom(Point offRoutePoint) {
    return true;
  }

  @Override
  public void onOffRoute(Point offRoutePoint) {

  }

  @Override
  public void onRerouteAlong(DirectionsRoute directionsRoute) {

  }

  @Override
  public void onFailedReroute(String errorMessage) {

  }

  @Override
  public void onArrival() {
    if (!dropoffDialogShown && !points.isEmpty()) {
      showDropoffDialog();
      dropoffDialogShown = true; // Accounts for multiple arrival events
    }
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    lastKnownLocation = location;
  }

  private void startNavigation(DirectionsRoute directionsRoute) {
    NavigationViewOptions navigationViewOptions = setupOptions(directionsRoute);
    navigationView.startNavigation(navigationViewOptions);
  }

  private void showDropoffDialog() {
    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    alertDialog.setMessage(getString(R.string.dropoff_dialog_text));
    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dropoff_dialog_positive_text),
      (dialogInterface, in) -> fetchRoute(getLastKnownLocation(), points.remove(0)));
    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dropoff_dialog_negative_text),
      (dialogInterface, in) -> {
        // Do nothing
      });

    alertDialog.show();
  }

  private void fetchRoute(Point origin, Point destination) {
    NavigationRoute.builder(this)
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination)
      .alternatives(true)
      .build()
      .getRoute(new SimplifiedCallback() {
        @Override
        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
          startNavigation(response.body().routes().get(0));
        }
      });
  }

  private NavigationViewOptions setupOptions(DirectionsRoute directionsRoute) {
    dropoffDialogShown = false;

    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.directionsRoute(directionsRoute)
      .navigationListener(this)
      .progressChangeListener(this)
      .routeListener(this)
      .shouldSimulateRoute(true);
    return options.build();
  }

  private Point getLastKnownLocation() {
    return Point.fromLngLat(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude());
  }
}
