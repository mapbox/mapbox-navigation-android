package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.examples.R;
import com.mapbox.navigation.examples.utils.Utils;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.route.NavigationMapRoute;


import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static com.mapbox.navigation.examples.utils.Utils.PRIMARY_ROUTE_BUNDLE_KEY;
import static com.mapbox.navigation.examples.utils.Utils.getRouteFromBundle;

/**
 * This activity demonstrates turn by turn navigation using the NavigationMapRoute class. This can
 * be used instead of the convenience class NavigationMapboxMap if it suits your needs.
 */
public class NavigationMapRouteActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapLongClickListener {
  private static final int ONE_HUNDRED_MILLISECONDS = 100;

  @BindView(R.id.mapView)
  MapView mapView;
  @BindView(R.id.routeLoadingProgressBar)
  ProgressBar routeLoading;
  @BindView(R.id.startNavigation)
  Button startNavigationButton;

  private MapboxMap mapboxMap;
  private NavigationMapRoute navigationMapRoute;
  private MapboxNavigation mapboxNavigation;
  private NavigationCamera mapCamera;
  private DirectionsRoute activeRoute;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_map_route);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @SuppressLint("MissingPermission")
  @OnClick(R.id.startNavigation)
  public void onStartNavigation() {
    mapboxMap.getLocationComponent().setCameraMode(CameraMode.TRACKING_GPS);
    mapboxMap.getLocationComponent().setRenderMode(RenderMode.GPS);
    mapCamera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    mapCamera.start(activeRoute);
    mapboxNavigation.startActiveGuidance();
    startNavigationButton.setVisibility(View.GONE);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      initializeLocationComponent(mapboxMap, style);
      NavigationOptions navigationOptions = MapboxNavigation
              .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
              .locationEngine(new ReplayLocationEngine())
              .build();
      mapboxNavigation = new MapboxNavigation(navigationOptions);
      mapboxNavigation.registerLocationObserver(locationObserver);
      mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
      mapboxReplayer.pushRealLocation(this, 0.0);
      mapboxReplayer.play();

      mapCamera = new NavigationCamera(mapboxMap, mapboxNavigation, mapboxMap.getLocationComponent());
      mapCamera.addProgressChangeListener(mapboxNavigation);
      navigationMapRoute = new NavigationMapRoute.Builder(mapView, mapboxMap, this)
              .withMapboxNavigation(mapboxNavigation, true)
              .build();

      mapboxMap.addOnMapLongClickListener(this);

      if (activeRoute != null) {
        final List<DirectionsRoute> routes = Arrays.asList(activeRoute);
        navigationMapRoute.addRoutes(routes);
        mapboxNavigation.setRoutes(routes);
        startNavigationButton.setVisibility(View.VISIBLE);
      } else {
        Snackbar.make(mapView, R.string.msg_long_press_map_to_place_waypoint, Snackbar.LENGTH_SHORT).show();
      }
    });
  }


  @Override
  public boolean onMapLongClick(@NonNull LatLng point) {
    handleClicked(point);
    return true;
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
    if (mapboxNavigation != null) {
      mapboxNavigation.registerLocationObserver(locationObserver);
    }
    if (mapCamera != null) {
      mapCamera.onStart();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapCamera.onStop();
    mapboxNavigation.unregisterLocationObserver(locationObserver);
    mapView.onStop();
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
    mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver);
    mapboxNavigation.stopActiveGuidance();
    mapboxNavigation.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);

    // This is not the most efficient way to preserve the route on a device rotation.
    // This is here to demonstrate that this event needs to be handled in order to
    // redraw the route line after a rotation.
    if (activeRoute != null) {
      outState.putString(PRIMARY_ROUTE_BUNDLE_KEY, activeRoute.toJson());
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    activeRoute = getRouteFromBundle(savedInstanceState);
  }

  @SuppressWarnings("MissingPermission")
  private void initializeLocationComponent(MapboxMap mapboxMap, Style style) {
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(false)
            .build();
    LocationComponent locationComponent = mapboxMap.getLocationComponent();
    locationComponent.activateLocationComponent(activationOptions);
    locationComponent.setLocationComponentEnabled(true);
    locationComponent.setRenderMode(RenderMode.COMPASS);
    locationComponent.setCameraMode(CameraMode.TRACKING);
  }

  private void handleClicked(@NonNull LatLng touchLocation) {
    vibrate();
    hideRoute();
    Location currentLocation = mapboxMap.getLocationComponent().getLastKnownLocation();
    if (currentLocation != null) {
      Point originPoint = Point.fromLngLat(
              currentLocation.getLongitude(),
              currentLocation.getLatitude()
      );
      Point destinationPoint = Point.fromLngLat(touchLocation.getLongitude(), touchLocation.getLatitude());
      findRoute(originPoint, destinationPoint);
      routeLoading.setVisibility(View.VISIBLE);
    }
  }

  @SuppressLint("MissingPermission")
  private void vibrate() {
    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    if (vibrator == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createOneShot(ONE_HUNDRED_MILLISECONDS, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
      vibrator.vibrate(ONE_HUNDRED_MILLISECONDS);
    }
  }

  private void hideRoute() {
    navigationMapRoute.updateRouteVisibilityTo(false);
    startNavigationButton.setVisibility(View.GONE);
  }

  public void findRoute(Point origin, Point destination) {
    RouteOptions routeOptions = RouteOptions.builder()
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
            .geometries(RouteUrl.GEOMETRY_POLYLINE6)
            .requestUuid("")
            .accessToken(Utils.getMapboxAccessToken(this.getApplicationContext()))
            .coordinates(Arrays.asList(origin, destination))
            .alternatives(false)
            .build();

    mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
    );
  }

  private RoutesRequestCallback routesReqCallback = new RoutesRequestCallback() {
      @Override
      public void onRoutesReady(@NotNull List<? extends DirectionsRoute> routes) {
        if (!routes.isEmpty()) {
          activeRoute = routes.get(0);
          navigationMapRoute.addRoutes(routes);
          routeLoading.setVisibility(View.INVISIBLE);
          startNavigationButton.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onRoutesRequestFailure(@NotNull Throwable throwable, @NotNull RouteOptions routeOptions) {
        Timber.e("route request failure %s", throwable.toString());
      }

      @Override
      public void onRoutesRequestCanceled(@NotNull RouteOptions routeOptions) {
        Timber.d("route request canceled");
      }
  };

  private void updateLocation(Location location) {
    updateLocation(Arrays.asList(location));
  }

  private void updateLocation(List<Location> locations) {
    mapboxMap.getLocationComponent().forceLocationUpdate(locations, false);
  }

  private LocationObserver locationObserver = new LocationObserver() {
    @Override
    public void onRawLocationChanged(@NotNull Location rawLocation) {
      Timber.d("raw location %s", rawLocation.toString());
    }

    @Override
    public void onEnhancedLocationChanged(
            @NotNull Location enhancedLocation,
            @NotNull List<? extends Location> keyPoints
    ) {
        if (keyPoints.isEmpty()) {
          updateLocation(enhancedLocation);
        } else {
          updateLocation((List<Location>) keyPoints);
        }
    }
  };

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);
}
