package com.mapbox.services.android.navigation.testapp.activity;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.ArrayList;
import java.util.List;

public class WaypointNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
  NavigationListener, ProgressChangeListener {

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
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
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
    navigationView.startNavigation(setupOptions(points.remove(0)));
  }

  private NavigationViewOptions setupOptions(Point origin) {
    dropoffDialogShown = false;

    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    options.progressChangeListener(this);
    options.origin(origin);
    options.destination(points.remove(0));
    options.shouldSimulateRoute(true);
    return options.build();
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
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (RouteUtils.isArrivalEvent(routeProgress)) {
      lastKnownLocation = location; // Accounts for driver moving after dialog was triggered
      if (!dropoffDialogShown && !points.isEmpty()) {
        showDropoffDialog();
        dropoffDialogShown = true; // Accounts for multiple arrival events
      }
    }
  }

  private void showDropoffDialog() {
    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    alertDialog.setMessage(getString(R.string.dropoff_dialog_text));
    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dropoff_dialog_positive_text),
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int in) {
          navigationView.startNavigation(
            setupOptions(Point.fromLngLat(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude())));
        }
      });
    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dropoff_dialog_negative_text),
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int in) {
          // Do nothing
        }
      });

    alertDialog.show();
  }
}
