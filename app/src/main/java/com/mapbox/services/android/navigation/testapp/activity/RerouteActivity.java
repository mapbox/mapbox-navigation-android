package com.mapbox.services.android.navigation.testapp.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.v5.location.MockLocationEngine;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

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
  ProgressChangeListener, MilestoneEventListener {

  @BindView(R.id.mapView)
  MapView mapView;
  @BindView(android.R.id.content)
  View contentLayout;
  @BindView(R.id.instructionView)
  InstructionView instructionView;

  private Point origin = Point.fromLngLat(-87.6900, 41.8529);
  private Point destination = Point.fromLngLat(-87.8921, 41.9794);
  private Polyline polyline;

  private LocationLayerPlugin locationLayerPlugin;
  private MockLocationEngine mockLocationEngine;
  private MapboxNavigation navigation;
  private MapboxMap mapboxMap;
  private boolean running;
  private boolean tracking;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.NavigationViewLight);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_reroute);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    // Initialize MapboxNavigation and add listeners
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().isDebugLoggingEnabled(true).build();
    navigation = new MapboxNavigation(getApplicationContext(), Mapbox.getAccessToken(), options);
    navigation.addNavigationEventListener(this);
    navigation.addMilestoneEventListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @SuppressLint("MissingPermission")
  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStart();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();

    shutdownLocationEngine();

    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStop();
    }

    if (navigation != null) {
      // End the navigation session
      navigation.stopNavigation();
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
    shutdownNavigation();
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setOnMapClickListener(this);

    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, null);
    locationLayerPlugin.setRenderMode(RenderMode.GPS);

    // Setup the mockLocationEngine
    mockLocationEngine = new MockLocationEngine(1000, 30, false);
    mockLocationEngine.addLocationEngineListener(this);
    navigation.setLocationEngine(mockLocationEngine);

    // Acquire the navigation route
    getRoute(origin, destination, null);
  }

  @Override
  public void onConnected() {
    // No-op - mock automatically begins pushing updates
  }

  @Override
  public void onLocationChanged(Location location) {
    if (!tracking) {
      locationLayerPlugin.forceLocationUpdate(location);
    }
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (!running || mapboxMap == null) {
      return;
    }

    mapboxMap.addMarker(new MarkerOptions().position(point));
    mapboxMap.setOnMapClickListener(null);

    Point newDestination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    mockLocationEngine.moveToLocation(newDestination);
    destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    tracking = false;
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
    Snackbar.make(contentLayout, "User Off Route", Snackbar.LENGTH_SHORT).show();
    mapboxMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));
  }


  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (tracking) {
      locationLayerPlugin.forceLocationUpdate(location);
      CameraPosition cameraPosition = new CameraPosition.Builder()
        .zoom(15)
        .target(new LatLng(location.getLatitude(), location.getLongitude()))
        .bearing(location.getBearing())
        .build();
      mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000);
    }
    instructionView.update(routeProgress);
  }

  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
    Timber.d("onMilestoneEvent - Current Instruction: " + instruction);
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    Timber.d(call.request().url().toString());
    if (response.body() != null) {
      if (!response.body().routes().isEmpty()) {
        // Extract the route
        DirectionsRoute route = response.body().routes().get(0);
        // Draw it on the map
        drawRoute(route);
        // Start mocking the new route
        resetLocationEngine(route);
        navigation.startNavigation(route);
        mapboxMap.setOnMapClickListener(this);
        tracking = true;
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    Timber.e("Getting directions failed: ", throwable);
  }

  private void getRoute(Point origin, Point destination, Float bearing) {
    Double heading = bearing == null ? null : bearing.doubleValue();
    NavigationRoute.builder(this)
      .origin(origin, heading, 90d)
      .destination(destination)
      .accessToken(Mapbox.getAccessToken())
      .build().getRoute(this);
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

  private void resetLocationEngine(DirectionsRoute directionsRoute) {
    mockLocationEngine.deactivate();
    mockLocationEngine.setRoute(directionsRoute);
  }

  private void shutdownLocationEngine() {
    if (mockLocationEngine != null) {
      mockLocationEngine.removeLocationEngineListener(this);
      mockLocationEngine.removeLocationUpdates();
      mockLocationEngine.deactivate();
    }
  }

  private void shutdownNavigation() {
    navigation.removeNavigationEventListener(this);
    navigation.removeProgressChangeListener(this);
    navigation.onDestroy();
  }
}