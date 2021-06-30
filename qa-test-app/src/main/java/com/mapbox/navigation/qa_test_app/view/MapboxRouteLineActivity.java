package com.mapbox.navigation.qa_test_app.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.bindgen.Expected;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
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
import com.mapbox.navigation.base.extensions.RouteOptionsExtensions;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.options.RoutingTilesOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.history.ReplayEventBase;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.qa_test_app.R;
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer;
import com.mapbox.navigation.ui.maps.PredictiveCacheController;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView;
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowAddedValue;
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowVisibilityChangeValue;
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions;
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources;
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound;
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineUpdateValue;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mapbox.android.gestures.Utils.dpToPx;
import static com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID;

public class MapboxRouteLineActivity extends AppCompatActivity implements OnMapLongClickListener {

  private static final String TAG = "MapboxRouteLineActivity";
  private final float routeClickPadding = dpToPx(30f);
  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private PredictiveCacheController predictiveCacheController;
  protected NavigationLocationProvider navigationLocationProvider;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPlugin mapCamera;
  private final ReplayRouteMapper replayRouteMapper = new ReplayRouteMapper();
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
    setContentView(R.layout.layout_activity_routeline);

    String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
    NavigationOptions navigationOptions = new NavigationOptions.Builder(this)
        .accessToken(getMapboxAccessTokenFromResources())
        .locationEngine(new ReplayLocationEngine(mapboxReplayer))
        .routingTilesOptions(
            new RoutingTilesOptions.Builder()
                .build()
                .toBuilder()
                .filePath(path)
                .build()
        )
        .build();

    mapView = findViewById(R.id.mapView);
    startNavigation = findViewById(R.id.startNavigation);
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

    init(navigationOptions);
  }

  private void init(NavigationOptions navigationOptions) {
    initNavigation(navigationOptions);
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
      Log.e(TAG, "predictive cache error: " + message);
    });
    predictiveCacheController.setMapInstance(mapboxMap);
  }

  @SuppressLint("MissingPermission")
  private void initListeners() {
    startNavigation.setOnClickListener(v -> {
      mapboxNavigation.startTripSession();
      startNavigation.setVisibility(View.GONE);
      getLocationComponent().addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
      startSimulation(mapboxNavigation.getRoutes().get(0));
    });


    ((FloatingActionButton) findViewById(R.id.fabToggleStyle)).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (mapboxMap.getStyle() == null || !mapboxMap.getStyle().isFullyLoaded()) {
          return;
        }

        Visibility primaryRouteLineVisibility = mapboxRouteLineView.getPrimaryRouteVisibility(mapboxMap.getStyle());
        Visibility alternativeRouteLineVisibility = mapboxRouteLineView.getAlternativeRoutesVisibility(mapboxMap.getStyle());
        Visibility arrowVisibility = routeArrowView.getVisibility(mapboxMap.getStyle());

        Collections.shuffle(mapStyles);
        String style = mapStyles.get(0);
        Log.e(TAG, "*** Chosen map style is " + style);
        mapboxMap.loadStyleUri(style, style1 -> {
          if (primaryRouteLineVisibility == Visibility.NONE) {
            mapboxRouteLineView.hidePrimaryRoute(style1);
          } else {
            mapboxRouteLineView.showPrimaryRoute(style1);
          }

          if (alternativeRouteLineVisibility == Visibility.NONE) {
            mapboxRouteLineView.hideAlternativeRoutes(style1);
          } else {
            mapboxRouteLineView.showAlternativeRoutes(style1);
          }

          mapboxRouteLineApi.getRouteDrawData(
              redrawData -> mapboxRouteLineView.renderRouteDrawData(style1, redrawData)
          );

          ArrowVisibilityChangeValue arrowVisibilityState;
          if (arrowVisibility == Visibility.NONE) {
            arrowVisibilityState = routeArrow.hideManeuverArrow();
          } else {
            arrowVisibilityState = routeArrow.showManeuverArrow();
          }
          routeArrowView.render(style1, arrowVisibilityState);

          ArrowAddedValue redrawState = routeArrow.redraw();
          routeArrowView.render(style1, redrawState);
        }, null);
      }
    });

    getGesturePlugin().addOnMapClickListener(mapClickListener);
  }

  @SuppressLint("MissingPermission")
  private void initNavigation(NavigationOptions navigationOptions) {
    mapboxNavigation = new MapboxNavigation(navigationOptions);

    mapboxReplayer.pushRealLocation(this, 0.0);
    mapboxReplayer.playbackSpeed(1.5);
    mapboxReplayer.play();
  }

  private void startSimulation(DirectionsRoute route) {
    mapboxReplayer.stop();
    mapboxReplayer.clearEvents();
    List<ReplayEventBase> replayData = replayRouteMapper.mapDirectionsRouteGeometry(route);
    mapboxReplayer.pushEvents(replayData);
    mapboxReplayer.seekTo(replayData.get(0));
    mapboxReplayer.play();
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    // a style without composite sources
    String styleId = "mapbox://styles/lukaspaczos/ckirf03jn7hv817nrr69ndwdw";
    mapboxMap.loadStyleUri(styleId, style -> {
      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);
      getGesturePlugin().addOnMapLongClickListener(this);
    }, (mapLoadError, s) -> Log.e(TAG, "Error loading map: " + mapLoadError.name()));
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
    startNavigation.setVisibility(View.GONE);
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
    RouteOptions.Builder builder = RouteOptions.builder();
    RouteOptionsExtensions.applyDefaultNavigationOptions(builder);
    RouteOptionsExtensions.applyLanguageAndVoiceUnitOptions(builder, this);
    RouteOptions routeOptions = builder
        .accessToken(getMapboxAccessTokenFromResources())
        .coordinatesList(Arrays.asList(origin, destination))
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
      mapboxNavigation.setRoutes(routes);
      if (!routes.isEmpty()) {
        routeLoading.setVisibility(View.INVISIBLE);
        startNavigation.setVisibility(View.VISIBLE);
      }
    }

    @Override
    public void onRoutesRequestFailure(@NotNull Throwable throwable, @NotNull RouteOptions routeOptions) {
      Log.e(TAG, "route request failure " + throwable.toString());
      routeLoading.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRoutesRequestCanceled(@NotNull RouteOptions routeOptions) {
      Log.d(TAG, "route request canceled");
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
      long start = System.currentTimeMillis();
      mapboxRouteLineApi.setRoutes(routeLines, routeDrawData -> {
        Log.e("foobar", "total calc time: " + (System.currentTimeMillis() - start));

        long startDraw = System.currentTimeMillis();
        mapboxRouteLineView.renderRouteDrawData(mapboxMap.getStyle(), routeDrawData);
        Log.e("foobar", "total draw time: " + (System.currentTimeMillis() - startDraw));
      });
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
      Log.d(TAG, "raw location " + rawLocation.toString());
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
    return CameraAnimationsPluginImplKt.getCamera(mapView);
  }

  private GesturesPluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturesPluginImpl.class);
  }

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);

  private OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
    Expected<RouteLineError, VanishingRouteLineUpdateValue> vanishingRouteLineData = mapboxRouteLineApi.updateTraveledRouteLine(point);
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

      Expected<InvalidPointError, UpdateManeuverArrowValue> updateArrowState = routeArrow.addUpcomingManeuverArrow(routeProgress);
      routeArrowView.renderManeuverUpdate(mapboxMap.getStyle(), updateArrowState);

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
        mapboxRouteLineApi.setRoutes(
            Collections.singletonList(new RouteLine(routeProgress.getRoute(), null)),
            routeDrawData ->
                mapboxRouteLineView.renderRouteDrawData(mapboxMap.getStyle(), routeDrawData)
        );
      }
    }
  };

  private final MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>> closestRouteResultConsumer =
      new MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>() {
        @Override
        public void accept(Expected<RouteNotFound, ClosestRouteValue> closestRouteResult) {
          closestRouteResult.onValue(input -> {
            final DirectionsRoute selectedRoute = input.getRoute();
            if (selectedRoute != mapboxRouteLineApi.getPrimaryRoute()) {
              List<DirectionsRoute> updatedRoutes = mapboxRouteLineApi.getRoutes();
              updatedRoutes.remove(selectedRoute);
              updatedRoutes.add(0, selectedRoute);
              mapboxNavigation.setRoutes(updatedRoutes);
            }
          });
        }
      };

  private final OnMapClickListener mapClickListener = new OnMapClickListener() {
    @Override
    public boolean onMapClick(@NotNull Point point) {
      if (mapboxMap.getStyle() != null && mapboxMap.getStyle().isFullyLoaded()) {
        Visibility primaryLineVisibility = mapboxRouteLineView.getPrimaryRouteVisibility(mapboxMap.getStyle());
        Visibility alternativeRouteLinesVisibility = mapboxRouteLineView.getAlternativeRoutesVisibility(mapboxMap.getStyle());
        if (primaryLineVisibility == Visibility.VISIBLE && alternativeRouteLinesVisibility == Visibility.VISIBLE) {
          mapboxRouteLineApi.findClosestRoute(point, mapboxMap, routeClickPadding, closestRouteResultConsumer);
        }
      }
      return false;
    }
  };

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<MapboxRouteLineActivity> activityRef;

    MyLocationEngineCallback(MapboxRouteLineActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      Location location = result.getLastLocation();
      MapboxRouteLineActivity activity = activityRef.get();
      if (location != null && activity != null) {
        Point point = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        CameraOptions cameraOptions = new CameraOptions.Builder().center(point).zoom(13.0).build();
        activity.mapboxMap.setCamera(cameraOptions);
        activityRef.get().navigationLocationProvider.changePosition(location, Collections.emptyList(), null, null);
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
      Log.i(TAG, exception.getMessage());
    }
  }
}
