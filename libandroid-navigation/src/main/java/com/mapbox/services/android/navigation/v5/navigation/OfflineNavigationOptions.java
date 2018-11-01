package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@AutoValue
public abstract class OfflineNavigationOptions {

  /**
   * The type of bicycle, either <tt>Road</tt>, <tt>Hybrid</tt>, <tt>City</tt>, <tt>Cross</tt>, <tt>Mountain</tt>.
   * The default type is <tt>Hybrid</tt>.
   *
   * @return the type of bicycle
   */
  @SerializedName("bicycle_type")
  @OfflineCriteria.BicycleType
  @Nullable
  public abstract String bicycleType();

  /**
   * Cycling speed is the average travel speed along smooth, flat roads. This is meant to be the
   * speed a rider can comfortably maintain over the desired distance of the route. It can be
   * modified (in the costing method) by surface type in conjunction with bicycle type and
   * (coming soon) by hilliness of the road section. When no speed is specifically provided, the
   * default speed is determined by the bicycle type and are as follows: Road = 25 KPH (15.5 MPH),
   * Cross = 20 KPH (13 MPH), Hybrid/City = 18 KPH (11.5 MPH), and Mountain = 16 KPH (10 MPH).
   *
   * @return the cycling speed in kmh
   */
  @SerializedName("cycling_speed")
  @Nullable
  public abstract Float cyclingSpeed();

  /**
   * A cyclist's propensity to use roads alongside other vehicles. This is a range of values from 0
   * to 1, where 0 attempts to avoid roads and stay on cycleways and paths, and 1 indicates the
   * rider is more comfortable riding on roads. Based on the use_roads factor, roads with certain
   * classifications and higher speeds are penalized in an attempt to avoid them when finding the
   * best path. The default value is 0.5.
   *
   * @return a cyclist's propensity to use roads alongside other vehicles
   */
  @SerializedName("use_roads")
  @Nullable
  public abstract Float useRoads();

  /**
   * A cyclist's desire to tackle hills in their routes. This is a range of values from 0 to 1,
   * where 0 attempts to avoid hills and steep grades even if it means a longer (time and
   * distance) path, while 1 indicates the rider does not fear hills and steeper grades. Based on
   * the use_hills factor, penalties are applied to roads based on elevation change and grade.
   * These penalties help the path avoid hilly roads in favor of flatter roads or less steep
   * grades where available. Note that it is not always possible to find alternate paths to avoid
   * hills (for example when route locations are in mountainous areas). The default value is 0.5.
   *
   * @return a cyclist's desire to tackle hills in their routes
   */
  @SerializedName("use_hills")
  @Nullable
  public abstract Float useHills();

  /**
   * This value indicates the willingness to take ferries. This is a range of values between 0 and 1.
   * Values near 0 attempt to avoid ferries and values near 1 will favor ferries. Note that
   * sometimes ferries are required to complete a route so values of 0 are not guaranteed to avoid
   * ferries entirely. The default value is 0.5.
   *
   * @return the willingness to take ferries
   */
  @SerializedName("use_ferry")
  @Nullable
  public abstract Float useFerry();

  /**
   * This value is meant to represent how much a cyclist wants to avoid roads with poor surfaces
   * relative to the bicycle type being used. This is a range of values between 0 and 1. When the
   * value is 0, there is no penalization of roads with different surface types; only bicycle
   * speed on each surface is taken into account. As the value approaches 1, roads with poor
   * surfaces for the bike are penalized heavier so that they are only taken if they
   * significantly improve travel time. When the value is equal to 1, all bad surfaces are
   * completely disallowed from routing, including start and end points. The default value is 0.25.
   *
   * @return how much a cyclist wants to avoid roads with poor surfaces
   */
  @SerializedName("avoid_bad_surfaces")
  @Nullable
  public abstract Float avoidBadSurfaces();

  /**
   * Type of location, accepts  <tt>break</tt> (default), <tt>through</tt> or null.
   * A <tt>break</tt> is a stop, so the first and last locations must be of type <tt>break</tt>.
   * A <tt>through</tt> location is one that the route path travels through, and is useful to
   * force a route to go through location.The path is not allowed to reverse direction at the through locations.
   * If no type is provided, the type is assumed to be a <tt>break</tt>.
   * If provided, the list of waypoint types must be the same length as the list of waypoints.
   * However, you can skip a coordinate and show its position in the list with the <tt>;</tt> separator.
   *
   * @return a string representing waypoint types for each waypoint
   */
  @SerializedName("waypoint_types")
  @OfflineCriteria.WaypointType
  @Nullable
  public abstract List<String> waypointTypes();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * The type of bicycle, either <tt>Road</tt>, <tt>Hybrid</tt>, <tt>City</tt>, <tt>Cross</tt>, <tt>Mountain</tt>.
     * The default type is <tt>Hybrid</tt>.
     *
     * @param bicycleType the type of bicycle
     * @return this builder for chaining options together
     */
    @Nullable
    public abstract Builder bicycleType(@OfflineCriteria.BicycleType @Nullable String bicycleType);

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
    @Nullable
    public abstract Builder cyclingSpeed(@Nullable @FloatRange(from = 5, to = 60) Float cyclingSpeed);

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
    @Nullable
    public abstract Builder useRoads(@Nullable @FloatRange(from = 0.0, to = 1.0) Float useRoads);

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
    @Nullable
    public abstract Builder useHills(@Nullable @FloatRange(from = 0.0, to = 1.0) Float useHills);

    /**
     * This value indicates the willingness to take ferries. This is a range of values between 0 and 1.
     * Values near 0 attempt to avoid ferries and values near 1 will favor ferries. Note that
     * sometimes ferries are required to complete a route so values of 0 are not guaranteed to avoid
     * ferries entirely. The default value is 0.5.
     *
     * @param useFerry the willingness to take ferries
     * @return this builder for chaining options together
     */
    @Nullable
    public abstract Builder useFerry(@Nullable @FloatRange(from = 0.0, to = 1.0) Float useFerry);

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
    @Nullable
    public abstract Builder avoidBadSurfaces(@Nullable @FloatRange(from = 0.0, to = 1.0) Float avoidBadSurfaces);

    /**
     * The same waypoint types the user originally made when the request was made.
     *
     * @param waypointTypes break, through or omitted (;)
     * @return this builder for chaining options together
     */
    @Nullable
    public abstract Builder waypointTypes(@OfflineCriteria.WaypointType @Nullable List<String> waypointTypes);

    /**
     * Builds a new instance of the {@link OfflineNavigationOptions} object.
     *
     * @return a new {@link OfflineNavigationOptions} instance
     */
    public abstract OfflineNavigationOptions build();
  }

  public static Builder builder() {
    return new AutoValue_OfflineNavigationOptions.Builder();
  }
}
