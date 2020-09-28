package com.mapbox.navigation.carbon.examples;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.gesture.GesturePluginImpl;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationComponentPlugin;
import com.mapbox.maps.plugin.location.modes.RenderMode;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import timber.log.Timber;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.mapbox.navigation.carbon.examples.LocationPermissionHelperKt.LOCATION_PERMISSIONS_REQUEST_CODE;

public class CameraAnimationsActivity extends AppCompatActivity implements PermissionsListener, AnimationAdapter.OnAnimationButtonClicked {

  private LocationPermissionHelper permissionsHelper = new LocationPermissionHelper(this);
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPluginImpl mapCamera;
  private MapboxNavigation mapboxNavigation;
  private RecyclerView recyclerView;

  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout_camera_animations);
    mapView = findViewById(R.id.mapView);
    recyclerView = findViewById(R.id.animationsList);
    mapboxMap = mapView.getMapboxMap();
    locationComponent = getLocationComponent();
    mapCamera = getMapCamera();

    if (LocationPermissionHelper.Companion.areLocationPermissionsGranted(this)) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      permissionsHelper.requestLocationPermissions(this);
    }
  }

  private void init() {
    initAnimations();
    initNavigation();
    initStyle();
  }

  @SuppressLint("MissingPermission")
  private void initNavigation() {
    NavigationOptions navigationOptions = MapboxNavigation
        .defaultNavigationOptionsBuilder(this, getMapboxAccessTokenFromResources())
        .locationEngine(LocationEngineProvider.getBestLocationEngine(this))
        .build();
    mapboxNavigation = new MapboxNavigation(navigationOptions);
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
      initializeLocationComponent(style);
      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);
    }, (mapLoadError, s) -> Timber.e("Error loading map: %s", mapLoadError.name()));
  }

  private void initAnimations() {
    AnimationAdapter adapter = new AnimationAdapter(this, this);
    LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(manager);
    recyclerView.setAdapter(adapter);
  }

  @Override public void onButtonClicked(AnimationType animationType) {
    Toast.makeText(this, animationType.name(), Toast.LENGTH_SHORT).show();
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

  private LocationComponentPlugin getLocationComponent() {
    return mapView.getPlugin(LocationComponentPlugin.class);
  }

  private CameraAnimationsPluginImpl getMapCamera() {
    return CameraAnimationsPluginImplKt.getCameraAnimationsPlugin(mapView);
  }

  private GesturePluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturePluginImpl.class);
  }

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<CameraAnimationsActivity> activityRef;

    MyLocationEngineCallback(CameraAnimationsActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      Location location = result.getLastLocation();
      CameraAnimationsActivity activity = activityRef.get();
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


