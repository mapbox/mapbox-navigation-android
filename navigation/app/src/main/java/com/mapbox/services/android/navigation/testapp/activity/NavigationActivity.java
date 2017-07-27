package com.mapbox.services.android.navigation.testapp.activity;

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
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.NavigationConstants;

import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapClickListener, ProgressChangeListener, MilestoneEventListener,
  LocationEngineListener {

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;
  @BindView(R.id.startRouteButton)
  Button startRouteButton;

  private MapboxMap mapboxMap;
  private List<Marker> pathMarkers = new ArrayList<>();

  // Navigation related variables
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private DirectionsRoute route;
  private NavigationMapRoute navigationMapRoute;
  private LocationLayerPlugin locationLayerPlugin;
  private Location userLocation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    navigation = new MapboxNavigation(this, Mapbox.getAccessToken());
  }

  @OnClick(R.id.startRouteButton)
  @SuppressWarnings( {"MissingPermission"})
  public void onStartRouteClick() {
    if (navigation != null && route != null) {

      // Hide the start button
      startRouteButton.setVisibility(View.INVISIBLE);

      // Attach all of our navigation listeners.

      navigation.addProgressChangeListener(this);

      navigation.addMilestoneEventListener(this);

      locationLayerPlugin.setLocationEngine(null);
      locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);

      navigation.setLocationEngine(locationEngine);
      navigation.startNavigation(route);
    }
  }

  @Override
  @SuppressWarnings( {"MissingPermission"})
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    setupLocation();

    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
    locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);

    navigationMapRoute = new NavigationMapRoute(navigation, mapView, mapboxMap);

    mapboxMap.setOnMapClickListener(this);
    Snackbar.make(mapView, "Tap map to place destination", BaseTransientBottomBar.LENGTH_LONG).show();
  }

  @SuppressWarnings( {"MissingPermission"})
  private void setupLocation() {
    locationEngine = new LostLocationEngine(this);
    locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
    locationEngine.setFastestInterval(0);
    locationEngine.setInterval(1000);
    locationEngine.addLocationEngineListener(this);
    locationEngine.activate();
    userLocation = locationEngine.getLastLocation();
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (pathMarkers.size() >= 2) {
      Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show();
      return;
    }
    Marker marker = mapboxMap.addMarker(new MarkerOptions().position(point));
    pathMarkers.add(marker);

    startRouteButton.setVisibility(View.VISIBLE);
    calculateRoute();
  }

  @Override
  @SuppressWarnings( {"MissingPermission"})
  public void onConnected() {
    Timber.d("onConnected called");
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    Timber.d("new location: %s", location.toString());
    userLocation = location;
  }

  private void calculateRoute() {
    if (userLocation == null) {
      Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
      return;
    }

    Position origin = Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude());
    Position destination = Position.fromLngLat(
      pathMarkers.get(pathMarkers.size() - 1).getPosition().getLongitude(),
      pathMarkers.get(pathMarkers.size() - 1).getPosition().getLatitude()
    );
    if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
      for (Marker marker : pathMarkers) {
        mapboxMap.removeMarker(marker);
      }
      startRouteButton.setVisibility(View.GONE);
      return;
    }

    List<Position> coordinates = new ArrayList<>();
    coordinates.add(origin);

    for (Marker marker : pathMarkers) {
      coordinates.add(Position.fromLngLat(marker.getPosition().getLongitude(), marker.getPosition().getLatitude()));
    }

    navigation.getRoute(coordinates, null, new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        Timber.d("Url: %s", call.request().url().toString());
        if (response.body() != null) {
          if (response.body().getRoutes().size() > 0) {
            DirectionsRoute route = response.body().getRoutes().get(0);
            NavigationActivity.this.route = route;
            navigationMapRoute.addRoute(route);
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
      default:
        Toast.makeText(this, "Undefined milestone event occurred", Toast.LENGTH_LONG).show();
        break;
    }
  }



  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayerPlugin.forceLocationUpdate(location);
    Timber.d("onProgressChange: fraction of route traveled: %f", routeProgress.getFractionTraveled());
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
  @SuppressWarnings( {"MissingPermission"})
  protected void onStart() {
    super.onStart();
    navigation.onStart();
    mapView.onStart();
    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStart();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    navigation.onStop();
    mapView.onStop();
    locationLayerPlugin.onStop();
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

    navigation.removeProgressChangeListener(this);
    navigation.removeMilestoneEventListener(this);

    navigation.onDestroy();

    // End the navigation session
    navigation.endNavigation();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}
