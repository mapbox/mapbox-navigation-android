package testapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.test.espresso.IdlingResource;

import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.lang.reflect.Field;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnNavigationReadyIdlingResource implements IdlingResource, Callback<DirectionsResponse>,
  OnNavigationReadyCallback {

  private static final String NAVIGATION_VIEW = "navigationView";
  private static final String TEST_ROUTE_JSON = "test_route_json";
  private static final int FIRST_ROUTE = 0;
  private boolean isNavigationReady;
  private NavigationView navigationView;
  private DirectionsRoute testRoute;
  private ResourceCallback resourceCallback;
  private SharedPreferences preferences;

  public OnNavigationReadyIdlingResource(Activity activity) {
    try {
      Field field = activity.getClass().getDeclaredField(NAVIGATION_VIEW);
      field.setAccessible(true);
      navigationView = (NavigationView) field.get(activity);
      preferences = PreferenceManager.getDefaultSharedPreferences(activity);
      fetchRoute(activity);
    } catch (Exception err) {
      throw new RuntimeException(err);
    }
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isIdleNow() {
    return isNavigationReady;
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
    this.resourceCallback = resourceCallback;
  }

  public NavigationView getNavigationView() {
    return navigationView;
  }

  @Override
  public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
    testRoute = response.body().routes().get(FIRST_ROUTE);
    storeRouteForRotation(testRoute);
    navigationView.initialize(this);
  }

  @Override
  public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
    throw new RuntimeException(throwable);
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    navigationView.startNavigation(buildTestNavigationViewOptions());
    transitionToIdle();
  }

  private void fetchRoute(Context context) {
    Point origin = Point.fromLngLat(-77.033987, 38.900123);
    Point destination = Point.fromLngLat(-77.044818, 38.848942);
    NavigationRoute.builder(context)
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination)
      .build().getRoute(this);
  }

  private void storeRouteForRotation(DirectionsRoute route) {
    SharedPreferences.Editor editor = preferences.edit();
    String testRouteJson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create().toJson(route);
    editor.putString(TEST_ROUTE_JSON, testRouteJson);
    editor.apply();
  }

  private NavigationViewOptions buildTestNavigationViewOptions() {
    return NavigationViewOptions.builder()
      .directionsRoute(testRoute)
      .shouldSimulateRoute(true)
      .build();
  }

  private void transitionToIdle() {
    isNavigationReady = true;
    if (resourceCallback != null) {
      resourceCallback.onTransitionToIdle();
    }
  }
}
