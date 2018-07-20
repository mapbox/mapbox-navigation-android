package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.android.core.location.LocationEnginePriority.HIGH_ACCURACY;

public class DualNavigationMapActivity extends AppCompatActivity implements OnNavigationReadyCallback,
  NavigationListener, Callback<DirectionsResponse>, OnMapReadyCallback, MapboxMap.OnMapLongClickListener,
  LocationEngineListener, OnRouteSelectionChangeListener {

  private static final int CAMERA_ANIMATION_DURATION = 1000;
  private static final int DEFAULT_CAMERA_ZOOM = 16;
  private ConstraintLayout dualNavigationMap;
  private NavigationView navigationView;
  private MapView mapView;
  private ProgressBar loading;
  private FloatingActionButton launchNavigationFab;
  private Point origin = Point.fromLngLat(-122.423579, 37.761689);
  private Point destination = Point.fromLngLat(-122.426183, 37.760872);
  private DirectionsRoute route;
  private boolean isNavigationRunning;
  private LocationLayerPlugin locationLayer;
  private LocationEngine locationEngine;
  private NavigationMapRoute mapRoute;
  private MapboxMap mapboxMap;
  private Marker currentMarker;
  private boolean locationFound;
  private ConstraintSet navigationMapConstraint;
  private ConstraintSet navigationMapExpandedConstraint;
  private boolean[] constraintChanged;


  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    initializeViews(savedInstanceState);
    navigationView.initialize(this);
    navigationMapConstraint = new ConstraintSet();
    navigationMapConstraint.clone(dualNavigationMap);
    navigationMapExpandedConstraint = new ConstraintSet();
    navigationMapExpandedConstraint.clone(this, R.layout.activity_dual_navigation_map_expanded);

    constraintChanged = new boolean[] {false};
    launchNavigationFab.setOnClickListener(v -> {
      expandCollapse();
      launchNavigation();
    });
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    isNavigationRunning = isRunning;
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.setOnMapLongClickListener(this);
    initLocationEngine();
    initLocationLayer();
    initMapRoute();
    fetchRoute();
  }

  @Override
  public void onMapLongClick(@NonNull LatLng point) {
    destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    updateLoadingTo(true);
    setCurrentMarkerPosition(point);
    if (origin != null) {
      fetchRoute();
    }
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      updateLoadingTo(false);
      launchNavigationFab.show();
      route = response.body().routes().get(0);
      mapRoute.addRoutes(response.body().routes());
      if (isNavigationRunning) {
        launchNavigation();
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
  }

  @Override
  public void onCancelNavigation() {
    navigationView.stopNavigation();
    expandCollapse();
  }

  @Override
  public void onNavigationFinished() {
  }

  @Override
  public void onNavigationRunning() {
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    onLocationFound(location);
  }

  @Override
  public void onNewPrimaryRouteSelected(DirectionsRoute directionsRoute) {
    route = directionsRoute;
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
    mapView.onStart();
    if (locationLayer != null) {
      locationLayer.onStart();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
    mapView.onResume();
    if (locationEngine != null) {
      locationEngine.addLocationEngineListener(this);
      if (!locationEngine.isConnected()) {
        locationEngine.activate();
      }
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public void onBackPressed() {
    if (!navigationView.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
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
    mapView.onPause();
    if (locationEngine != null) {
      locationEngine.removeLocationEngineListener(this);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    navigationView.onStop();
    mapView.onStop();
    if (locationLayer != null) {
      locationLayer.onStop();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
    mapView.onDestroy();
    if (locationEngine != null) {
      locationEngine.removeLocationUpdates();
      locationEngine.deactivate();
    }
  }

  private void expandCollapse() {
    TransitionManager.beginDelayedTransition(dualNavigationMap);
    ConstraintSet constraint;
    if (constraintChanged[0]) {
      constraint = navigationMapConstraint;
    } else {
      constraint = navigationMapExpandedConstraint;
    }
    constraint.applyTo(dualNavigationMap);
    constraintChanged[0] = !constraintChanged[0];
  }

  private void fetchRoute() {
    NavigationRoute builder = NavigationRoute.builder(this)
      .accessToken(getString(R.string.mapbox_access_token))
      .origin(origin)
      .destination(destination)
      .alternatives(true)
      .build();
    builder.getRoute(this);
  }

  private void launchNavigation() {
    launchNavigationFab.hide();
    navigationView.setVisibility(View.VISIBLE);
    NavigationViewOptions.Builder options = NavigationViewOptions.builder()
      .navigationListener(this)
      .directionsRoute(route);
    navigationView.startNavigation(options.build());
  }

  private void initializeViews(@Nullable Bundle savedInstanceState) {
    setContentView(R.layout.activity_dual_navigation_map);
    dualNavigationMap = findViewById(R.id.dualNavigationMap);
    mapView = findViewById(R.id.mapView);
    navigationView = findViewById(R.id.navigationView);
    loading = findViewById(R.id.loading);
    launchNavigationFab = findViewById(R.id.launchNavigation);
    navigationView.onCreate(savedInstanceState);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  private void updateLoadingTo(boolean isVisible) {
    if (isVisible) {
      loading.setVisibility(View.VISIBLE);
    } else {
      loading.setVisibility(View.INVISIBLE);
    }
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null && !response.body().routes().isEmpty();
  }


  @SuppressWarnings( {"MissingPermission"})
  private void initLocationEngine() {
    locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
    locationEngine.setPriority(HIGH_ACCURACY);
    locationEngine.setInterval(0);
    locationEngine.setFastestInterval(1000);
    locationEngine.addLocationEngineListener(this);
    locationEngine.activate();

    if (locationEngine.getLastLocation() != null) {
      Location lastLocation = locationEngine.getLastLocation();
      onLocationChanged(lastLocation);
      origin = Point.fromLngLat(lastLocation.getLongitude(), lastLocation.getLatitude());
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  private void initLocationLayer() {
    locationLayer = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
    locationLayer.setRenderMode(RenderMode.COMPASS);
  }

  private void initMapRoute() {
    mapRoute = new NavigationMapRoute(mapView, mapboxMap);
    mapRoute.setOnRouteSelectionChangeListener(this);
  }

  private void setCurrentMarkerPosition(LatLng position) {
    if (position != null) {
      if (currentMarker == null) {
        MarkerViewOptions markerViewOptions = new MarkerViewOptions()
          .position(position);
        currentMarker = mapboxMap.addMarker(markerViewOptions);
      } else {
        currentMarker.setPosition(position);
      }
    }
  }

  private void onLocationFound(Location location) {
    if (!locationFound) {
      animateCamera(new LatLng(location.getLatitude(), location.getLongitude()));
      locationFound = true;
      updateLoadingTo(false);
    }
  }

  private void animateCamera(LatLng point) {
    mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, DEFAULT_CAMERA_ZOOM), CAMERA_ANIMATION_DURATION);
  }
}