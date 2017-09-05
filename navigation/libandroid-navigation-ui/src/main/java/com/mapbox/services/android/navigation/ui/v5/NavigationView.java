package com.mapbox.services.android.navigation.ui.v5;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationView extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
  ProgressChangeListener, OffRouteListener, Callback<DirectionsResponse> {

  private MapView mapView;
  private InstructionView instructionView;

  private MapboxMap map;
  private MapboxNavigation navigation;
  private NavigationMapRoute mapRoute;
  private NavigationCamera camera;
  private LocationEngine locationEngine;
  private LocationLayerPlugin locationLayer;

  private Location location;
  private Position destination;
  private boolean unpackBundle;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.navigation_view_layout);
    unpackBundle = savedInstanceState == null;
    bind();

    initMap(savedInstanceState);
    initNavigation();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
    if (locationLayer != null) {
      locationLayer.onStop();
    }
  }

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
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
    if (locationLayer != null) {
      locationLayer.onStop();
    }
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    map = mapboxMap;
    initLocation();
    initLocationLayer();
    initRoute();
    initCamera();
  }

  @SuppressWarnings({"MissingPermission"})
  @Override
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    this.location = location;
    if (unpackBundle) {
      unpackBundle(getIntent());
      unpackBundle = false;
    }
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayer.forceLocationUpdate(location);
    instructionView.update(routeProgress);
  }

  @Override
  public void userOffRoute(Location location) {
    Position newOrigin = Position.fromLngLat(location.getLongitude(), location.getLatitude());
    fetchRoute(newOrigin, destination);
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      startNavigation(response.body().getRoutes().get(0));
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable t) {
  }

  private void bind() {
    mapView = findViewById(R.id.mapView);
    instructionView = findViewById(R.id.instructionView);
  }

  private void initMap(Bundle savedInstanceState) {
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  private void initNavigation() {
    navigation = new MapboxNavigation(this);
    navigation.addProgressChangeListener(this);

  }

  @SuppressWarnings({"MissingPermission"})
  private void initLocation() {
    //    locationEngine = new LocationSource(this);
    //    locationEngine.setPriority(HIGH_ACCURACY);
    //    locationEngine.setInterval(0);
    //    locationEngine.setFastestInterval(1000);
    //    locationEngine.addLocationEngineListener(this);
    //    locationEngine.activate();
    //
    //    if (locationEngine.getLastLocation() != null) {
    //      onLocationChanged(locationEngine.getLastLocation());
    //    }
    onLocationChanged(null);
  }

  private void initRoute() {
    mapRoute = new NavigationMapRoute(mapView, map, NavigationConstants.ROUTE_BELOW_LAYER);
  }

  private void initCamera() {
    camera = new NavigationCamera(this, map, navigation);
  }

  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, map, null);
  }

  private void unpackBundle(Intent intent) {
    double originLng = intent.getDoubleExtra(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LNG_KEY, 0);
    double originLat = intent.getDoubleExtra(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LAT_KEY, 0);
    double destinationLng = intent.getDoubleExtra(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LNG_KEY, 0);
    double destinationLat = intent.getDoubleExtra(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LAT_KEY, 0);
    Position origin = Position.fromLngLat(originLng, originLat);
    destination = Position.fromLngLat(destinationLng, destinationLat);
    fetchRoute(origin, destination);
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && response.body().getRoutes() != null
      && response.body().getRoutes().size() > 0;
  }

  private void fetchRoute(Position origin, Position destination) {
    NavigationRoute.Builder routeBuilder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination);

    if (locationHasBearing()) {
      fetchRouteWithBearing(routeBuilder);
    } else {
      routeBuilder.build().getRoute(this);
    }
  }

  private boolean locationHasBearing() {
    return location != null && location.hasBearing();
  }

  private void fetchRouteWithBearing(NavigationRoute.Builder routeBuilder) {
    routeBuilder.addBearing(location.getBearing(), 90);
    routeBuilder.build().getRoute(this);
  }

  @SuppressWarnings({"MissingPermission"})
  private void startNavigation(DirectionsRoute route) {
    activateMockLocationEngine(route);
    mapRoute.addRoute(route);
    camera.start(route);
    navigation.setLocationEngine(locationEngine);
    navigation.startNavigation(route);
    locationLayer.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);
    instructionView.show();
  }

  private void activateMockLocationEngine(DirectionsRoute route) {
    locationEngine = new MockLocationEngine(1000, 30, false);
    ((MockLocationEngine) locationEngine).setRoute(route);
    locationEngine.activate();
  }
}
