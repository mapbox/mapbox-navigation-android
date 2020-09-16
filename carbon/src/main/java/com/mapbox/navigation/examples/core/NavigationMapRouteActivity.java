package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.gesture.GesturePluginImpl;
import com.mapbox.maps.plugin.gesture.OnMapLongClickListener;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationComponentPlugin;
import com.mapbox.maps.plugin.location.modes.RenderMode;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.examples.LocationPermissionsHelper;
import com.mapbox.navigation.ui.route.NavigationMapRoute;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.mapbox.navigation.examples.LocationPermissionsHelperKt.LOCATION_PERMISSIONS_REQUEST_CODE;

public class NavigationMapRouteActivity extends AppCompatActivity implements PermissionsListener,
  OnMapLongClickListener {

  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private LocationPermissionsHelper permissionsHelper = new LocationPermissionsHelper(this);
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPluginImpl mapCamera;
  private NavigationMapRoute navigationMapRoute;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private MapboxNavigation mapboxNavigation;
  private Button startNavigation;
  private ProgressBar routeLoading;

  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_map_route);
    mapView = findViewById(R.id.mapView);
    startNavigation = findViewById(R.id.startNavigation);
    routeLoading = findViewById(R.id.routeLoadingProgressBar);
    mapboxMap = mapView.getMapboxMap();
    locationComponent = getLocationComponent();
    mapCamera = getMapCamera();

    startNavigation.setOnClickListener(v -> {
      locationComponent.setRenderMode(RenderMode.GPS);
      mapboxNavigation.startTripSession();
      startNavigation.setVisibility(View.GONE);
    });

    if (LocationPermissionsHelper.areLocationPermissionsGranted(this)) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      permissionsHelper.requestLocationPermissions(this);
    }
  }

  private void init() {
    initNavigation();
    initStyle();
  }

  @SuppressLint("MissingPermission")
  private void initNavigation() {
    NavigationOptions navigationOptions = MapboxNavigation
      .defaultNavigationOptionsBuilder(NavigationMapRouteActivity.this, getMapboxAccessTokenFromResources())
      .locationEngine(new ReplayLocationEngine(mapboxReplayer))
      .build();
    mapboxNavigation = new MapboxNavigation(navigationOptions);
    mapboxNavigation.registerLocationObserver(locationObserver);
    mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
    mapboxNavigation.registerRoutesObserver(routesObserver);

    mapboxReplayer.pushRealLocation(this, 0.0);
    mapboxReplayer.play();
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
      initializeLocationComponent(style);
      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);

      navigationMapRoute = new NavigationMapRoute.Builder(mapView, mapboxMap, this)
        .withVanishRouteLineEnabled(true)
        .withMapboxNavigation(mapboxNavigation)
        .build();

      getGesturePlugin().addOnMapLongClickListener(this);

    }, (mapLoadError, s) -> Timber.e("Error loading map: %s", mapLoadError.name()));
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
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    mapboxNavigation.onDestroy();
  }

  @Override
  public boolean onMapLongClick(@NotNull Point point) {
    vibrate();
    hideRoute();

    Location currentLocation = getLocationComponent().getLastKnownLocation();
    if (currentLocation != null) {
      Point originPoint = Point.fromLngLat(
        currentLocation.getLongitude(),
        currentLocation.getLatitude()
      );
      findRoute(originPoint, point);
      routeLoading.setVisibility(View.VISIBLE);
    }
    return false;
  }

  public void findRoute(Point origin, Point destination) {
    RouteOptions routeOptions = RouteOptions.builder()
      .baseUrl(RouteUrl.BASE_URL)
      .user(RouteUrl.PROFILE_DEFAULT_USER)
      .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
      .geometries(RouteUrl.GEOMETRY_POLYLINE6)
      .requestUuid("")
      .accessToken(getMapboxAccessTokenFromResources())
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
        routeLoading.setVisibility(View.INVISIBLE);
        startNavigation.setVisibility(View.VISIBLE);
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

  private RoutesObserver routesObserver = new RoutesObserver() {
    @Override
    public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
      navigationMapRoute.addRoutes(routes);
    }
  };

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
    startNavigation.setVisibility(View.GONE);
  }

  @SuppressWarnings("MissingPermission")
  private void initializeLocationComponent(Style style) {
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
      .useDefaultLocationEngine(false) //SBNOTE: I think this should be false eventually
      .build();
    locationComponent.activateLocationComponent(activationOptions);
    locationComponent.setEnabled(true);
    locationComponent.setRenderMode(RenderMode.COMPASS);
  }

  private String getMapboxAccessTokenFromResources() {
    return getString(this.getResources().getIdentifier("mapbox_access_token", "string", getPackageName()));
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

  private void updateLocation(Location location) {
    updateLocation(Arrays.asList(location));
  }

  private void updateLocation(List<Location> locations) {
    Location location = locations.get(0);
    getLocationComponent().forceLocationUpdate(locations, false);

    mapCamera.easeTo(
      new CameraOptions.Builder()
        .center(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
        .bearing((double) location.getBearing())
        .pitch(45.0)
        .zoom(17.0)
        .padding(new EdgeInsets(1000, 0, 0, 0))
        .build(),
      1500L,
      null
    );
  }

  private LocationComponentPlugin getLocationComponent() {
    return mapView.getPlugin(LocationComponentPlugin.class);
  }

  private CameraAnimationsPluginImpl getMapCamera() {
    return CameraAnimationsPluginImplKt.getCameraAnimationsPlugin(mapView);
  }

  private GesturePluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturePluginImpl.class);
  }

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<NavigationMapRouteActivity> activityRef;

    MyLocationEngineCallback(NavigationMapRouteActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      Location location = result.getLastLocation();
      NavigationMapRouteActivity activity = activityRef.get();
      if (location != null && activity != null) {
        Point point = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        CameraOptions cameraOptions = new CameraOptions.Builder().center(point).zoom(13.0).build();
        activity.mapboxMap.jumpTo(cameraOptions);
        activity.locationComponent.forceLocationUpdate(location);
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
      Timber.i(exception);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
      permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    } else if (grantResults.length > 0) {
      init();
    } else {
      Toast.makeText(this, "You didn't grant storage and/or location permissions.", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, "This app needs location and storage permissions in order to show its functionality.", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      Toast.makeText(this, "Uou didn't grant location permissions.", Toast.LENGTH_LONG).show();
    }
  }

  private void requestPermissionIfNotGranted(String permission) {
    List<String> permissionsNeeded = new ArrayList<>();
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      permissionsNeeded.add(permission);
      ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 10);
    } else {
      init();
    }
  }
}
