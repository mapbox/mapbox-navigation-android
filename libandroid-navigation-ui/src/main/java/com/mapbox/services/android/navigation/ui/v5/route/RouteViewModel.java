package com.mapbox.services.android.navigation.ui.v5.route;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.mapbox.directions.v5.DirectionsCriteria;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteViewModel extends AndroidViewModel implements Callback<DirectionsResponse> {

  public final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  public final MutableLiveData<Point> destination = new MutableLiveData<>();
  private MutableLiveData<Boolean> isSuccessful = new MutableLiveData<>();
  private Point origin;
  private Location rawLocation;
  private boolean extractLaunchData = true;
  private String unitType;

  public RouteViewModel(@NonNull Application application) {
    super(application);
    initUnitType(PreferenceManager.getDefaultSharedPreferences(application));
  }

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
   * Checks the options used to launch this {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
   * <p>
   * Will launch with either a {@link DirectionsRoute} or pair of {@link Point}s
   *
   * @param options holds either a set of {@link Point} coordinates or a {@link DirectionsRoute}
   */
  public void extractLaunchData(NavigationViewOptions options) {
    if (extractLaunchData) {
      if (launchWithRoute(options)) {
        extractRoute(options);
      } else {
        extractCoordinates(options);
      }
    }
  }

  /**
   * Called when an off-route event is fired and a new {@link DirectionsRoute}
   * is needed to continue navigating.
   *
   * @param newOrigin found from off-route event
   */
  public void fetchRouteNewOrigin(Point newOrigin) {
    if (newOrigin != null && destination.getValue() != null) {
      fetchRoute(newOrigin, destination.getValue());
    }
  }

  /**
   * Initializes distance unit (imperial or metric).
   */
  private void initUnitType(SharedPreferences preferences) {
    int unitType = preferences.getInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE,
      NavigationUnitType.TYPE_IMPERIAL);
    boolean isImperialUnitType = unitType == NavigationUnitType.TYPE_IMPERIAL;
    this.unitType = isImperialUnitType ? DirectionsCriteria.IMPERIAL : DirectionsCriteria.METRIC;
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

      Double bearing = null;
      if (rawLocation != null) {
        bearing = rawLocation.hasBearing() ? Float.valueOf(rawLocation.getBearing()).doubleValue() : null;
      }

      NavigationRoute.builder()
        .accessToken(Mapbox.getAccessToken())
        .origin(origin, bearing, 90d)
        .voiceUnits(unitType)
        .destination(destination).build().getRoute(this);
    }
  }

  private void fetchRouteFromCoordinates() {
    if (extractLaunchData) {
      fetchRoute(origin, destination.getValue());
      extractLaunchData = false;
    }
  }

  /**
   * Check if the given {@link NavigationViewOptions} has been launched with a {@link DirectionsRoute}.
   *
   * @param options possibly containing route
   * @return true if route found, false if not
   */
  private boolean launchWithRoute(NavigationViewOptions options) {
    return options.directionsRoute() != null;
  }

  /**
   * Extracts the {@link DirectionsRoute}, adds a destination marker,
   * and starts navigation.
   *
   * @param options containing route
   */
  private void extractRoute(NavigationViewOptions options) {
    DirectionsRoute route = options.directionsRoute();
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
   *
   * @param options containing origin and destination
   */
  private void extractCoordinates(NavigationViewOptions options) {
    if (options.origin() != null && options.destination() != null) {
      origin = options.origin();
      destination.setValue(options.destination());
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
}
