package com.mapbox.services.android.navigation.testapp.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mapbox.android.core.location.LocationEngineListener;
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
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class OfflineRerouteActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
  MapboxMap.OnMapClickListener, NavigationEventListener, OffRouteListener,
  ProgressChangeListener, MilestoneEventListener {

  @BindView(R.id.mapView)
  MapView mapView;
  @BindView(android.R.id.content)
  View contentLayout;
  @BindView(R.id.instructionView)
  InstructionView instructionView;

  private Point origin = Point.fromLngLat(-3.714873, 40.397389);
  private Point destination = Point.fromLngLat(-3.712331, 40.401686);
  private Polyline polyline;

  private ReplayRouteLocationEngine mockLocationEngine;
  private MapboxNavigation navigation;
  private MapboxMap mapboxMap;
  private boolean running;
  private boolean tracking;
  private DirectionsRoute route;
  private Point newDestination;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.NavigationViewLight);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_reroute);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);

    // Initialize MapboxNavigation and add listeners
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().isDebugLoggingEnabled(true).build();
    navigation = new MapboxNavigation(getApplicationContext(), Mapbox.getAccessToken(), options);
    navigation.addNavigationEventListener(this);
    navigation.addMilestoneEventListener(this);

    String tilesDirPath = obtainOfflineDirectoryFor("tiles");
    Timber.d("Tiles directory path: %s", tilesDirPath);
    String translationsDirPath = obtainOfflineDirectoryFor("translations");
    Timber.d("Translations directory path: %s", translationsDirPath);

    navigation.initializeOfflineData(tilesDirPath, translationsDirPath);

    mapView.getMapAsync(this);
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
    mapboxMap.addOnMapClickListener(this);

    LocationComponent locationComponent = mapboxMap.getLocationComponent();
    locationComponent.activateLocationComponent(this);
    locationComponent.setLocationComponentEnabled(true);
    locationComponent.setRenderMode(RenderMode.GPS);

    // Setup the mockLocationEngine
    mockLocationEngine = new ReplayRouteLocationEngine();
    mockLocationEngine.addLocationEngineListener(this);
    navigation.setLocationEngine(mockLocationEngine);

    String routeUri = obtainRouteUri(origin, destination);
    route = navigation.findOfflineRouteFor(routeUri);
    handleNewRoute(route);
  }

  @Override
  public void onConnected() {
    // No-op - mock automatically begins pushing updates
  }

  @Override
  public void onLocationChanged(Location location) {
    if (!tracking) {
      mapboxMap.getLocationComponent().forceLocationUpdate(location);
    }
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (!running || mapboxMap == null) {
      return;
    }

    mapboxMap.addMarker(new MarkerOptions().position(point));
    mapboxMap.removeOnMapClickListener(this);

    newDestination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    mockLocationEngine.moveTo(newDestination);
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
    Snackbar.make(contentLayout, "User Off Route", Snackbar.LENGTH_SHORT).show();
    mapboxMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));
    Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    String routeUri = obtainRouteUri(newOrigin, newDestination);
    route = navigation.findOfflineRouteFor(routeUri);
    handleNewRoute(route);
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (tracking) {
      mapboxMap.getLocationComponent().forceLocationUpdate(location);
      animateCameraFor(location);
    }
    instructionView.updateDistanceWith(routeProgress);
  }

  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
    if (milestone instanceof VoiceInstructionMilestone) {
      Snackbar.make(contentLayout, instruction, Snackbar.LENGTH_SHORT).show();
    }
    instructionView.updateBannerInstructionsWith(milestone);
    Timber.d("onMilestoneEvent - Current Instruction: " + instruction);
  }

  private boolean checkRoute() {
    if (route != null) {
      Snackbar.make(contentLayout, "Offline route found", Snackbar.LENGTH_SHORT).show();
      return true;
    } else {
      Snackbar.make(contentLayout, "Offline route not found", Snackbar.LENGTH_SHORT).show();
      return false;
    }
  }

  private String obtainOfflineDirectoryFor(String fileName) {
    File offline = Environment.getExternalStoragePublicDirectory("Offline");
    if (!offline.exists()) {
      Timber.d("Offline directory does not exist");
    }
    File file = new File(offline, fileName);
    return file.getAbsolutePath();
  }

  private void startNavigation(DirectionsRoute route) {
    navigation.startNavigation(route);
    mapboxMap.addOnMapClickListener(this);
    tracking = true;
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
      polyline = mapboxMap.addPolyline(new PolylineOptions()
        .addAll(points)
        .color(Color.parseColor(getString(R.string.blue)))
        .width(5));
    }
  }

  private String obtainRouteUri(Point origin, Point destination) {
    return NavigationRoute.builder(this)
      .origin(origin)
      .destination(destination)
      .accessToken(Mapbox.getAccessToken())
      .build().getCall().request().url().toString();
  }

  private void handleNewRoute(DirectionsRoute route) {
    if (!checkRoute()) {
      return;
    }
    drawRoute(route);
    resetLocationEngine(route);
    startNavigation(route);
  }

  private void animateCameraFor(Location location) {
    CameraPosition cameraPosition = new CameraPosition.Builder()
      .zoom(15)
      .target(new LatLng(location.getLatitude(), location.getLongitude()))
      .bearing(location.getBearing())
      .build();
    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000);
  }

  private void resetLocationEngine(DirectionsRoute directionsRoute) {
    mockLocationEngine.deactivate();
    mockLocationEngine.assign(directionsRoute);
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
