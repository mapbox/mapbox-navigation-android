package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationViewOptions;
import com.mapbox.navigation.ui.OnNavigationReadyCallback;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.listeners.RouteListener;
import com.mapbox.services.android.navigation.testapp.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WaypointNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, RouteListener, LocationObserver, RoutesObserver {

  private NavigationView navigationView;
  private boolean dropoffDialogShown;
  private Location lastKnownLocation;

  private List<Point> points = new ArrayList<>();

  private MapboxNavigation mapboxNavigation;

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
    mapboxNavigation = new MapboxNavigation(getApplicationContext(), Mapbox.getAccessToken());
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
    mapboxNavigation.registerRoutesObserver(this);
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
    mapboxNavigation.unregisterRoutesObserver(this);
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
      Toast.makeText(this, "You have arrived!", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onRawLocationChanged(@NotNull Location rawLocation) {

  }

  @Override
  public void onEnhancedLocationChanged(@NotNull Location enhancedLocation, @NotNull List<? extends Location> keyPoints) {
    lastKnownLocation = enhancedLocation;
  }

  /*
      RouteObserver
    */
  @Override
  public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
    startNavigation(routes.get(0));
  }

  /*
    RouteObserver end
  */

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
    mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                    .accessToken(Mapbox.getAccessToken())
                    .coordinates(Arrays.asList(origin, destination))
                    .alternatives(true)
                    .build()
    );
  }

  private NavigationViewOptions setupOptions(DirectionsRoute directionsRoute) {
    dropoffDialogShown = false;

    return NavigationViewOptions.builder()
            .directionsRoute(directionsRoute)
            .navigationListener(this)
            .locationObserver(this)
            .routeListener(this)
            .shouldSimulateRoute(true)
            .build();
  }

  private Point getLastKnownLocation() {
    return Point.fromLngLat(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude());
  }
}
