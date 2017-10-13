package com.mapbox.services.android.navigation.testapp.activity;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.Utils;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.mock.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MockNavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapClickListener, ProgressChangeListener, NavigationEventListener,
  MilestoneEventListener, OffRouteListener {

  private static final int BEGIN_ROUTE_MILESTONE = 1001;

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;

  @BindView(R.id.newLocationFab)
  FloatingActionButton newLocationFab;

  @BindView(R.id.startRouteButton)
  Button startRouteButton;

  private MapboxMap mapboxMap;
  private boolean running;

  // Navigation related variables
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private DirectionsRoute route;
  private NavigationMapRoute navigationMapRoute;
  private LocationLayerPlugin locationLayerPlugin;
  private Point destination;
  private Point waypoint;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mock_navigation);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    navigation = new MapboxNavigation(this, Mapbox.getAccessToken());

    navigation.addMilestone(new RouteMilestone.Builder()
      .setIdentifier(BEGIN_ROUTE_MILESTONE)
      .setInstruction(new BeginRouteInstruction())
      .setTrigger(
        Trigger.all(
          Trigger.lt(TriggerProperty.STEP_INDEX, 3),
          Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
          Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
        )
      ).build());
  }

  @OnClick(R.id.startRouteButton)
  public void onStartRouteClick() {
    if (navigation != null && route != null) {

      // Hide the start button
      startRouteButton.setVisibility(View.INVISIBLE);

      // Attach all of our navigation listeners.
      navigation.addNavigationEventListener(this);
      navigation.addProgressChangeListener(this);
      navigation.addMilestoneEventListener(this);
      navigation.addOffRouteListener(this);

      ((MockLocationEngine) locationEngine).setRoute(route);
      navigation.setLocationEngine(locationEngine);
      navigation.startNavigation(route);
      mapboxMap.setOnMapClickListener(null);
    }
  }

  @OnClick(R.id.newLocationFab)
  public void onNewLocationClick() {
    newOrigin();
  }

  private void newOrigin() {
    if (mapboxMap != null) {
      LatLng latLng = Utils.getRandomLatLng(new double[] {-77.1825, 38.7825, -76.9790, 39.0157});
      ((MockLocationEngine) locationEngine).setLastLocation(
        Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())
      );
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
      mapboxMap.setMyLocationEnabled(true);
      mapboxMap.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
    }
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, null);
    locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);

    navigationMapRoute = new NavigationMapRoute(navigation, mapView, mapboxMap);

    mapboxMap.setOnMapClickListener(this);
    Snackbar.make(mapView, "Tap map to place waypoint", BaseTransientBottomBar.LENGTH_LONG).show();

    locationEngine = new MockLocationEngine(1000, 50, true);
    mapboxMap.setLocationSource(locationEngine);

    newOrigin();
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (destination == null) {
      destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    } else if (waypoint == null) {
      waypoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    } else {
      Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show();
    }
    mapboxMap.addMarker(new MarkerOptions().position(point));

    startRouteButton.setVisibility(View.VISIBLE);
    calculateRoute();
  }

  private void calculateRoute() {
    Location userLocation = mapboxMap.getMyLocation();
    if (userLocation == null) {
      Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
      return;
    }

    Point origin = Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude());
    if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
      startRouteButton.setVisibility(View.GONE);
      return;
    }

    NavigationRoute.Builder navigationRouteBuilder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken());
    navigationRouteBuilder.origin(origin);
    navigationRouteBuilder.destination(destination);
    if (waypoint != null) {
      navigationRouteBuilder.addWaypoint(waypoint);
    }

    navigationRouteBuilder.build().getRoute(new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        Timber.d("Url: %s", call.request().url().toString());
        if (response.body() != null) {
          if (!response.body().routes().isEmpty()) {
            DirectionsRoute directionsRoute = response.body().routes().get(0);
            MockNavigationActivity.this.route = directionsRoute;
            navigationMapRoute.addRoute(directionsRoute);

            /*for (LegStep step: route.getLegs().get(0).getSteps()) {
              mapboxMap.addMarker(new MarkerOptions().position(new LatLng(
                  step.getManeuver().asPosition().getLatitude(), step.getManeuver().asPosition().getLongitude())));
             }*/
          }
        }
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
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    Timber.d("Milestone Event Occurred with id: %d", identifier);
    switch (identifier) {
      case NavigationConstants.URGENT_MILESTONE:
        Toast.makeText(this, "Urgent Milestone", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.IMMINENT_MILESTONE:
        Toast.makeText(this, "Imminent Milestone", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.NEW_STEP_MILESTONE:
        Toast.makeText(this, "New Step", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.DEPARTURE_MILESTONE:
        Toast.makeText(this, "Depart", Toast.LENGTH_LONG).show();
        break;
      case NavigationConstants.ARRIVAL_MILESTONE:
        Toast.makeText(this, "Arrival", Toast.LENGTH_LONG).show();
        break;
      case BEGIN_ROUTE_MILESTONE:
        Toast.makeText(this, "you should reach your destination by", Toast.LENGTH_LONG).show();
        break;
      default:
        Toast.makeText(this, "Undefined milestone event occurred", Toast.LENGTH_LONG).show();
        break;
    }
  }

  @Override
  public void onRunning(boolean running) {
    this.running = running;
    if (running) {
      Timber.d("onRunning: Started");
    } else {
      Timber.d("onRunning: Stopped");
    }
  }

  @Override
  public void userOffRoute(Location location) {
    Toast.makeText(this, "off-route called", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayerPlugin.forceLocationUpdate(location);
    Timber.d("onProgressChange: fraction of route traveled: %f", routeProgress.fractionTraveled());
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
    mapView.onStart();
    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStart();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStop();
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigation.onDestroy();
    locationEngine.removeLocationUpdates();
    locationEngine.deactivate();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  private static class BeginRouteInstruction extends Instruction {

    @Override
    public String buildInstruction(RouteProgress routeProgress) {
      return "Have a safe trip!";
    }
  }
}
