package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.mapbox.maps.MapboxMapOptions;
import com.mapbox.maps.ResourceOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationComponentPlugin;
import com.mapbox.maps.plugin.location.LocationUpdate;
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
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.examples.util.LocationPermissionsHelper;
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer;
import com.mapbox.navigation.ui.maps.PredictiveCacheController;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineState;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
//import static com.mapbox.android.gestures.Utils.dpToPx;
import static com.mapbox.navigation.examples.util.LocationPermissionsHelperKt.LOCATION_PERMISSIONS_REQUEST_CODE;
import static com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID;

public class MapboxRouteLineApiExampleActivity extends AppCompatActivity implements PermissionsListener,
    OnMapLongClickListener {

  //private final float routeClickPadding = dpToPx(30f);
  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private final LocationPermissionsHelper permissionsHelper = new LocationPermissionsHelper(this);
  private MapView mapView;
  private MapboxMap mapboxMap;
  private PredictiveCacheController predictiveCacheController;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPlugin mapCamera;
  private final MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private MapboxNavigation mapboxNavigation;
  private Button startNavigation;
  private ProgressBar routeLoading;
  private final List<String> mapStyles = Arrays.asList(
      Style.MAPBOX_STREETS,
      Style.OUTDOORS,
      Style.LIGHT,
      Style.SATELLITE_STREETS
  );
  private MapboxRouteLineApi mapboxRouteLineApi;
  private MapboxRouteLineView mapboxRouteLineView;
  private final MapboxRouteArrowApi routeArrow = new MapboxRouteArrowApi();
  private MapboxRouteArrowView routeArrowView;

  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_map_route);
    MapboxMapOptions mapboxMapOptions = new MapboxMapOptions(this, null, getResources().getDisplayMetrics().density);
    ResourceOptions resourceOptions = new ResourceOptions.Builder()
        .accessToken(getMapboxAccessTokenFromResources())
        .assetPath(getFilesDir().getAbsolutePath())
        .cachePath(getFilesDir().getAbsolutePath() + "/mbx.db")
        .cacheSize(100_000_000L) // 100 MB
        .tileStorePath(getFilesDir().getAbsolutePath() + "/maps_tile_store/")
        .build();
    mapboxMapOptions.setResourceOptions(resourceOptions);
    mapView = new MapView(this, mapboxMapOptions);
    RelativeLayout mapLayout = findViewById(R.id.mapView_container);
    mapLayout.addView(mapView);

    startNavigation = findViewById(R.id.startNavigation);
    routeLoading = findViewById(R.id.routeLoadingProgressBar);
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

    RouteLineResources routeLineResources = new RouteLineResources.Builder().build();
    MapboxRouteLineOptions mapboxRouteLineOptions = new MapboxRouteLineOptions.Builder(this)
        .withRouteLineResources(routeLineResources)
        .withVanishingRouteLineEnabled(true)
        .build();

    mapboxRouteLineApi = new MapboxRouteLineApi(mapboxRouteLineOptions);
    mapboxRouteLineView = new MapboxRouteLineView(mapboxRouteLineOptions);

    RouteArrowOptions routeArrowOptions = new RouteArrowOptions.Builder(this)
        .withAboveLayerId(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
        .build();
    routeArrowView = new MapboxRouteArrowView(routeArrowOptions);

    predictiveCacheController = new PredictiveCacheController(mapboxNavigation, message -> {
      Timber.e("predictive cache error: %s", message);
    });
    predictiveCacheController.setMapInstance(mapboxMap);
  }

  @SuppressLint("MissingPermission")
  private void initListeners() {
    startNavigation.setOnClickListener(v -> {
      locationComponent.setRenderMode(RenderMode.GPS);
      mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.startTripSession();
      startNavigation.setVisibility(View.GONE);
      getLocationComponent().addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    });

    ((FloatingActionButton)findViewById(R.id.fabToggleStyle)).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Visibility primaryRouteLineVisibility = mapboxRouteLineView.getPrimaryRouteVisibility(mapboxMap.getStyle());
        Visibility alternativeRouteLineVisibility = mapboxRouteLineView.getAlternativeRoutesVisibility(mapboxMap.getStyle());
        Visibility arrowVisibility = routeArrowView.getVisibility(mapboxMap.getStyle());

        Collections.shuffle(mapStyles);
        String style = mapStyles.get(0);
        Timber.e("*** Chosen map style is %s", style);
        locationComponent.setEnabled(false);
        mapboxMap.loadStyleUri(style, new Style.OnStyleLoaded() {
          @Override public void onStyleLoaded(@NotNull Style style) {
            new Handler().post(new Runnable() {
              @Override
              public void run() {
                // workaround for https://github.com/mapbox/mapbox-maps-android/issues/900
                locationComponent.setEnabled(true);
              }
            });
            RouteLineState.UpdateLayerVisibilityState primaryRouteVisibilityState;
            if (primaryRouteLineVisibility == Visibility.VISIBLE) {
              primaryRouteVisibilityState = mapboxRouteLineApi.showPrimaryRoute();
            } else {
              primaryRouteVisibilityState = mapboxRouteLineApi.hidePrimaryRoute();
            }
            mapboxRouteLineView.render(style, primaryRouteVisibilityState);

            RouteLineState.UpdateLayerVisibilityState alternateRouteVisibilityState;
            if (alternativeRouteLineVisibility == Visibility.VISIBLE) {
              alternateRouteVisibilityState = mapboxRouteLineApi.showAlternativeRoutes();
            } else {
              alternateRouteVisibilityState = mapboxRouteLineApi.hideAlternativeRoutes();
            }
            mapboxRouteLineView.render(style, alternateRouteVisibilityState);

            RouteLineState.RouteSetState redrawData = mapboxRouteLineApi.getRouteDrawData();
            mapboxRouteLineView.render(style, redrawData);

            RouteArrowState.UpdateRouteArrowVisibilityState arrowVisibilityState;
            if (arrowVisibility == Visibility.VISIBLE) {
              arrowVisibilityState = routeArrow.showManeuverArrow();
            } else {
              arrowVisibilityState = routeArrow.hideManeuverArrow();
            }
            routeArrowView.render(style, arrowVisibilityState);

            RouteArrowState.UpdateManeuverArrowState redrawState = routeArrow.redraw();
            routeArrowView.render(style, redrawState);
          }
        }, null);
      }
    });

    getGesturePlugin().addOnMapClickListener(mapClickListener);
  }

  @SuppressLint("MissingPermission")
  private void initNavigation() {
    NavigationOptions navigationOptions = MapboxNavigation
        .defaultNavigationOptionsBuilder(MapboxRouteLineApiExampleActivity.this, getMapboxAccessTokenFromResources())
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
    // a style without composite sources
    String styleId = "mapbox://styles/lukaspaczos/ckirf03jn7hv817nrr69ndwdw";
    mapboxMap.loadStyleUri(styleId, style -> {
      initializeLocationComponent(style);
      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);

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
    getLocationComponent().removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    if (mapboxNavigation != null) {
      mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
    }
    mapView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (predictiveCacheController != null) {
      predictiveCacheController.onDestroy();
    }
    mapView.onDestroy();
    mapboxNavigation.onDestroy();
  }

  @Override public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public boolean onMapLongClick(@NotNull Point point) {
    vibrate();
    startNavigation.setVisibility(View.GONE);

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
      List<RouteLine> routeLines = new ArrayList<>();
      for (DirectionsRoute route: routes) {
        routeLines.add(new RouteLine(route, null));
      }
      RouteLineState.RouteSetState routeDrawData = mapboxRouteLineApi.setRoutes(routeLines);
      mapboxRouteLineView.render(mapboxMap.getStyle(), routeDrawData);
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
    LocationUpdate locationUpdate = new LocationUpdate(location, null, null);
    locationComponent.forceLocationUpdate(locationUpdate);

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

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<MapboxRouteLineApiExampleActivity> activityRef;

    MyLocationEngineCallback(MapboxRouteLineApiExampleActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      Location location = result.getLastLocation();
      MapboxRouteLineApiExampleActivity activity = activityRef.get();
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
    Toast.makeText(
        this,
        "This app needs location and storage permissions in order to show its functionality.",
        Toast.LENGTH_LONG
    ).show();
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

  private OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
    RouteLineState.VanishingRouteLineUpdateState vanishingRouteLineData = mapboxRouteLineApi.updateTraveledRouteLine(point);
    if (vanishingRouteLineData != null && mapboxMap.getStyle() != null) {
      mapboxRouteLineView.render(mapboxMap.getStyle(), vanishingRouteLineData);
    }
  };

  private final RouteProgressObserver routeProgressObserver = new RouteProgressObserver() {
    @Override public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
      if (mapboxMap.getStyle() == null) {
        return;
      }
      mapboxRouteLineApi.updateWithRouteProgress(routeProgress);

      RouteArrowState.UpdateManeuverArrowState updateArrowState = routeArrow.updateUpcomingManeuverArrow(routeProgress);
      routeArrowView.render(mapboxMap.getStyle(), updateArrowState);

      DirectionsRoute currentRoute = routeProgress.getRoute();
      boolean hasGeometry = false;
      if (currentRoute.geometry() != null && !currentRoute.geometry().isEmpty()) {
        hasGeometry = true;
      }

      boolean isNewRoute = false;
      if (hasGeometry && currentRoute != mapboxRouteLineApi.getPrimaryRoute()) {
        isNewRoute = true;
      }

      if (isNewRoute) {
        RouteLineState.RouteSetState routeDrawData =
            mapboxRouteLineApi.setRoutes(
                Collections.singletonList(new RouteLine(routeProgress.getRoute(), null))
            );
        mapboxRouteLineView.render(mapboxMap.getStyle(), routeDrawData);
      }
    }
  };

  private final MapboxNavigationConsumer<RouteLineState.ClosestRouteState> closestRouteResultConsumer =
      new MapboxNavigationConsumer<RouteLineState.ClosestRouteState>() {
        @Override public void accept(RouteLineState.ClosestRouteState closestRouteState) {
          final int index = closestRouteState.getRouteIndex();
          if (index >= 0) {
            final DirectionsRoute selectedRoute = mapboxRouteLineApi.getRoutes().get(index);
            if (selectedRoute != mapboxRouteLineApi.getPrimaryRoute()) {
              mapboxRouteLineApi.updateToPrimaryRoute(selectedRoute);
              // NOTE: We don't have to render the state because there is a RoutesObserver on the
              // MapboxNavigation object which will draw the routes. Rendering the state would draw the routes
              // twice unnecessarily in this implementation.
              mapboxNavigation.setRoutes(mapboxRouteLineApi.getRoutes());
            }
          }
        }
  };

  private final OnMapClickListener mapClickListener = new OnMapClickListener() {
    @Override public boolean onMapClick(@NotNull Point point) {
      Visibility primaryLineVisibility = mapboxRouteLineView.getPrimaryRouteVisibility(mapboxMap.getStyle());
      Visibility alternativeRouteLinesVisibility = mapboxRouteLineView.getAlternativeRoutesVisibility(mapboxMap.getStyle());
      if (primaryLineVisibility == Visibility.VISIBLE && alternativeRouteLinesVisibility == Visibility.VISIBLE) {
        // temporarily commenting this out since there is a bug on the maps side that causes a crash when
        // MapboxMap::queryRenderedFeatures is called.
        // mapboxRouteLineApi.findClosestRoute(point, mapboxMap, routeClickPadding, closestRouteResultConsumer);
        // issue: https://github.com/mapbox/mapbox-maps-android/issues/885
      }
      return false;
    }
  };
}
