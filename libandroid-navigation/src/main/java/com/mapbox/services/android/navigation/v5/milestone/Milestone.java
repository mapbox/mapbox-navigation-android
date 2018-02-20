package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.exception.NavigationException;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * Base Milestone statement. Subclassed to provide concrete statements.
 *
 * @since 0.4.0
 */
@SuppressWarnings("WeakerAccess") // Public exposed for creation of milestone classes outside SDK
public abstract class Milestone {

  private Builder builder;

  public Milestone(Builder builder) {
    this.builder = builder;
  }

  /**
   * Milestone specific identifier as an {@code int} value, useful for deciphering which milestone
   * invoked {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)}.
   *
   * @return {@code int} representing the identifier
   * @since 0.4.0
   */
  public int getIdentifier() {
    return builder.getIdentifier();
  }

  /**
   * Milestone specific {@link Instruction}, which can be used to build a {@link String}
   * instruction specified by the superclass.
   *
   * @return {@link Instruction} to be used to build the {@link String} passed to
   * {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)}
   * @since 0.4.0
   */
  public Instruction getInstruction() {
    return builder.getInstruction();
  }

  /**
   * A milestone can either be passed in to the
   * {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation} object
   * (recommended) or validated directly inside your activity.
   *
   * @param previousRouteProgress last locations generated {@link RouteProgress} object used to
   *                              determine certain {@link TriggerProperty}s
   * @param routeProgress         used to determine certain {@link TriggerProperty}s
   * @return true if the milestone trigger's valid, else false
   * @since 0.4.0
   */
  public abstract boolean isOccurring(RouteProgress previousRouteProgress,
                                      RouteProgress routeProgress);

  /**
   * Build a new {@link Milestone}
   *
   * @since 0.4.0
   */
  public abstract static class Builder {

    private int identifier;
    private Instruction instruction;

    public Builder() {
    }

    /**
     * Milestone specific identifier as an {@code int} value, useful for deciphering which milestone
     * invoked {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)}.
     *
     * @return {@code int} representing the identifier
     * @since 0.4.0
     */
    public int getIdentifier() {
      return identifier;
    }

    /**
     * Milestone specific identifier as an {@code int} value, useful for deciphering which milestone
     * invoked {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)}.
     *
     * @param identifier an {@code int} used to identify this milestone instance
     * @return this builder
     * @since 0.4.0
     */
    public Builder setIdentifier(int identifier) {
      this.identifier = identifier;
      return this;
    }

    /**
     * Milestone specific {@link Instruction}, which can be used to build a {@link String}
     * instruction specified by the superclass
     *
     * @return this builder
     * @since 0.4.0
     */
    public Instruction getInstruction() {
      return instruction;
    }

    public Builder setInstruction(Instruction instruction) {
      this.instruction = instruction;
      return this;
    }

    /**
     * The list of triggers that are used to determine whether this milestone should invoke
     * {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)}
     *
     * @param trigger a single simple statement or compound statement found in {@link Trigger}
     * @return this builder
     * @since 0.4.0
     */
    public abstract Builder setTrigger(Trigger.Statement trigger);

    abstract Trigger.Statement getTrigger();

    /**
     * Build a new milestone
     *
     * @return A new {@link Milestone} object
     * @throws NavigationException if an invalid value has been set on the milestone
     * @since 0.4.0
     */
    public abstract Milestone build();
  }
}
