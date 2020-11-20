package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.*;
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationComponentPlugin;
import com.mapbox.maps.plugin.location.OnIndicatorPositionChangedListener;
import com.mapbox.maps.plugin.location.modes.RenderMode;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.examples.util.LocationPermissionsHelper;
import com.mapbox.navigation.examples.util.ThemeUtil;
import com.mapbox.navigation.ui.base.api.guidanceimage.GuidanceImageApi;
import com.mapbox.navigation.ui.base.domain.BannerInstructionsApi;
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState;
import com.mapbox.navigation.ui.maps.guidance.api.MapboxGuidanceImageApi;
import com.mapbox.navigation.ui.maps.guidance.api.OnGuidanceImageReady;
import com.mapbox.navigation.ui.maps.guidance.model.GuidanceImageOptions;
import com.mapbox.navigation.ui.maps.guidance.view.MapboxGuidanceView;
import com.mapbox.navigation.ui.maps.route.RouteArrowLayerInitializer;
import com.mapbox.navigation.ui.maps.route.RouteLineLayerInitializer;
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowAPI;
import com.mapbox.navigation.ui.maps.route.routearrow.internal.MapboxRouteArrowAPI;
import com.mapbox.navigation.ui.maps.route.routearrow.internal.MapboxRouteArrowActions;
import com.mapbox.navigation.ui.maps.route.routearrow.internal.MapboxRouteArrowView;
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineAPI;
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineResourceProvider;
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineAPI;
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineActions;
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineView;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.mapbox.navigation.examples.util.LocationPermissionsHelperKt.LOCATION_PERMISSIONS_REQUEST_CODE;
import static com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineResourceProviderFactory.getRouteLineResourceProvider;

public class MapboxGuidanceImageActivity extends AppCompatActivity implements PermissionsListener {

  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private LocationPermissionsHelper permissionsHelper = new LocationPermissionsHelper(this);
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPlugin mapCamera;
  private MapboxNavigation mapboxNavigation;
  private Button startNavigation;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private GuidanceImageApi guidanceImageApi;
  private MapboxGuidanceView guidanceView;

  private OnGuidanceImageReady callback = new OnGuidanceImageReady() {
    @Override public void onGuidanceImagePrepared(@NotNull GuidanceImageState.GuidanceImagePrepared bitmap) {
      Log.d("kjkjkj", "result: "+bitmap);
      guidanceView.render(bitmap);
    }

    @Override public void onFailure(@NotNull GuidanceImageState.GuidanceImageFailure error) {
      Log.d("kjkjkj", "error: "+error);
    }
  };

  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout_activity_guidance_image);
    mapView = findViewById(R.id.mapView);
    startNavigation = findViewById(R.id.startNavigation);
    guidanceView = findViewById(R.id.guidanceView);
    mapboxMap = mapView.getMapboxMap();
    locationComponent = getLocationComponent();
    mapCamera = getMapCamera();

    if (LocationPermissionsHelper.areLocationPermissionsGranted(this)) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      permissionsHelper.requestLocationPermissions(this);
    }
  }

  private void init() {
    initNavigation();
    initStyle();
    initListeners();
  }

  @SuppressLint("MissingPermission") private void initListeners() {
    startNavigation.setOnClickListener(v -> {
      locationComponent.setRenderMode(RenderMode.GPS);
      mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver);
      mapboxNavigation.startTripSession();
      startNavigation.setVisibility(View.GONE);
    });
  }

  @SuppressLint("MissingPermission")
  private void initNavigation() {
    NavigationOptions navigationOptions = MapboxNavigation
        .defaultNavigationOptionsBuilder(MapboxGuidanceImageActivity.this, getMapboxAccessTokenFromResources())
        .locationEngine(new ReplayLocationEngine(mapboxReplayer))
        .build();
    mapboxNavigation = new MapboxNavigation(navigationOptions);
    mapboxNavigation.registerLocationObserver(locationObserver);
    mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
    GuidanceImageOptions options = new GuidanceImageOptions.Builder().build();
    guidanceImageApi = new MapboxGuidanceImageApi(this, options, callback);

    mapboxReplayer.pushRealLocation(this, 0.0);
    mapboxReplayer.play();
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS,
        this::initializeLocationComponent,
        (mapLoadError, s) -> Timber.e("Error loading map: %s", mapLoadError.name()));
    mapboxNavigation.setRoutes(Collections.singletonList(getDirectionsRoute()));
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mapboxNavigation != null) {
      mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
    }
    mapView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    mapboxNavigation.onDestroy();
  }

  @Override public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
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

  @SuppressWarnings("MissingPermission")
  private void initializeLocationComponent(Style style) {
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
        .useDefaultLocationEngine(false)
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
        null,
        null
    );
  }

  private LocationComponentPlugin getLocationComponent() {
    return mapView.getPlugin(LocationComponentPlugin.class);
  }

  private CameraAnimationsPlugin getMapCamera() {
    return CameraAnimationsPluginImplKt.getCameraAnimationsPlugin(mapView);
  }

  private GesturesPluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturesPluginImpl.class);
  }

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);

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
    Toast.makeText(this, "This app needs location and storage permissions in order to show its functionality.",
        Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      Toast.makeText(this, "You didn't grant location permissions.", Toast.LENGTH_LONG).show();
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

  private BannerInstructionsObserver bannerInstructionsObserver = new BannerInstructionsObserver() {
    @Override public void onNewBannerInstructions(@NotNull BannerInstructions bannerInstructions) {
      Log.d("kjkjkj", "call API");
      guidanceImageApi.generateGuidanceImage(bannerInstructions, Point.fromLngLat(-121.966668, 37.555581));
    }
  };

  private RouteProgressObserver routeProgressObserver = new RouteProgressObserver() {
    @Override public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {

    }
  };

  private MapboxGuidanceImageActivity.MyLocationEngineCallback
      locationEngineCallback = new MapboxGuidanceImageActivity.MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<MapboxGuidanceImageActivity> activityRef;

    MyLocationEngineCallback(MapboxGuidanceImageActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      Location location = result.getLastLocation();
      MapboxGuidanceImageActivity activity = activityRef.get();
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

  private DirectionsRoute getDirectionsRoute() {
    InputStream is = getResources().openRawResource(R.raw.route_guidance_1);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    byte buf[] = new byte[1024];
    int len;
    try {
      while ((len = is.read(buf)) != -1) {
        outputStream.write(buf, 0, len);
      }
      outputStream.close();
      is.close();
    } catch (IOException e) {

    }
    return DirectionsRoute.fromJson(outputStream.toString());
  }
}
