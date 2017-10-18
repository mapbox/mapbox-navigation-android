package com.mapbox.services.android.navigation.ui.v5.route;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteViewModel extends ViewModel implements Callback<DirectionsResponse> {

  public final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  public final MutableLiveData<Point> destination = new MutableLiveData<>();
  private MutableLiveData<Boolean> isSuccessful = new MutableLiveData<>();
  private Point origin;
  private Location rawLocation;
  private boolean extractLaunchData = true;

  /**
   * A new directions response has been received.
   * <p>
   * The {@link DirectionsResponse} is validated.
   * If navigation is running, {@link MapboxNavigation} is updated and reroute state is dismissed.
   * If not, navigation is started.
   *
   * @param call     used to request the new {@link DirectionsRoute}
   * @param response contains the new {@link DirectionsRoute}
   * @since 0.6.0
   */
  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      route.setValue(response.body().routes().get(0));
      isSuccessful.setValue(true);
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    isSuccessful.setValue(false);
  }

  public void updateRawLocation(@NonNull Location rawLocation) {
    this.rawLocation = rawLocation;
  }

  /**
   * Checks the activity used to launch this activity.
   * Will start navigation based on the data found in the {@link Intent}
   *
   * @param activity holds either a set of {@link Point} coordinates or a {@link DirectionsRoute}
   */
  public void extractLaunchData(Activity activity) {
    if (extractLaunchData) {
      if (launchWithRoute(activity.getIntent())) {
        extractRoute(activity);
      } else {
        extractCoordinates(activity);
      }
    }
  }

  public void fetchRouteNewOrigin(Point newOrigin) {
    if (newOrigin != null && destination.getValue() != null) {
      fetchRoute(newOrigin, destination.getValue());
    }
  }

  /**
   * Requests a new {@link DirectionsRoute}.
   * <p>
   * Will use {@link Location} bearing if we have a rawLocation with bearing.
   *
   * @param origin      start point
   * @param destination end point
   */
  private void fetchRoute(Point origin, Point destination) {
    if (origin != null && destination != null) {
      NavigationRoute.Builder routeBuilder = NavigationRoute.builder()
        .accessToken(Mapbox.getAccessToken())
        .origin(origin)
        .destination(destination);

      if (locationHasBearing()) {
        fetchRouteWithBearing(routeBuilder);
      } else {
        routeBuilder.build().getRoute(this);
      }
    }
  }

  private void fetchRouteFromCoordinates() {
    if (extractLaunchData) {
      fetchRoute(origin, destination.getValue());
      extractLaunchData = false;
    }
  }

  /**
   * Check if the given {@link Intent} has been launched with a {@link DirectionsRoute}.
   *
   * @param intent possibly containing route
   * @return true if route found, false if not
   */
  private boolean launchWithRoute(Intent intent) {
    return intent.getBooleanExtra(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, false);
  }

  /**
   * Extracts the {@link DirectionsRoute}, adds a destination marker,
   * and starts navigation.
   */
  private void extractRoute(Context context) {
    DirectionsRoute route = NavigationLauncher.extractRoute(context);
    if (route != null) {
      RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
      LegStep lastStep = lastLeg.steps().get(lastLeg.steps().size() - 1);
      destination.setValue(lastStep.maneuver().location());
      this.route.setValue(route);
      extractLaunchData = false;
    }
  }

  /**
   * Extracts the {@link Point} coordinates, adds a destination marker,
   * and fetches a route with the coordinates.
   */
  private void extractCoordinates(Context context) {
    HashMap<String, Point> coordinates = NavigationLauncher.extractCoordinates(context);
    if (coordinates.size() > 0) {
      origin = coordinates.get(NavigationConstants.NAVIGATION_VIEW_ORIGIN);
      destination.setValue(coordinates.get(NavigationConstants.NAVIGATION_VIEW_DESTINATION));
      fetchRouteFromCoordinates();
    }
  }

  /**
   * Checks if we have at least one {@link DirectionsRoute} in the given
   * {@link DirectionsResponse}.
   *
   * @param response to be checked
   * @return true if valid, false if not
   */
  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && response.body().routes() != null
      && response.body().routes().size() > 0;
  }

  /**
   * Used to determine if a rawLocation has a bearing.
   *
   * @return true if bearing exists, false if not
   */
  private boolean locationHasBearing() {
    return rawLocation != null && rawLocation.hasBearing();
  }

  /**
   * Will finish building {@link NavigationRoute} after adding a bearing
   * and request the route.
   *
   * @param routeBuilder to fetch the route
   */
  private void fetchRouteWithBearing(NavigationRoute.Builder routeBuilder) {
    routeBuilder.addBearing(Float.valueOf(rawLocation.getBearing()).doubleValue(), 90d);
    routeBuilder.build().getRoute(this);
  }
}
