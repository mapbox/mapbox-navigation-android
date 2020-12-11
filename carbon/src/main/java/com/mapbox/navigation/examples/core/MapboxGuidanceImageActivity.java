package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.*;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationComponentPlugin;
import com.mapbox.maps.plugin.location.modes.RenderMode;
import com.mapbox.maps.renderer.MapboxRenderer;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.examples.util.Slackline;
import com.mapbox.navigation.ui.base.api.guidanceimage.GuidanceImageApi;
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState;
import com.mapbox.navigation.ui.maps.guidance.api.MapboxGuidanceImageApi;
import com.mapbox.navigation.ui.maps.guidance.api.OnGuidanceImageReady;
import com.mapbox.navigation.ui.maps.guidance.model.GuidanceImageOptions;
import com.mapbox.navigation.ui.maps.guidance.view.MapboxGuidanceView;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static com.mapbox.navigation.examples.util.Utils.getMapboxAccessToken;
import static com.mapbox.navigation.examples.util.Utils.getMapboxRouteAccessToken;

public class MapboxGuidanceImageActivity extends AppCompatActivity implements OnMapLongClickListener {

  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationComponentPlugin locationComponent;
  private CameraAnimationsPlugin mapCamera;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private MapboxNavigation mapboxNavigation;
  private Button startNavigation;
  private Slackline slackline = new Slackline(this);
  private GuidanceImageApi guidanceImageApi;
  private MapboxGuidanceView guidanceView;

  private OnGuidanceImageReady callback = new OnGuidanceImageReady() {
    @Override public void onGuidanceImagePrepared(@NotNull GuidanceImageState.GuidanceImagePrepared bitmap) {
      guidanceView.render(bitmap);
    }

    @Override public void onFailure(@NotNull GuidanceImageState.GuidanceImageFailure error) {
      //guidanceView.render(error);
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
    init();
  }

  private void init() {
    initNavigation();
    initStyle();
    slackline.initialize(mapView, mapboxNavigation);
    initListeners();
  }

  @SuppressLint("MissingPermission")
  private void initListeners() {
    startNavigation.setOnClickListener(v -> {
      locationComponent.setRenderMode(RenderMode.GPS);
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
    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);

    float density = getResources().getDisplayMetrics().density;
    GuidanceImageOptions options = new GuidanceImageOptions.Builder()
      .density(density)
      .edgeInsets(new EdgeInsets(60.0*density, 10.0*density, 10.0*density, 10.0*density))
      .styleUri("mapbox://styles/mapbox-map-design/ckifcx2i84huf19pbvgi0cka6")
      .build();

    MapInterface mapInterface = null;
    try {

      Field privateMapViewField = MapView.class.getDeclaredField("mapController");
      privateMapViewField.setAccessible(true);
      MapController controller = (MapController) privateMapViewField.get(mapView);

      Field privateMapControllerField = MapController.class.getDeclaredField("renderer");
      privateMapControllerField.setAccessible(true);
      MapboxRenderer renderer = (MapboxRenderer) privateMapControllerField.get(controller);

      Field privateMapRendererField = MapboxRenderer.class.getDeclaredField("map");
      privateMapRendererField.setAccessible(true);
      mapInterface = (MapInterface) privateMapRendererField.get(renderer);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    guidanceImageApi = new MapboxGuidanceImageApi(this, mapboxMap, mapInterface, options, callback);

    mapboxReplayer.pushRealLocation(this, 0.0);
    mapboxReplayer.play();
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
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
    if (mapboxNavigation != null) {
      mapboxNavigation.unregisterLocationObserver(locationObserver);
      mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver);
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
      Point or = Point.fromLngLat(-3.5870,40.5719);
      Point de = Point.fromLngLat(-3.607835,40.551486);
      findRoute(or, de);
    }
    return false;
  }

  public void findRoute(Point origin, Point destination) {
    RouteOptions routeOptions = RouteOptions.builder()
        .baseUrl("https://api-valhalla-route-staging.tilestream.net/")
        .user("directions-team")
        .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
        .geometries(RouteUrl.GEOMETRY_POLYLINE6)
        .requestUuid("")
        .accessToken(getMapboxRouteAccessToken(this))
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
        //routeLoading.setVisibility(View.INVISIBLE);
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
    return getMapboxAccessToken(this);
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

  private RouteProgressObserver routeProgressObserver = new RouteProgressObserver() {
    @Override public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
      guidanceImageApi.generateGuidanceImage(routeProgress);
    }
  };

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

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
}
