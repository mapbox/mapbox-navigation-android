package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gesture.GesturePluginImpl;
import com.mapbox.maps.plugin.gesture.OnMapLongClickListener;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationComponentPlugin;
import com.mapbox.maps.plugin.location.modes.CameraMode;
import com.mapbox.maps.plugin.location.modes.RenderMode;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.examples.LocationPermissionsHelper;
import com.mapbox.navigation.ui.route.NavigationMapRoute;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.mapbox.navigation.examples.LocationPermissionsHelperKt.LOCATION_PERMISSIONS_REQUEST_CODE;

public class NavigationMapRouteActivity extends AppCompatActivity implements PermissionsListener,
  OnMapLongClickListener {

  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private LocationPermissionsHelper permissionsHelper = new LocationPermissionsHelper(this);
  private MapView mapView;
  private MapboxMap mapboxMap;
  private NavigationMapRoute navigationMapRoute;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private MapboxNavigation mapboxNavigation;
  private Button startNavigation;
  private ProgressBar routeLoading;
  private DirectionsRoute activeRoute;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_map_route);
    mapView = findViewById(R.id.mapView);
    startNavigation = findViewById(R.id.startNavigation);
    routeLoading = findViewById(R.id.routeLoadingProgressBar);
    mapboxMap = mapView.getMapboxMap();

    if (LocationPermissionsHelper.areLocationPermissionsGranted(this)) {
      requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE);
    } else {
      permissionsHelper.requestLocationPermissions(this);
    }
  }

  @SuppressLint("MissingPermission")
  public void initStyle() {
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
      initializeLocationComponent(style);
      NavigationOptions navigationOptions = MapboxNavigation
        .defaultNavigationOptionsBuilder(NavigationMapRouteActivity.this, getMapboxAccessTokenFromResources())
        .locationEngine(new ReplayLocationEngine(mapboxReplayer))
        .build();
      mapboxNavigation = new MapboxNavigation(navigationOptions);
      mapboxNavigation.registerLocationObserver(locationObserver);
      mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);

      mapboxReplayer.pushRealLocation(this, 0.0);
      mapboxReplayer.play();

      navigationMapRoute = new NavigationMapRoute.Builder(mapView, mapboxMap, this)
        .withVanishRouteLineEnabled(true)
        .withMapboxNavigation(mapboxNavigation)
        .build();

      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);
      getGesturePlugin().addOnMapLongClickListener(this);

      //fixme temporary
//      CameraOptions cameraOptions = new CameraOptions.Builder().center(Point.fromLngLat(-122.396817, 37.788009)).zoom(13.0).build();
//      mapboxMap.jumpTo(cameraOptions);
//      navigationMapRoute.addRoute(getRoute());
      //fixme end temporary

    }, (mapLoadError, s) -> Timber.e("Error loading map: " + mapLoadError.name()));
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
  }

  @Override
  public boolean onMapLongClick(@NotNull Point point) {
    vibrate();
    hideRoute();

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
      .alternatives(false)
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
        activeRoute = routes.get(0);
        navigationMapRoute.addRoutes(routes);
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
    startNavigation.setVisibility(View.GONE);
  }

  @SuppressWarnings("MissingPermission")
  private void initializeLocationComponent(Style style) {
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
      .useDefaultLocationEngine(false) //SBNOTE: I think this should be false eventually
      .build();
    LocationComponentPlugin locationComponent = getLocationComponent();
    locationComponent.activateLocationComponent(activationOptions);
    locationComponent.setEnabled(true);
    locationComponent.setRenderMode(RenderMode.COMPASS);
    locationComponent.setCameraMode(CameraMode.TRACKING, 0, 16.0, null, 45.0, null);
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
    getLocationComponent().forceLocationUpdate(locations, false);
  }

  private LocationComponentPlugin getLocationComponent() {
    return mapView.getPlugin(LocationComponentPlugin.class);
  }

  private GesturePluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturePluginImpl.class);
  }

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<NavigationMapRouteActivity> activityRef;

    MyLocationEngineCallback(NavigationMapRouteActivity activity) {
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

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
      permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    } else if (grantResults.length > 0) {
      initStyle();
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
      initStyle();
    }
  }

  private DirectionsRoute getRoute() {
    String routeAsJson = "{\"routeIndex\":\"0\",\"distance\":2884.1,\"duration\":502.0,\"geometry\":\"iwkagAxeomhF|AwBbe@xn@`Ud[rJtMfClDhCnDzFbIjRvWzB|ClBhCrLzOxAnBhB`ChUnZBBpWp^fClDwDfFm]le@_NrQ}HjKeKfNqCvDkCpDoIjLiJhM}TzZqNnQwAtA_An@gA^aALyBRoId@k@Bk@F}@F_Ef@gBVoDh@gCZaVrCcAJa\\\\xD_[rDoFn@gLtAoH|@c[rDe]`EyNdBwR|B_VvBsDd@oYjD_BPeQvBmD^YD[BqDd@oH|@uh@nGqZpDeXbDoANuAZoIbAmJhAsMrAeJrAuBNyCT_k@|GuBVmBTkBTiGt@kRzB{IdAwKjBgCXcPnBoFn@qVvCeDMUkDcBmWqAaS}AuUsAiSImAW}DUiD_A}NeAmPg@kIiDmh@QkC]eFqEh@mb@`FwKpAsC\\\\iD`@uGt@sf@|FmCZaD`@sv@bJuXbDcYdDmBT_JdAmi@hGaD^XzEp@tK\",\"weight\":892.3,\"weight_name\":\"routability\",\"legs\":[{\"distance\":2884.1,\"duration\":502.0,\"summary\":\"Howard Street, Kearny Street\",\"steps\":[{\"distance\":7.4,\"duration\":5.4,\"geometry\":\"iwkagAxeomhF|AwB\",\"name\":\"\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.396781,37.788037],\"bearing_before\":0.0,\"bearing_after\":135.0,\"instruction\":\"Head southeast\",\"type\":\"depart\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":7.4,\"announcement\":\"Head southeast, then turn right onto Howard Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead southeast, then turn right onto Howard Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":7.4,\"primary\":{\"text\":\"Howard Street\",\"components\":[{\"text\":\"Howard Street\",\"type\":\"text\",\"abbr\":\"Howard St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":28.1,\"intersections\":[{\"location\":[-122.396781,37.788037],\"bearings\":[135],\"classes\":[\"restricted\"],\"entry\":[true],\"out\":0}]},{\"distance\":467.5,\"duration\":75.6,\"geometry\":\"ktkagA`bomhFbe@xn@`Ud[rJtMfClDhCnDzFbIjRvWzB|ClBhCrLzOxAnBhB`ChUnZBBpWp^fClD\",\"name\":\"Howard Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.396721,37.78799],\"bearing_before\":133.0,\"bearing_after\":223.0,\"instruction\":\"Turn right onto Howard Street\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":447.5,\"announcement\":\"In a quarter mile, turn right onto 3rd Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, turn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":92.8,\"announcement\":\"Turn right onto 3rd Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":467.5,\"primary\":{\"text\":\"3rd Street\",\"components\":[{\"text\":\"3rd Street\",\"type\":\"text\",\"abbr\":\"3rd St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":136.3,\"intersections\":[{\"location\":[-122.396721,37.78799],\"bearings\":[45,225,315],\"entry\":[false,true,false],\"in\":2,\"out\":1},{\"location\":[-122.398259,37.786773],\"bearings\":[45,135,225,315],\"entry\":[false,true,true,true],\"in\":0,\"out\":2},{\"location\":[-122.398984,37.786206],\"bearings\":[45,225,315],\"entry\":[false,true,false],\"in\":0,\"out\":1},{\"location\":[-122.399379,37.785888],\"bearings\":[45,135,225],\"entry\":[false,true,true],\"in\":0,\"out\":2}]},{\"distance\":1565.8999999999999,\"duration\":278.40000000000003,\"geometry\":\"mzeagAzlvmhFwDfFm]le@_NrQ}HjKeKfNqCvDkCpDoIjLiJhM}TzZqNnQwAtA_An@gA^aALyBRoId@k@Bk@F}@F_Ef@gBVoDh@gCZaVrCcAJa\\\\xD_[rDoFn@gLtAoH|@c[rDe]`EyNdBwR|B_VvBsDd@oYjD_BPeQvBmD^YD[BqDd@oH|@uh@nGqZpDeXbDoANuAZoIbAmJhAsMrAeJrAuBNyCT_k@|GuBVmBTkBTiGt@kRzB{IdAwKjBgCXcPnBoFn@qVvCeDM\",\"name\":\"3rd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.400478,37.785015],\"bearing_before\":225.0,\"bearing_after\":315.0,\"instruction\":\"Turn right onto 3rd Street\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":1545.8999999999999,\"announcement\":\"Continue on 3rd Street for 1 mile\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eContinue on \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003e3rd\\u003c/say-as\\u003e Street for 1 mile\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":393.7,\"announcement\":\"In a quarter mile, turn right onto Broadway\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, turn right onto Broadway\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":84.4,\"announcement\":\"Turn right onto Broadway\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Broadway\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1565.8999999999999,\"primary\":{\"text\":\"Broadway\",\"components\":[{\"text\":\"Broadway\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":84.4,\"primary\":{\"text\":\"Broadway\",\"components\":[{\"text\":\"Broadway\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"right\"},\"sub\":{\"text\":\"\",\"components\":[{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"left\"],\"active\":false},{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"right\"],\"active\":true}]}}],\"driving_side\":\"right\",\"weight\":484.20000000000005,\"intersections\":[{\"location\":[-122.400478,37.785015],\"bearings\":[45,135,225,315],\"entry\":[false,false,true,true],\"in\":0,\"out\":3},{\"location\":[-122.402041,37.786261],\"bearings\":[45,135,225,315],\"entry\":[true,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.403436,37.787676],\"bearings\":[180,345],\"entry\":[false,true],\"in\":0,\"out\":1},{\"location\":[-122.403497,37.787965],\"bearings\":[165,255,345],\"entry\":[false,true,true],\"in\":0,\"out\":2},{\"location\":[-122.403684,37.788901],\"bearings\":[75,165,255,345],\"entry\":[true,false,false,true],\"in\":1,\"out\":3},{\"location\":[-122.403872,37.789833],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.404059,37.790766],\"bearings\":[75,165,255,345],\"entry\":[true,false,false,true],\"in\":1,\"out\":3},{\"location\":[-122.404233,37.791703],\"bearings\":[75,180,255,345],\"entry\":[false,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.404426,37.792656],\"bearings\":[75,165,255,345],\"entry\":[true,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.404614,37.793578],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.404793,37.794462],\"bearings\":[75,165,255,345],\"entry\":[true,false,false,true],\"in\":1,\"out\":3},{\"location\":[-122.404878,37.794856],\"bearings\":[75,165,345],\"entry\":[true,false,true],\"in\":1,\"out\":2},{\"location\":[-122.40497,37.795328],\"bearings\":[75,165,255,345],\"entry\":[false,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.405147,37.796223],\"bearings\":[75,165,255,345],\"entry\":[true,false,false,true],\"in\":1,\"out\":3},{\"location\":[-122.405282,37.796894],\"bearings\":[135,165,315,345],\"entry\":[true,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.405336,37.797098],\"bearings\":[75,165,255,345],\"entry\":[false,false,false,true],\"in\":1,\"out\":3}]},{\"distance\":289.6,\"duration\":57.8,\"geometry\":\"gg_bgArf`nhFUkDcBmWqAaS}AuUsAiSImAW}DUiD_A}NeAmPg@kIiDmh@QkC]eF\",\"name\":\"Broadway\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.405498,37.79802],\"bearing_before\":350.0,\"bearing_after\":80.0,\"instruction\":\"Turn right onto Broadway\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":269.6,\"announcement\":\"In 900 feet, turn left onto Sansome Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn 900 feet, turn left onto Sansome Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":75.2,\"announcement\":\"Turn left onto Sansome Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Sansome Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":289.6,\"primary\":{\"text\":\"Sansome Street\",\"components\":[{\"text\":\"Sansome Street\",\"type\":\"text\",\"abbr\":\"Sansome St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":114.2,\"intersections\":[{\"location\":[-122.405498,37.79802],\"bearings\":[0,75,165,255],\"entry\":[false,true,false,true],\"in\":2,\"out\":1,\"lanes\":[{\"valid\":false,\"indications\":[\"left\"]},{\"valid\":true,\"indications\":[\"right\"]}]},{\"location\":[-122.403878,37.798228],\"bearings\":[75,165,255,345],\"entry\":[true,true,false,true],\"in\":2,\"out\":0}]},{\"distance\":525.8,\"duration\":64.8,\"geometry\":\"ea`bgAh{ymhFqEh@mb@`FwKpAsC\\\\iD`@uGt@sf@|FmCZaD`@sv@bJuXbDcYdDmBT_JdAmi@hGaD^\",\"name\":\"Sansome Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.402245,37.798435],\"bearing_before\":80.0,\"bearing_after\":350.0,\"instruction\":\"Turn left onto Sansome Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":505.79999999999995,\"announcement\":\"In a quarter mile, turn left onto Greenwich Street\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, turn left onto Greenwich Street\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":121.7,\"announcement\":\"Turn left onto Greenwich Street, then you will arrive at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Greenwich Street, then you will arrive at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":525.8,\"primary\":{\"text\":\"Greenwich Street\",\"components\":[{\"text\":\"Greenwich Street\",\"type\":\"text\",\"abbr\":\"Greenwich St\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}}],\"driving_side\":\"right\",\"weight\":109.5,\"intersections\":[{\"location\":[-122.402245,37.798435],\"bearings\":[75,165,255,345],\"entry\":[true,true,false,true],\"in\":2,\"out\":3},{\"location\":[-122.402435,37.799385],\"bearings\":[75,165,255,345],\"entry\":[true,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.40262,37.800314],\"bearings\":[75,165,255,345],\"entry\":[true,false,true,true],\"in\":1,\"out\":3},{\"location\":[-122.402815,37.801285],\"bearings\":[75,165,345],\"entry\":[true,false,true],\"in\":1,\"out\":2},{\"location\":[-122.402991,37.802169],\"bearings\":[165,255,345],\"entry\":[false,true,true],\"in\":0,\"out\":2}]},{\"distance\":27.8,\"duration\":20.0,\"geometry\":\"aeibgAlu{mhFXzEp@tK\",\"name\":\"Greenwich Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.403175,37.803105],\"bearing_before\":350.0,\"bearing_after\":260.0,\"instruction\":\"Turn left onto Greenwich Street\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":7.0,\"announcement\":\"You have arrived at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":27.8,\"primary\":{\"text\":\"You will arrive\",\"components\":[{\"text\":\"You will arrive\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"straight\"}},{\"distanceAlongGeometry\":7.0,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"straight\"}}],\"driving_side\":\"right\",\"weight\":20.0,\"intersections\":[{\"location\":[-122.403175,37.803105],\"bearings\":[75,165,255,345],\"entry\":[true,false,true,true],\"in\":1,\"out\":2}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"ubibgA~h|mhF\",\"name\":\"Greenwich Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.403488,37.803067],\"bearing_before\":261.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination\",\"type\":\"arrive\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.403488,37.803067],\"bearings\":[81],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[7.425862327122799,95.52546288437826,55.79585983281602,29.235309169712288,10.755850560179406,10.896580808732383,19.97967259618138,48.99556600507754,9.786643274890613,8.614560317727404,33.929663806559766,7.02021482140906,8.209576316581595,55.43145661097636,0.283536799307206,62.30023535774677,10.755979904520633,14.44597299929199,76.5286676763532,37.40012636787177,24.812978236605296,30.503191536282845,11.459778271633695,11.03736589809851,26.514590403072486,28.469188138310233,55.32738694103918,38.000124789328176,6.18363280790793,4.1374777615485385,4.2439603328353135,3.7216847386479657,6.84150765230015,18.76050555783645,2.453285632444654,2.4721101722571404,3.4658958230385424,10.821479115584705,5.87916513105225,9.9604540665592,7.662847422384856,41.5547475928779,3.8182928761703248,52.36226788538483,50.45344175501242,13.512840023715219,23.88097293019762,17.124574636341766,50.67313924572979,54.39466095296195,28.495066999113774,35.58105120966958,41.26962762846207,10.148715052651015,47.76190651512682,5.397149552389907,32.79367521992095,9.778344469138418,1.469788211253166,1.5670592676664927,10.039018048146442,17.124556575151683,75.14478760592077,49.670640346982445,45.39992058415493,4.50427253121985,4.938485154882802,18.923463756640956,20.612562045152366,26.287435233745036,20.2488351986211,6.599913310225065,8.618822006430658,79.30557619773968,6.64656492052528,6.193371147131688,6.083533037118657,14.982224938865762,34.908087669740965,19.59632184461276,23.18120949049012,7.649202140488827,30.870876841156097,13.512803962547595,42.460985828335374,9.252259718357045,7.656783326963846,34.811433778352146,28.578327670549214,32.328884100421185,28.943140691089038,3.4724604930994065,8.455382342259396,7.570016524607846,22.692331863718533,24.82786455086968,14.758029969333695,59.031707693633756,6.233062182991279,10.24388392381897,11.823699616877656,63.842492731138165,22.97450409908747,8.33565252063844,9.571563387665867,15.641499986608387,71.39533628308173,7.992343533701505,9.132372814054392,100.21985230766296,46.27855232279133,47.06133023464177,6.1933590001844605,19.8160121233501,76.42179588333079,9.118398991225975,9.774596107655096,18.055521690936175],\"congestion\":[\"unknown\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"heavy\",\"low\",\"low\",\"moderate\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"heavy\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"moderate\",\"moderate\",\"moderate\",\"low\",\"low\",\"low\",\"low\",\"moderate\",\"moderate\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"heavy\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"low\",\"unknown\",\"unknown\"]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":[[-122.396817,37.788009],[-122.403495,37.803101]],\"alternatives\":true,\"language\":\"en\",\"continue_straight\":false,\"roundabout_exits\":false,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion,distance\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"access_token\":\"pk.eyJ1Ijoic2V0aC1ib3VyZ2V0IiwiYSI6ImNrYjAwNnk5ODAzYnEycnBvMTgzajdhanUifQ.vfwMqIW8sThk0s58JyvaJg\",\"uuid\":\"ckf4awwqy0m52j8qsiba9rf3v\"},\"voiceLocale\":\"en-US\"}";
    return DirectionsRoute.fromJson(routeAsJson);
  }
}
