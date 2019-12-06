package com.mapbox.services.android.navigation.testapp.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.route.DirectionsSession;
import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation;
import com.mapbox.navigation.directions.session.MapboxDirectionsSession;
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter;
import com.mapbox.navigation.route.offboard.router.NavigationRoute;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.utils.Utils;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.utils.extensions.Mappers;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class OffboardRouterActivityJava extends AppCompatActivity implements
        OnMapReadyCallback,
        MapboxMap.OnMapClickListener,
        DirectionsSession.RouteObserver {

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;

  private MapboxMap mapboxMap;

  private DirectionsRoute route;
  private NavigationMapRoute navigationMapRoute;
  private DirectionsSession directionsSession;
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
    Router offboardRouter = new MapboxOffboardRouter(
      NavigationRoute.builder(this)
    );
    directionsSession = new MapboxDirectionsSession(
      offboardRouter,
      this
    );
    if (origin != null && destination != null) {
      if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) > 50) {
        List<Point> waypoints = new ArrayList<>();
        if (waypoint != null) {
          waypoints.add(waypoint);
        }
        RouteOptionsNavigation.Builder optionsBuilder = new RouteOptionsNavigation.Builder();
        optionsBuilder.accessToken(Utils.getMapboxAccessToken(this));
        optionsBuilder.origin(origin);
        optionsBuilder.destination(destination);
        for (Point waypointPoint : waypoints) {
          optionsBuilder.addWaypoint(waypointPoint);
        }
        directionsSession.requestRoutes(optionsBuilder.build());
      }
    }
  }

  /*
   * DirectionSessions.RouteObserver
   */

  @Override
  public void onRoutesChanged(@NotNull List<Route> routes) {
    if (!routes.isEmpty()) {
      route = Mappers.mapToDirectionsRoute(routes.get(0));
      navigationMapRoute.addRoute(route);
    }
  }

  @Override
  public void onRoutesRequested() {
    Timber.d("onRoutesRequested: navigation.getRoute()");
  }

  @Override
  public void onRoutesRequestFailure(@NotNull Throwable throwable) {
    Timber.e(throwable, "onRoutesRequestFailure: navigation.getRoute()");
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
    if (directionsSession != null) {
      directionsSession.cancel();
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
