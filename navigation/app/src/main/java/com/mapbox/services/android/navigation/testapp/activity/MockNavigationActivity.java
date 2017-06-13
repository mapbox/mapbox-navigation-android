package com.mapbox.services.android.navigation.testapp.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.v5.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.NavigationConstants;
import com.mapbox.services.android.navigation.v5.RouteProgress;
import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.NavigationMilestone;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MockNavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapClickListener, ProgressChangeListener, NavigationEventListener, AlertLevelChangeListener,
  OffRouteListener, MilestoneEventListener {

  private static final int ON_NEW_STEP_MILESTONE = 1;

  // Map variables
  private MapView mapView;
  private Polyline routeLine;
  private MapboxMap mapboxMap;
  private Marker destinationMarker;

  // Navigation related variables
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private Button startRouteButton;
  private DirectionsRoute route;
  private Position destination;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_activity);

    mapView = (MapView) findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    navigation = new MapboxNavigation(this, Mapbox.getAccessToken());

    startRouteButton = (Button) findViewById(R.id.startRouteButton);
    startRouteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (navigation != null && route != null) {

          // Hide the start button
          startRouteButton.setVisibility(View.INVISIBLE);

          // Attach all of our navigation listeners.
          navigation.addNavigationEventListener(MockNavigationActivity.this);
          navigation.addProgressChangeListener(MockNavigationActivity.this);
          navigation.addAlertLevelChangeListener(MockNavigationActivity.this);

          // Adjust location engine to force a gps reading every second. This isn't required but gives an overall
          // better navigation experience for users. The updating only occurs if the user moves 3 meters or further
          // from the last update.
          locationEngine.setInterval(0);
          locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
          locationEngine.setFastestInterval(1000);
          locationEngine.activate();

          NavigationMilestone newStepMilestone = new NavigationMilestone.Builder()
            .triggerOnNewStep(true)
            .setIdentifier(ON_NEW_STEP_MILESTONE)
            .build();

          navigation.addMilestone(newStepMilestone);
          navigation.addMileStoneEventListener(MockNavigationActivity.this);

          ((MockLocationEngine) locationEngine).setRoute(route);
          navigation.setLocationEngine(locationEngine);
          navigation.startNavigation(route);
        }
      }
    });
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    mapboxMap.setOnMapClickListener(this);
    Snackbar.make(mapView, "Tap map to place destination", BaseTransientBottomBar.LENGTH_LONG).show();

    mapboxMap.moveCamera(CameraUpdateFactory.zoomBy(12));

    locationEngine = new MockLocationEngine();
    mapboxMap.setLocationSource(locationEngine);

    if (PermissionsManager.areLocationPermissionsGranted(this)) {
      mapboxMap.setMyLocationEnabled(true);
      mapboxMap.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
      mapboxMap.getTrackingSettings().setDismissAllTrackingOnGesture(false);
    }
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (destinationMarker != null) {
      mapboxMap.removeMarker(destinationMarker);
    }
    destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));

    startRouteButton.setVisibility(View.VISIBLE);

    this.destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());
    calculateRoute();
  }

  private void drawRouteLine(DirectionsRoute route) {
    List<Position> positions = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6).getCoordinates();
    List<LatLng> latLngs = new ArrayList<>();
    for (Position position : positions) {
      latLngs.add(new LatLng(position.getLatitude(), position.getLongitude()));
    }

    // Remove old route if currently being shown on map.
    if (routeLine != null) {
      mapboxMap.removePolyline(routeLine);
    }

    routeLine = mapboxMap.addPolyline(new PolylineOptions()
      .addAll(latLngs)
      .color(Color.parseColor("#56b881"))
      .width(5f));
  }

  private void calculateRoute() {
    Location userLocation = mapboxMap.getMyLocation();
    if (userLocation == null) {
      Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
      return;
    }

    Position origin = (Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude()));
    if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
      mapboxMap.removeMarker(destinationMarker);
      startRouteButton.setVisibility(View.GONE);
      return;
    }

    navigation.getRoute(origin, destination, new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        DirectionsRoute route = response.body().getRoutes().get(0);
        MockNavigationActivity.this.route = route;
        drawRouteLine(route);
      }

      @Override
      public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
        Timber.e("onFailure: navigation.getRoute()", throwable);
      }
    });
  }

  /*
   * Navigation listeners
   */

  @Override
  public void onRunning(boolean running) {
    if (running) {
      Timber.d("onRunning: Started");
    } else {
      Timber.d("onRunning: Stopped");
    }
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    Timber.d("onProgressChange: fraction of route traveled: %f", routeProgress.getFractionTraveled());
  }

  @Override
  public void onAlertLevelChange(int alertLevel, RouteProgress routeProgress) {

    switch (alertLevel) {
      case NavigationConstants.HIGH_ALERT_LEVEL:
        Toast.makeText(MockNavigationActivity.this, "HIGH", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.MEDIUM_ALERT_LEVEL:
        Toast.makeText(MockNavigationActivity.this, "MEDIUM", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.LOW_ALERT_LEVEL:
        Toast.makeText(MockNavigationActivity.this, "LOW", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.ARRIVE_ALERT_LEVEL:
        Toast.makeText(MockNavigationActivity.this, "ARRIVE", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.DEPART_ALERT_LEVEL:
        Toast.makeText(MockNavigationActivity.this, "DEPART", Toast.LENGTH_LONG).show();
        break;
      default:
      case NavigationConstants.NONE_ALERT_LEVEL:
        Toast.makeText(MockNavigationActivity.this, "NONE", Toast.LENGTH_LONG).show();
        break;
    }
  }

  @Override
  public void userOffRoute(Location location) {
    Position newOrigin = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    navigation.getRoute(newOrigin, destination, location.getBearing(), new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        DirectionsRoute route = response.body().getRoutes().get(0);
        MockNavigationActivity.this.route = route;

        // Remove old route line from map and draw the new one.
        if (routeLine != null) {
          mapboxMap.removePolyline(routeLine);
        }
        drawRouteLine(route);
      }

      @Override
      public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
        Timber.e("onFailure: navigation.getRoute()", throwable);
      }
    });
  }

  /*
   * Activity lifecycle methods
   */

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStart() {
    super.onStart();
    navigation.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    navigation.onStop();
    mapView.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();

    // Remove all navigation listeners
    navigation.removeAlertLevelChangeListener(this);
    navigation.removeNavigationEventListener(this);
    navigation.removeProgressChangeListener(this);
    navigation.removeOffRouteListener(this);

    // End the navigation session
    navigation.endNavigation();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, NavigationMilestone milestone) {
    Timber.d("MileStone Event Occurred");
  }
}
