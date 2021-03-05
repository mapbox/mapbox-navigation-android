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
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPluginImpl;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
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
import com.mapbox.navigation.ui.base.model.Expected;
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer;
import com.mapbox.navigation.ui.maps.PredictiveCacheController;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources;
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue;
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineUpdateValue;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.android.gestures.Utils.dpToPx;
import static com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID;

public class MapboxRouteLineActivity extends AppCompatActivity implements OnMapLongClickListener {

  private final float routeClickPadding = dpToPx(30f);
  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private PredictiveCacheController predictiveCacheController;
  private NavigationLocationProvider navigationLocationProvider;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPlugin mapCamera;
  private final MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private MapboxNavigation mapboxNavigation;
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
    setContentView(R.layout.layout_activity_routeline);
    MapboxMapOptions mapboxMapOptions = new MapboxMapOptions(this, getResources().getDisplayMetrics().density, null);
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

    routeLoading = findViewById(R.id.routeLoadingProgressBar);
    mapboxMap = mapView.getMapboxMap();
    navigationLocationProvider = new NavigationLocationProvider();
    locationComponent = getLocationComponent();
    locationComponent.setLocationPuck(
        new LocationPuck2D(null, ContextCompat.getDrawable(this, R.drawable.mapbox_navigation_puck_icon), null, null)
    );
    locationComponent.setLocationProvider(navigationLocationProvider);
    locationComponent.setEnabled(true);
    mapCamera = getMapCamera();

    init();
  }

  private void init() {
    initNavigation();
    initStyle();
    initListeners();

    RouteLineResources routeLineResources = new RouteLineResources.Builder().build();
    MapboxRouteLineOptions mapboxRouteLineOptions = new MapboxRouteLineOptions.Builder(this)
        .withRouteLineResources(routeLineResources)
        .withVanishingRouteLineEnabled(true)
        .withRouteLineBelowLayerId("road-label")
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
    ((FloatingActionButton) findViewById(R.id.fabToggleStyle)).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Visibility primaryRouteLineVisibility = mapboxRouteLineView.getPrimaryRouteVisibility(mapboxMap.getStyle());
        Visibility alternativeRouteLineVisibility = mapboxRouteLineView.getAlternativeRoutesVisibility(mapboxMap.getStyle());
        Visibility arrowVisibility = routeArrowView.getVisibility(mapboxMap.getStyle());

        Collections.shuffle(mapStyles);
        String style = mapStyles.get(0);
        Timber.e("*** Chosen map style is %s", style);
        mapboxMap.loadStyleUri(style, new Style.OnStyleLoaded() {
          @Override
          public void onStyleLoaded(@NotNull Style style) {
            if (primaryRouteLineVisibility == Visibility.NONE) {
              mapboxRouteLineView.hidePrimaryRoute(style);
            } else {
              mapboxRouteLineView.showPrimaryRoute(style);
            }

            if (alternativeRouteLineVisibility == Visibility.NONE) {
              mapboxRouteLineView.hideAlternativeRoutes(style);
            } else {
              mapboxRouteLineView.showAlternativeRoutes(style);
            }

            Expected<RouteSetValue, RouteLineError> redrawData = mapboxRouteLineApi.getRouteDrawData();
            mapboxRouteLineView.renderRouteDrawData(style, redrawData);

            RouteArrowState.UpdateRouteArrowVisibilityState arrowVisibilityState;
            if (arrowVisibility == Visibility.NONE) {
              arrowVisibilityState = routeArrow.hideManeuverArrow();
            } else {
              arrowVisibilityState = routeArrow.showManeuverArrow();
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
    mapboxNavigation = new MapboxNavigation(
        new NavigationOptions.Builder(this)
            .accessToken(getMapboxAccessTokenFromResources())
            .locationEngine(new ReplayLocationEngine(mapboxReplayer))
            .build()
    );
    mapboxNavigation.registerLocationObserver(locationObserver);
    mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
    mapboxNavigation.registerRoutesObserver(routesObserver);
    mapboxNavigation.startTripSession();

    mapboxReplayer.pushRealLocation(this, 0.0);
    mapboxReplayer.play();
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    // a style without composite sources
    String styleId = "mapbox://styles/lukaspaczos/ckirf03jn7hv817nrr69ndwdw";
    mapboxMap.loadStyleUri(styleId, style -> {
      getGesturePlugin().addOnMapLongClickListener(this);
    }, (mapLoadError, s) -> Timber.e("Error loading map: %s", mapLoadError.name()));
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mapboxNavigation != null) {
      mapboxNavigation.registerLocationObserver(locationObserver);
      mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
      mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.registerRoutesObserver(routesObserver);
    }
    getLocationComponent().addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    getLocationComponent().removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    if (mapboxNavigation != null) {
      mapboxNavigation.unregisterLocationObserver(locationObserver);
      mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver);
      mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.unregisterRoutesObserver(routesObserver);
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

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public boolean onMapLongClick(@NotNull Point point) {
    vibrate();
    Location currentLocation = navigationLocationProvider.getLastLocation();
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
      routeLoading.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRoutesRequestFailure(@NotNull Throwable throwable, @NotNull RouteOptions routeOptions) {
      Timber.e("route request failure %s", throwable.toString());
      routeLoading.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRoutesRequestCanceled(@NotNull RouteOptions routeOptions) {
      Timber.d("route request canceled");
      routeLoading.setVisibility(View.INVISIBLE);
    }
  };

  private RoutesObserver routesObserver = new RoutesObserver() {
    @Override
    public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
      List<RouteLine> routeLines = new ArrayList<>();
      for (DirectionsRoute route : routes) {
        routeLines.add(new RouteLine(route, null));
      }
      Expected<RouteSetValue, RouteLineError> routeDrawData = mapboxRouteLineApi.setRoutes(routeLines);
      mapboxRouteLineView.renderRouteDrawData(mapboxMap.getStyle(), routeDrawData);
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
      navigationLocationProvider.changePosition(enhancedLocation, keyPoints, null, null);
      updateCamera(enhancedLocation);
    }
  };

  private void updateCamera(Location location) {
    MapAnimationOptions.Builder mapAnimationOptionsBuilder = new MapAnimationOptions.Builder();
    mapAnimationOptionsBuilder.duration(1500L);
    mapCamera.easeTo(
        new CameraOptions.Builder()
            .center(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
            .bearing((double) location.getBearing())
            .pitch(45.0)
            .zoom(17.0)
            .padding(new EdgeInsets(1000, 0, 0, 0))
            .build(),
        mapAnimationOptionsBuilder.build()
    );
  }

  private LocationComponentPluginImpl getLocationComponent() {
    return mapView.getPlugin(LocationComponentPluginImpl.class);
  }

  private CameraAnimationsPlugin getMapCamera() {
    return CameraAnimationsPluginImplKt.getCameraAnimationsPlugin(mapView);
  }

  private GesturesPluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturesPluginImpl.class);
  }

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);

  private OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
    Expected<VanishingRouteLineUpdateValue, RouteLineError> vanishingRouteLineData = mapboxRouteLineApi.updateTraveledRouteLine(point);
    if (vanishingRouteLineData != null && mapboxMap.getStyle() != null) {
      mapboxRouteLineView.renderVanishingRouteLineUpdateValue(mapboxMap.getStyle(), vanishingRouteLineData);
    }
  };

  private final RouteProgressObserver routeProgressObserver = new RouteProgressObserver() {
    @Override
    public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
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
        Expected<RouteSetValue, RouteLineError> routeDrawData =
            mapboxRouteLineApi.setRoutes(
                Collections.singletonList(new RouteLine(routeProgress.getRoute(), null))
            );
        mapboxRouteLineView.renderRouteDrawData(mapboxMap.getStyle(), routeDrawData);
      }
    }
  };

  private final MapboxNavigationConsumer<Expected<ClosestRouteValue, RouteLineError>> closestRouteResultConsumer =
      new MapboxNavigationConsumer<Expected<ClosestRouteValue, RouteLineError>>() {
        @Override
        public void accept(Expected<ClosestRouteValue, RouteLineError> closestRouteResult) {
          if (closestRouteResult instanceof Expected.Success) {
            final int index = ((int)((Expected.Success) closestRouteResult).getValue());
            if (index > 0) {
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
        }
      };

  private final OnMapClickListener mapClickListener = new OnMapClickListener() {
    @Override
    public boolean onMapClick(@NotNull Point point) {
      Visibility primaryLineVisibility = mapboxRouteLineView.getPrimaryRouteVisibility(mapboxMap.getStyle());
      Visibility alternativeRouteLinesVisibility = mapboxRouteLineView.getAlternativeRoutesVisibility(mapboxMap.getStyle());
      if (primaryLineVisibility == Visibility.VISIBLE && alternativeRouteLinesVisibility == Visibility.VISIBLE) {
        mapboxRouteLineApi.findClosestRoute(point, mapboxMap, routeClickPadding, closestRouteResultConsumer);
      }
      return false;
    }
  };
}
