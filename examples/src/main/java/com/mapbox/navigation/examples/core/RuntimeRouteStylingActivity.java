package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
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
import com.mapbox.navigation.ui.route.IdentifiableRoute;
import com.mapbox.navigation.ui.route.NavigationMapRoute;
import com.mapbox.navigation.ui.route.RouteStyleDescriptor;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * This activity demonstrates fine grained control over the route line coloring. You can override
 * the defined styling and apply a data driven line color that you can define at runtime.
 */
public class RuntimeRouteStylingActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapLongClickListener {
  private static final int ONE_HUNDRED_MILLISECONDS = 100;

  @BindView(R.id.mapView)
  MapView mapView;
  @BindView(R.id.routeLoadingProgressBar)
  ProgressBar routeLoading;

  private MapboxMap mapboxMap;
  private NavigationMapRoute navigationMapRoute;
  private MapboxNavigation mapboxNavigation;
  private NavigationCamera mapCamera;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();

  private static final String PRIMARY_ROUTE_IDENTIFIER = "undefined";
  private static final String ALTERNATIVE_ROUTE_0 = "alternativeRoute0";
  private static final String ALTERNATIVE_ROUTE_1 = "alternativeRoute1";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_runtime_route_styling);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      initializeLocationComponent(mapboxMap, style);

      NavigationOptions navigationOptions = MapboxNavigation
        .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
        .locationEngine(new ReplayLocationEngine(mapboxReplayer))
        .build();
      mapboxNavigation = new MapboxNavigation(navigationOptions);
      mapboxNavigation.registerLocationObserver(locationObserver);
      mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
      mapboxReplayer.pushRealLocation(this, 0.0);
      mapboxReplayer.play();

      mapCamera = new NavigationCamera(mapboxMap);
      mapCamera.addProgressChangeListener(mapboxNavigation);

      List<RouteStyleDescriptor> routeDescriptors =
              getRouteStyleDescriptors(this);

      navigationMapRoute = new NavigationMapRoute.Builder(mapView, mapboxMap, this)
              .withMapboxNavigation(mapboxNavigation)
              .withRouteStyleDescriptors(routeDescriptors)
              .build();

      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);
      mapboxMap.addOnMapLongClickListener(this);
      Snackbar.make(mapView, R.string.msg_long_press_map_to_place_waypoint, Snackbar.LENGTH_SHORT).show();
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
    mapboxNavigation.stopTripSession();
    mapboxNavigation.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
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
            .alternatives(true)
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

          // By adding identifiable routes you can specify route colors be overridden with
          // values you defined with the RouteStyleDescriptors. Below you can see
          // the routes are identified with labels that match the RouteStyleDescriptors
          // passed into the NavigationMapRoute builder. Routes that have no pre-defined
          // identity will get the default color from the style applied.
          List<IdentifiableRoute> identifiableRoutes = new ArrayList<>();
          for (int i = 0; i < routes.size(); i++) {
            if (i == 0) {
              identifiableRoutes.add(new IdentifiableRoute(routes.get(i), PRIMARY_ROUTE_IDENTIFIER));
            } else if (i == 1) {
              identifiableRoutes.add(new IdentifiableRoute(routes.get(i), ALTERNATIVE_ROUTE_0));
            } else {
              identifiableRoutes.add(new IdentifiableRoute(routes.get(i), ALTERNATIVE_ROUTE_1));
            }
          }
          navigationMapRoute.addIdentifiableRoutes(identifiableRoutes);
          routeLoading.setVisibility(View.INVISIBLE);
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


  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<RuntimeRouteStylingActivity> activityRef;

    MyLocationEngineCallback(RuntimeRouteStylingActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }


    @Override
    public void onSuccess(LocationEngineResult result) {
      activityRef.get().updateLocation(result.getLocations());
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
      Timber.i(exception);
    }
  }

  private void updateLocation(Location location) {
    updateLocation(Arrays.asList(location));
  }

  private void updateLocation(List<Location> locations) {
    long minimalRequiredLookAheadTimestamp = System.currentTimeMillis();
    boolean lookahead = locations.get(0).getTime() > minimalRequiredLookAheadTimestamp;
    mapboxMap.getLocationComponent().forceLocationUpdate(locations, lookahead);
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

  // The route style descriptors give you an opportunity to indicate route line(s) with
  // a given identifier should be colored with a color other than the color specified in style.
  // Routes with the same identifiers as those defined here will have their color overridden
  // with the values defined in the descriptor.
  private List<RouteStyleDescriptor> getRouteStyleDescriptors(Context context) {
    int firstAlternativeRouteDefaultColor = ContextCompat.getColor(context, R.color.customAlternativeRouteColor);
    int firstAlternativeRouteShieldColor = ContextCompat.getColor(context, R.color.customAlternativeRouteCasingColor);
    int otherAlternativeRouteDefaultColor = ContextCompat.getColor(context, R.color.customRouteLowCongestionColor);
    int otherAlternativeRouteShieldColor = ContextCompat.getColor(context, R.color.customRouteSevereCongestionColor);

    return Arrays.asList(
            new RouteStyleDescriptor(
                    ALTERNATIVE_ROUTE_0,
                    firstAlternativeRouteDefaultColor,
                    firstAlternativeRouteShieldColor
            ),
            new RouteStyleDescriptor(
                    ALTERNATIVE_ROUTE_1,
                    otherAlternativeRouteDefaultColor,
                    otherAlternativeRouteShieldColor
            )
    );
  }
}
