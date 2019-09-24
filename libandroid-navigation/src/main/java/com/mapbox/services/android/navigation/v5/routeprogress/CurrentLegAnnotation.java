package com.mapbox.services.android.navigation.v5.routeprogress;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.MaxSpeed;

import java.io.Serializable;

/**
 * This class represents the current annotation being traveled along at a given time
 * during navigation.
 * <p>
 * The Mapbox Directions API gives a list of annotations, each item in the list representing an
 * annotation between two points along the leg.
 *
 * @since 0.13.0
 */
@AutoValue
public abstract class CurrentLegAnnotation implements Serializable {

  /**
   * Create a new instance of this class by using the {@link CurrentLegAnnotation.Builder} class.
   *
   * @return this classes {@link CurrentLegAnnotation.Builder} for creating a new instance
   * @since 0.13.0
   */
  public static Builder builder() {
    return new AutoValue_CurrentLegAnnotation.Builder()
      .distanceToAnnotation(0);
  }

  /**
   * The index used to retrieve the annotation values from each array in
   * {@link com.mapbox.api.directions.v5.models.LegAnnotation}.
   *
   * @return index used to look up annotation values
   * @since 0.13.0
   */
  public abstract int index();

  /**
   * Distance along the {@link com.mapbox.api.directions.v5.models.RouteLeg} that adds
   * up to this set of annotation data.
   *
   * @return distance to this set of annotation data
   * @since 0.13.0
   */
  public abstract double distanceToAnnotation();

  /**
   * The distance, in meters, for the given annotation segment.
   *
   * @return distance, in meters, for the given annotation segment
   * @since 0.13.0
   */
  public abstract Double distance();

  /**
   * The speed, in meters per second, for the given annotation segment.
   *
   * @return speed, in meters per second, for the given annotation segment
   * @since 0.13.0
   */
  @Nullable
  public abstract Double duration();

  /**
   * The speed, in meters per second, for the given annotation segment.
   *
   * @return speed, in meters per second, for the given annotation segment
   * @since 0.13.0
   */
  @Nullable
  public abstract Double speed();

  /**
   * The posted speed limit, for the given annotation segment.
   * Maxspeed is only available for the `mapbox/driving` and `mapbox/driving-traffic`
   * profiles, other profiles will return `unknown`s only.
   *
   * @return posted speed limit, for the given annotation segment
   * @since 0.13.0
   */
  @Nullable
  public abstract MaxSpeed maxspeed();

  /**
   * The congestion for the given annotation segment.
   *
   * @return congestion for the given annotation segment
   * @since 0.13.0
   */
  @Nullable
  public abstract String congestion();

  /**
   * This builder can be used to set the values describing the {@link CurrentLegAnnotation}.
   *
   * @since 0.13.0
   */
  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * The distance, in meters, between each pair of coordinates.
     *
     * @param distance a list with each entry being a distance value between two of the routeLeg
     *                 geometry coordinates
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder distance(Double distance);

    /**
     * The speed, in meters per second, for the given annotation segment.
     *
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder duration(@Nullable Double duration);

    /**
     * The speed, in meters per second, for the given annotation segment.
     *
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder speed(@Nullable Double speed);

    /**
     * The posted speed limit, for the given annotation segment.
     * Maxspeed is only available for the `mapbox/driving` and `mapbox/driving-traffic`
     * profiles, other profiles will return `unknown`s only.
     *
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder maxspeed(@Nullable MaxSpeed maxspeed);

    /**
     * The congestion for the given annotation segment.
     *
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder congestion(@Nullable String congestion);

    /**
     * The index used to retrieve the annotation values from each array in
     * {@link com.mapbox.api.directions.v5.models.LegAnnotation}.
     *
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder index(int index);

    /**
     * Distance along the {@link com.mapbox.api.directions.v5.models.RouteLeg} that adds
     * up to this set of annotation data.
     *
     * @return this builder for chaining options together
     * @since 0.13.0
     */
    public abstract Builder distanceToAnnotation(double distanceToAnnotation);

    /**
     * Build a new {@link CurrentLegAnnotation} object.
     *
     * @return a new {@link CurrentLegAnnotation} using the provided values in this builder
     * @since 0.13.0
     */
    public abstract CurrentLegAnnotation build();
  }
}
