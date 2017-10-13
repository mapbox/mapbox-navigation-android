package com.mapbox.services.android.navigation.testapp.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.v5.mock.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class RerouteActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
  Callback<DirectionsResponse>, MapboxMap.OnMapClickListener, NavigationEventListener, OffRouteListener,
  ProgressChangeListener {

  @BindView(R.id.mapView)
  MapView mapView;

  private LocationLayerPlugin locationLayerPlugin;
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private MapboxMap mapboxMap;
  private boolean running;
  private Polyline polyline;

  Point origin = Point.fromLngLat(-87.6900, 41.8529);
  Point destination = Point.fromLngLat(-87.8921, 41.9794);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_reroute);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    // Initialize MapboxNavigation and add listeners
    navigation = new MapboxNavigation(this, Mapbox.getAccessToken());
    navigation.addNavigationEventListener(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setOnMapClickListener(this);

    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, null);
    locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);

    // Use the mockLocationEngine
    locationEngine = new MockLocationEngine(1000, 30, false);
    locationEngine.addLocationEngineListener(this);

    mapboxMap.setLocationSource(locationEngine);
    mapboxMap.setMyLocationEnabled(true);
    navigation.setLocationEngine(locationEngine);

    // Acquire the navigation's route
    getRoute(origin, destination, null);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (!running || mapboxMap == null) {
      return;
    }

    mapboxMap.addMarker(new MarkerOptions().position(point));
    mapboxMap.setOnMapClickListener(null);

    Point newDestination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    ((MockLocationEngine) locationEngine).moveToLocation(newDestination);
  }

  @Override
  public void onRunning(boolean running) {
    this.running = running;
    if (running) {
      navigation.addOffRouteListener(this);
      navigation.addProgressChangeListener(this);
    }
  }

  @Override
  public void userOffRoute(Location location) {
    Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    getRoute(newOrigin, destination, location.getBearing());
    Timber.d("offRoute");
    mapboxMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));
  }


  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayerPlugin.forceLocationUpdate(location);
    Timber.d("onRouteProgressChange: %s", routeProgress.currentLegProgress().stepIndex());
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (response.body() != null) {
      if (!response.body().routes().isEmpty()) {
        DirectionsRoute route = response.body().routes().get(0);
        // First run
        drawRoute(route);
        if (!running) {
          ((MockLocationEngine) locationEngine).setRoute(route);
        }
        navigation.startNavigation(route);
      }
    }
  }

  private void getRoute(Point origin, Point destination, Float bearing) {
    NavigationRoute.Builder navigationRouteBuilder = NavigationRoute.builder()
      .origin(origin)
      .destination(destination)
      .accessToken(Mapbox.getAccessToken());

    if (bearing != null) {
      navigationRouteBuilder.addBearing(Float.valueOf(bearing).doubleValue(), 90d);
    }
    navigationRouteBuilder.build().getRoute(this);
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    Timber.e("Getting directions failed: ", throwable);
  }

  private void drawRoute(DirectionsRoute route) {
    List<LatLng> points = new ArrayList<>();
    List<Point> coords = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6).coordinates();

    for (Point point : coords) {
      points.add(new LatLng(point.latitude(), point.longitude()));
    }

    if (!points.isEmpty()) {

      if (polyline != null) {
        mapboxMap.removePolyline(polyline);
      }

      // Draw polyline on map
      polyline = mapboxMap.addPolyline(new PolylineOptions()
        .addAll(points)
        .color(Color.parseColor("#4264fb"))
        .width(5));
    }
  }

  @Override
  @SuppressWarnings( {"MissingPermission"})
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {

  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
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

    if (locationEngine != null) {
      locationEngine.removeLocationEngineListener(this);
      locationEngine.removeLocationUpdates();
      locationEngine.deactivate();
    }

    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStop();
    }

    if (navigation != null) {
      // End the navigation session
      navigation.endNavigation();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
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
    navigation.removeNavigationEventListener(this);
    navigation.removeProgressChangeListener(this);

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