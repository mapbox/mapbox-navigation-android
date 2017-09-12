package com.mapbox.services.android.navigation.testapp.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.gson.Gson;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LongStepTestActivity extends AppCompatActivity implements ProgressChangeListener,
  OnMapReadyCallback {

  @BindView(R.id.mapView)
  MapView mapView;

  private LocationLayerPlugin locationLayerPlugin;
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private DirectionsRoute route;
  private MapboxMap mapboxMap;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_long_step_test);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    Gson gson = new Gson();
    DirectionsResponse response = gson.fromJson(loadJsonFromAsset("long_step.json"), DirectionsResponse.class);
    route = response.getRoutes().get(0);

    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
      .defaultMilestonesEnabled(false)
      .snapToRoute(false)
      .build();

    navigation = new MapboxNavigation(this, Mapbox.getAccessToken(), options);
    navigation.addProgressChangeListener(this);
    locationEngine = new MockLocationEngine();
    locationEngine.activate();
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(38.00287907761617, -121.8483493917405), 10));
    mapboxMap.addPolyline(new PolylineOptions()
      .add(new LatLng(37.42067211454105, -122.26105120655842))
      .add(new LatLng(38.00287907761617, -121.8483493917405)).color(Color.BLUE).width(5f));
    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, null);
    locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayerPlugin.forceLocationUpdate(location);
  }

  @OnClick(R.id.startNavigationFab)
  public void onFabClick(View view) {
    navigation.addProgressChangeListener(this);
    navigation.setLocationEngine(locationEngine);
    ((MockLocationEngine) locationEngine).setRoute(route);
    navigation.startNavigation(route);
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
    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStop();
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    locationEngine.removeLocationUpdates();
    locationEngine.deactivate();
    navigation.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  private String loadJsonFromAsset(String filename) {
    // Using this method to load in GeoJSON files from the assets folder.

    try {
      InputStream is = getAssets().open(filename);
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      return new String(buffer, "UTF-8");

    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }
}
