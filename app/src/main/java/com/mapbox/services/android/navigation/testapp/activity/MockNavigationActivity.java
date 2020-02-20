package com.mapbox.services.android.navigation.testapp.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.extensions.MapboxRouteOptionsUtils;
import com.mapbox.navigation.base.metrics.MetricEvent;
import com.mapbox.navigation.base.metrics.MetricsObserver;
import com.mapbox.navigation.core.location.ReplayRouteLocationEngine;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.metrics.MapboxMetricsReporter;
import com.mapbox.navigation.ui.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.utils.Utils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MockNavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, RouteProgressObserver, LocationObserver,
        OffRouteObserver, RoutesObserver, MetricsObserver {

  private static final int BEGIN_ROUTE_MILESTONE = 1001;
  private static final double TWENTY_FIVE_METERS = 25d;

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;

  @BindView(R.id.startRouteButton)
  Button startRouteButton;

  private MapboxMap mapboxMap;

  // Navigation related variables
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private DirectionsRoute routes;
  private NavigationMapRoute navigationMapRoute;
  private Point destination;
  private Point waypoint;
  //  private RouteRefresh routeRefresh; TODO RouteRefresh impl
  private boolean isRefreshing = false;

  private static class MyBroadcastReceiver extends BroadcastReceiver {
    private final WeakReference<MapboxNavigation> weakNavigation;

    MyBroadcastReceiver(MapboxNavigation navigation) {
      this.weakNavigation = new WeakReference<>(navigation);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      MapboxNavigation navigation = weakNavigation.get();
      navigation.stopTripSession();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mock_navigation);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    locationEngine = new ReplayRouteLocationEngine(null);
    navigation = new MapboxNavigation(
            this,
            Mapbox.getAccessToken(),
            new NavigationOptions.Builder().build(),
            locationEngine
    );
    MapboxMetricsReporter.INSTANCE.setMetricsObserver(this);

  }

  @SuppressLint("MissingPermission")
  @OnClick(R.id.startRouteButton)
  public void onStartRouteClick() {
    boolean isValidNavigation = navigation != null;
    boolean isValidRoute =
            !navigation.getRoutes().isEmpty()
                    && navigation.getRoutes().get(0).distance() > TWENTY_FIVE_METERS;
    if (isValidNavigation && isValidRoute) {

      // Hide the start button
      startRouteButton.setVisibility(View.INVISIBLE);

      // Attach all of our navigation listeners.
      navigation.registerRouteProgressObserver(this);
      navigation.registerLocationObserver(this);
      navigation.registerOffRouteObserver(this);

      ((ReplayRouteLocationEngine) locationEngine).assign(navigation.getRoutes().get(0));
      mapboxMap.getLocationComponent().setLocationComponentEnabled(true);
      navigation.startTripSession();
      mapboxMap.removeOnMapClickListener(this);
    }
  }

  @OnClick(R.id.newLocationFab)
  public void onNewLocationClick() {
    newOrigin();
  }

  private void newOrigin() {
    if (mapboxMap != null) {
      LatLng latLng = Utils.getRandomLatLng(new double[]{-77.1825, 38.7825, -76.9790, 39.0157});
      ((ReplayRouteLocationEngine) locationEngine).assignLastLocation(
              Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude())
      );
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.addOnMapClickListener(this);
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      LocationComponent locationComponent = mapboxMap.getLocationComponent();
      locationComponent.activateLocationComponent(this, style);
      locationComponent.setRenderMode(RenderMode.GPS);
      locationComponent.setLocationComponentEnabled(false);
      navigationMapRoute = new NavigationMapRoute(navigation, mapView, mapboxMap);
      Snackbar.make(findViewById(R.id.container), "Tap map to place waypoint",
              Snackbar.LENGTH_LONG).show();
      newOrigin();
    });
  }

  @Override
  public boolean onMapClick(@NonNull LatLng point) {
    if (destination == null) {
      destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    } else if (waypoint == null) {
      waypoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    } else {
      Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show();
    }
    mapboxMap.addMarker(new MarkerOptions().position(point));
    calculateRoute();
    return false;
  }

  @SuppressLint("MissingPermission")
  private void calculateRoute() {
    locationEngine.getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
      @Override
      public void onSuccess(LocationEngineResult result) {
        findRouteWith(result);
      }

      @Override
      public void onFailure(@NonNull Exception exception) {
        Timber.e(exception);
      }
    });
  }

  private void findRouteWith(LocationEngineResult result) {
    Location userLocation = result.getLastLocation();
    if (userLocation == null) {
      Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
      return;
    }
    Point origin = Point.fromLngLat(userLocation.getLongitude(), userLocation.getLatitude());
    if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
      startRouteButton.setVisibility(View.GONE);
      return;
    }

    List<Point> coordinates = new ArrayList<Point>();
    coordinates.add(origin);
    if (waypoint != null) {
      coordinates.add(waypoint);
    }
    coordinates.add(destination);

    final RouteOptions routeOptions = MapboxRouteOptionsUtils.applyDefaultParams(RouteOptions.builder())
            .accessToken(Mapbox.getAccessToken())
            .coordinates(coordinates)
            .build();
    navigation.requestRoutes(routeOptions);
  }

  /*
   * Navigation listeners
   */

  @Override
  public void onOffRouteStateChanged(boolean offRoute) {
    Toast.makeText(this, "off-route called", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onRawLocationChanged(@NotNull Location rawLocation) {
  }

  @Override
  public void onEnhancedLocationChanged(
          @NotNull Location enhancedLocation,
          @NotNull List<? extends Location> keyPoints
  ) {
    mapboxMap.getLocationComponent().forceLocationUpdate(enhancedLocation);
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
//     TODO impl
//    if (!isRefreshing) {
//      isRefreshing = true;
//      routeRefresh.refresh(routeProgress, this);
//    }
    Timber.d("onProgressChange: fraction of route traveled: %s", routeProgress.currentLegProgress().fractionTraveled());
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
  }

  @Override
  protected void onStop() {
    super.onStop();
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
    MapboxMetricsReporter.INSTANCE.removeObserver();
    navigation.onDestroy();
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
    navigation.setRoutes(routes);
    navigation.startTripSession();
    isRefreshing = false;
    navigationMapRoute.addRoutes(routes);
    startRouteButton.setVisibility(View.VISIBLE);
  }

  @Override
  public void onMetricUpdated(@NotNull @MetricEvent.Metric String metricName, @NotNull String jsonStringData) {
    Timber.d("METRICS_LOG: %s", metricName);
    Timber.d("METRICS_LOG: %s", jsonStringData);
  }
}
