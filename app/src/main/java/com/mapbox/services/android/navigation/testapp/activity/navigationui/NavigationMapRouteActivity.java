package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.gson.Gson;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.commons.models.Position;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class NavigationMapRouteActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapClickListener, Callback<DirectionsResponse> {

  private static final String DIRECTIONS_RESPONSE = "directions-route.json";

  @BindView(R.id.mapView)
  MapView mapView;

  private MapboxMap mapboxMap;
  private NavigationMapRoute navigationMapRoute;
  private StyleCycle styleCycle = new StyleCycle();

  private Marker originMarker;
  private Marker destinationMarker;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_map_route);
    ButterKnife.bind(this);

    mapView.setStyleUrl(styleCycle.getStyle());
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @OnClick(R.id.fabStyles)
  public void onStyleFabClick() {
    if (mapboxMap != null) {
      mapboxMap.setStyleUrl(styleCycle.getNextStyle());
    }
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
    Gson gson = new Gson();
    DirectionsResponse response = gson.fromJson(loadJsonFromAsset(DIRECTIONS_RESPONSE), DirectionsResponse.class);
    navigationMapRoute.addRoute(response.getRoutes().get(0));
    mapboxMap.setOnMapClickListener(this);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (originMarker == null) {
      originMarker = mapboxMap.addMarker(new MarkerOptions().position(point));
    } else if (destinationMarker == null) {
      destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));
      Position originPosition = Position.fromLngLat(
        originMarker.getPosition().getLongitude(), originMarker.getPosition().getLatitude());
      Position destinationPosition = Position.fromLngLat(
        destinationMarker.getPosition().getLongitude(), destinationMarker.getPosition().getLatitude());
      requestDirectionsRoute(originPosition, destinationPosition);
    } else {
      mapboxMap.removeMarker(originMarker);
      mapboxMap.removeMarker(destinationMarker);
      originMarker = null;
      destinationMarker = null;
      navigationMapRoute.removeRoute();
    }
  }

  public void requestDirectionsRoute(Position origin, Position destination) {
    MapboxDirections directions = new MapboxDirections.Builder()
      .setOrigin(origin)
      .setDestination(destination)
      .setAccessToken(Mapbox.getAccessToken())
      .setProfile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
      .setOverview(DirectionsCriteria.OVERVIEW_FULL)
      .setAnnotation(DirectionsCriteria.ANNOTATION_CONGESTION)
      .setSteps(true)
      .build();

    directions.enqueueCall(this);
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (response.body() != null) {
      if (response.body().getRoutes().size() > 0) {
        navigationMapRoute.addRoute(response.body().getRoutes().get(0));
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    Timber.e(throwable);
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
  protected void onStop() {
    super.onStop();
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
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  public NavigationMapRoute getNavigationMapRoute() {
    return navigationMapRoute;
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

  private static class StyleCycle {
    private static final String[] STYLES = new String[] {
      Style.MAPBOX_STREETS,
      Style.OUTDOORS,
      Style.LIGHT,
      Style.DARK,
      Style.SATELLITE_STREETS
    };

    private int index;

    private String getNextStyle() {
      index++;
      if (index == STYLES.length) {
        index = 0;
      }
      return getStyle();
    }

    private String getStyle() {
      return STYLES[index];
    }
  }
}
