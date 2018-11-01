package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.navigator.RouterResult;

import java.util.List;

import okhttp3.HttpUrl;
import timber.log.Timber;

/**
 * The {@link OfflineRoute} class wraps the {@link NavigationRoute} class with parameters which
 * could be set in order for an offline navigation session to successfully begin.
 */
public class OfflineRoute {

  private static final String BICYCLE_TYPE_QUERY_PARAMETER = "bicycle_type";
  private static final String CYCLING_SPEED_QUERY_PARAMETER = "cycling_speed";
  private static final String USE_ROADS_QUERY_PARAMETER = "use_roads";
  private static final String USE_HILLS_QUERY_PARAMETER = "use_hills";
  private static final String USE_FERRY_QUERY_PARAMETER = "use_ferry";
  private static final String AVOID_BAD_SURFACES_QUERY_PARAMETER = "avoid_bad_surfaces";
  private static final String WAYPOINT_TYPES_QUERY_PARAMETER = "waypoint_types";
  private final NavigationRoute onlineRoute;
  private final String bicycleType;
  private final Float cyclingSpeed;
  private final Float useRoads;
  private final Float useHills;
  private final Float useFerry;
  private final Float avoidBadSurfaces;
  private final String waypointTypes;

  private OfflineRoute(NavigationRoute onlineRoute, String bicycleType, Float cyclingSpeed, Float useRoads,
                       Float useHills, Float useFerry, Float avoidBadSurfaces, List<String> waypointTypes) {
    this.onlineRoute = onlineRoute;
    this.bicycleType = checkBicycleType(bicycleType);
    this.cyclingSpeed = cyclingSpeed;
    this.useRoads = useRoads;
    this.useHills = useHills;
    this.useFerry = useFerry;
    this.avoidBadSurfaces = avoidBadSurfaces;
    this.waypointTypes = checkWaypointTypes(waypointTypes);
  }

  /**
   * Build a new {@link OfflineRoute} object with the proper offline navigation parameters already setup.
   *
   * @return a {@link Builder} object for creating this object
   */
  public static Builder builder(NavigationRoute.Builder onlineRouteBuilder) {
    return new Builder(onlineRouteBuilder);
  }

  /**
   * Builds a URL string for offline.
   *
   * @return the offline url string
   */
  public String buildUrl() {
    String onlineUrl = onlineRoute.getCall().request().url().toString();
    String offlineUrl = buildOfflineUrl(onlineUrl);
    return offlineUrl;
  }

  @Nullable
  DirectionsRoute retrieveOfflineRoute(@NonNull RouterResult response) {
    boolean success = response.getSuccess();
    String jsonResponse = response.getJson();
    if (checkOfflineRoute(success, jsonResponse)) {
      return null;
    }
    return obtainRouteFor(jsonResponse);
  }

  private String checkBicycleType(String bicycleType) {
    if (isBicycleType(bicycleType)) {
      return bicycleType;
    }
    return null;
  }

  private boolean isBicycleType(String type) {
    if (TextUtils.isEmpty(type)) {
      return false;
    }
    if (!type.equals(OfflineCriteria.ROAD) && !type.equals(OfflineCriteria.HYBRID)
      && !type.equals(OfflineCriteria.CITY) && !type.equals(OfflineCriteria.CROSS)
      && !type.equals(OfflineCriteria.MOUNTAIN)) {
      throw new ServicesException("Bicycle type value must be one of Road, Hybrid, City, Cross or Mountain");
    }
    return true;
  }

  private String checkWaypointTypes(List<String> waypointTypes) {
    if (waypointTypes == null || waypointTypes.isEmpty()) {
      return null;
    }
    String formattedWaypointTypes = formatWaypointTypes(waypointTypes);
    if (formattedWaypointTypes == null) {
      throw new ServicesException("All waypoint types values must be one of break, through or null");
    }
    return formattedWaypointTypes;
  }

  private String formatWaypointTypes(List<String> waypointTypesToFormat) {
    String[] waypointTypes = waypointTypesToFormat.toArray(new String[0]);
    for (int i = 0; i < waypointTypes.length; i++) {
      if (waypointTypes[i] == null) {
        waypointTypes[i] = "";
      } else if (!waypointTypes[i].equals(OfflineCriteria.BREAK)
        && !waypointTypes[i].equals(OfflineCriteria.THROUGH) && !waypointTypes[i].isEmpty()) {
        return null;
      }
    }
    return TextUtils.join(";", waypointTypes);
  }

  private String buildOfflineUrl(String onlineUrl) {
    HttpUrl.Builder offlineUrlBuilder = HttpUrl.get(onlineUrl).newBuilder();
    if (bicycleType != null) {
      offlineUrlBuilder.addQueryParameter(BICYCLE_TYPE_QUERY_PARAMETER, bicycleType);
    }

    if (cyclingSpeed != null) {
      offlineUrlBuilder.addQueryParameter(CYCLING_SPEED_QUERY_PARAMETER, cyclingSpeed.toString());
    }

    if (useRoads != null) {
      offlineUrlBuilder.addQueryParameter(USE_ROADS_QUERY_PARAMETER, useRoads.toString());
    }

    if (useHills != null) {
      offlineUrlBuilder.addQueryParameter(USE_HILLS_QUERY_PARAMETER, useHills.toString());
    }

    if (useFerry != null) {
      offlineUrlBuilder.addQueryParameter(USE_FERRY_QUERY_PARAMETER, useFerry.toString());
    }

    if (avoidBadSurfaces != null) {
      offlineUrlBuilder.addQueryParameter(AVOID_BAD_SURFACES_QUERY_PARAMETER, avoidBadSurfaces.toString());
    }

    if (waypointTypes != null) {
      offlineUrlBuilder.addQueryParameter(WAYPOINT_TYPES_QUERY_PARAMETER, waypointTypes);
    }
    return offlineUrlBuilder.build().toString();
  }

  private boolean checkOfflineRoute(boolean isSuccess, String json) {
    if (!isSuccess) {
      Gson gson = new Gson();
      OfflineError error = gson.fromJson(json, OfflineError.class);
      Timber.e("Error occurred fetching offline route: %s - Code: %d", error.getError(), error.getErrorCode());
      return true;
    }
    return false;
  }

  private DirectionsRoute obtainRouteFor(String response) {
    DirectionsRoute route = DirectionsResponse.fromJson(response).routes().get(0);
    return route;
  }

  public static final class Builder {
    private final NavigationRoute.Builder navigationRouteBuilder;
    private String bicycleType;
    private Float cyclingSpeed;
    private Float useRoads;
    private Float useHills;
    private Float useFerry;
    private Float avoidBadSurfaces;
    private List<String> waypointTypes;

    private Builder(NavigationRoute.Builder onlineRouteBuilder) {
      this.navigationRouteBuilder = onlineRouteBuilder;
    }

    /**
     * The type of bicycle, either <tt>Road</tt>, <tt>Hybrid</tt>, <tt>City</tt>, <tt>Cross</tt>, <tt>Mountain</tt>.
     * The default type is <tt>Hybrid</tt>.
     *
     * @param bicycleType the type of bicycle
     * @return this builder for chaining options together
     */
    public Builder bicycleType(@Nullable @OfflineCriteria.BicycleType String bicycleType) {
      this.bicycleType = bicycleType;
      return this;
    }

    /**
     * Cycling speed is the average travel speed along smooth, flat roads. This is meant to be the
     * speed a rider can comfortably maintain over the desired distance of the route. It can be
     * modified (in the costing method) by surface type in conjunction with bicycle type and
     * (coming soon) by hilliness of the road section. When no speed is specifically provided, the
     * default speed is determined by the bicycle type and are as follows: Road = 25 KPH (15.5 MPH),
     * Cross = 20 KPH (13 MPH), Hybrid/City = 18 KPH (11.5 MPH), and Mountain = 16 KPH (10 MPH).
     *
     * @param cyclingSpeed in kmh
     * @return this builder for chaining options together
     */
    public Builder cyclingSpeed(@Nullable @FloatRange(from = 5, to = 60) Float cyclingSpeed) {
      this.cyclingSpeed = cyclingSpeed;
      return this;
    }

    /**
     * A cyclist's propensity to use roads alongside other vehicles. This is a range of values from 0
     * to 1, where 0 attempts to avoid roads and stay on cycleways and paths, and 1 indicates the
     * rider is more comfortable riding on roads. Based on the use_roads factor, roads with certain
     * classifications and higher speeds are penalized in an attempt to avoid them when finding the
     * best path. The default value is 0.5.
     *
     * @param useRoads a cyclist's propensity to use roads alongside other vehicles
     * @return this builder for chaining options together
     */
    public Builder useRoads(@Nullable @FloatRange(from = 0.0, to = 1.0) Float useRoads) {
      this.useRoads = useRoads;
      return this;
    }

    /**
     * A cyclist's desire to tackle hills in their routes. This is a range of values from 0 to 1,
     * where 0 attempts to avoid hills and steep grades even if it means a longer (time and
     * distance) path, while 1 indicates the rider does not fear hills and steeper grades. Based on
     * the use_hills factor, penalties are applied to roads based on elevation change and grade.
     * These penalties help the path avoid hilly roads in favor of flatter roads or less steep
     * grades where available. Note that it is not always possible to find alternate paths to avoid
     * hills (for example when route locations are in mountainous areas). The default value is 0.5.
     *
     * @param useHills a cyclist's desire to tackle hills in their routes
     * @return this builder for chaining options together
     */
    public Builder useHills(@Nullable @FloatRange(from = 0.0, to = 1.0) Float useHills) {
      this.useHills = useHills;
      return this;
    }

    /**
     * This value indicates the willingness to take ferries. This is a range of values between 0 and 1.
     * Values near 0 attempt to avoid ferries and values near 1 will favor ferries. Note that
     * sometimes ferries are required to complete a route so values of 0 are not guaranteed to avoid
     * ferries entirely. The default value is 0.5.
     *
     * @param useFerry the willingness to take ferries
     * @return this builder for chaining options together
     */
    public Builder useFerry(@Nullable @FloatRange(from = 0.0, to = 1.0) Float useFerry) {
      this.useFerry = useFerry;
      return this;
    }

    /**
     * This value is meant to represent how much a cyclist wants to avoid roads with poor surfaces
     * relative to the bicycle type being used. This is a range of values between 0 and 1. When the
     * value is 0, there is no penalization of roads with different surface types; only bicycle
     * speed on each surface is taken into account. As the value approaches 1, roads with poor
     * surfaces for the bike are penalized heavier so that they are only taken if they
     * significantly improve travel time. When the value is equal to 1, all bad surfaces are
     * completely disallowed from routing, including start and end points. The default value is 0.25.
     *
     * @param avoidBadSurfaces how much a cyclist wants to avoid roads with poor surfaces
     * @return this builder for chaining options together
     */
    public Builder avoidBadSurfaces(@Nullable @FloatRange(from = 0.0, to = 1.0) Float avoidBadSurfaces) {
      this.avoidBadSurfaces = avoidBadSurfaces;
      return this;
    }

    /**
     * The same waypoint types the user originally made when the request was made.
     *
     * @param waypointTypes break, through or omitted null
     * @return this builder for chaining options together
     */
    public Builder waypointTypes(@OfflineCriteria.WaypointType @Nullable List<String> waypointTypes) {
      this.waypointTypes = waypointTypes;
      return this;
    }

    /**
     * This uses the provided parameters set using the {@link Builder} and adds the required
     * settings for offline navigation to work correctly.
     *
     * @return a new instance of {@link OfflineRoute}
     */
    public OfflineRoute build() {
      return new OfflineRoute(navigationRouteBuilder.build(), bicycleType, cyclingSpeed, useRoads, useHills,
        useFerry, avoidBadSurfaces, waypointTypes);
    }
  }
}
