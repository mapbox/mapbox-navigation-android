package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts;
import com.mapbox.navigation.examples.R;
import com.mapbox.navigation.examples.utils.Utils;
import com.mapbox.navigation.logger.MapboxLogger;
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static com.mapbox.navigation.base.extensions.MapboxRouteOptionsUtils.applyDefaultParams;
import static com.mapbox.navigation.base.extensions.MapboxRouteOptionsUtils.coordinates;

public class OffboardRouterActivityJava extends AppCompatActivity implements
  OnMapReadyCallback, MapboxMap.OnMapClickListener, Router.Callback {

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;

  private MapboxMap mapboxMap;

  private Router offboardRouter;
  private DirectionsRoute route;
  private NavigationMapRoute navigationMapRoute;
  private Point origin;
  private Point destination;
  private Point waypoint;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mock_navigation);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @OnClick(R.id.newLocationFab)
  public void onNewLocationClick() {
    newOrigin();
  }

  private void newOrigin() {
    if (mapboxMap != null) {
      clearMap();
      LatLng latLng = Utils.getRandomLatLng(new double[]{-77.1825, 38.7825, -76.9790, 39.0157});
      origin = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.addOnMapClickListener(this);
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      navigationMapRoute = new NavigationMapRoute(mapView, mapboxMap);
      Snackbar.make(findViewById(R.id.container), "Tap map to place waypoint", Snackbar.LENGTH_LONG).show();
      newOrigin();
    });
  }

  @Override
  public boolean onMapClick(@NonNull LatLng point) {
    if (destination == null) {
      destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
      mapboxMap.addMarker(new MarkerOptions().position(point));
      findRoute();
    } else if (waypoint == null) {
      waypoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
      mapboxMap.addMarker(new MarkerOptions().position(point));
      findRoute();
    } else {
      Toast.makeText(this, "Only 2 waypoints supported for this example", Toast.LENGTH_LONG).show();
      clearMap();
    }
    return false;
  }

  private void clearMap() {
    if (mapboxMap != null) {
      mapboxMap.clear();
      route = null;
      destination = null;
      waypoint = null;
      navigationMapRoute.updateRouteVisibilityTo(false);
      navigationMapRoute.updateRouteArrowVisibilityTo(false);
    }
  }

  private void findRoute() {
    if (origin != null && destination != null) {
      if (offboardRouter == null) {
        offboardRouter = new MapboxOffboardRouter(
          Utils.getMapboxAccessToken(this),
          this,
          MapboxNavigationAccounts.getInstance(this));
      } else {
        offboardRouter.cancel();
      }

      if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) > 50) {
        List<Point> waypoints = new ArrayList<>();
        if (waypoint != null) {
          waypoints.add(waypoint);
        }
        RouteOptions.Builder optionsBuilder =
          applyDefaultParams(RouteOptions.builder())
          .accessToken(Utils.getMapboxAccessToken(this))
          .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
          .annotations(
            DirectionsCriteria.ANNOTATION_CONGESTION + ","
            + DirectionsCriteria.ANNOTATION_DISTANCE
            + "," + DirectionsCriteria.ANNOTATION_DURATION
          );

        coordinates(optionsBuilder, origin, waypoints, destination);

        offboardRouter.getRoute(optionsBuilder.build(), this);
      }
    }
  }

  /*
   * Router.Callback
   */

  @Override
  public void onResponse(@NotNull List<? extends DirectionsRoute> routes) {
    if (!routes.isEmpty()) {
      navigationMapRoute.addRoute(routes.get(0));
    }
  }

  @Override
  public void onFailure(@NotNull Throwable throwable) {
    Toast.makeText(this, "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
    MapboxLogger.INSTANCE.e(new Message("Router.Callback#onFailure"), throwable);
  }

  @Override
  public void onCanceled() {
    Timber.e("onRoutesRequestCanceled");
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
    if (offboardRouter != null) {
      offboardRouter.cancel();
    }
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
}
