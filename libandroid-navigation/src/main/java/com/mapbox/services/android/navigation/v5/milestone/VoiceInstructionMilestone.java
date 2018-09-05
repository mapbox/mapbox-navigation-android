package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * A default milestone that is added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}
 * when default milestones are enabled.
 * <p>
 * Please note, this milestone has a custom trigger based on location progress along a route.  If you
 * set custom triggers, they will be ignored in favor of this logic.
 */
public class VoiceInstructionMilestone extends Milestone {

  private static final String EMPTY_STRING = "";
  private String announcement = EMPTY_STRING;
  private String ssmlAnnouncement = EMPTY_STRING;

  VoiceInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return updateCurrentAnnouncement(routeProgress);
  }

  @Override
  public Instruction getInstruction() {
    return new Instruction() {
      @Override
      public String buildInstruction(RouteProgress routeProgress) {
        return announcement;
      }
    };
  }

  /**
   * Provide the SSML instruction that can be used with Mapbox's API Voice.
   * <p>
   * This String will provide special markup denoting how certain portions of the announcement
   * should be pronounced.
   *
   * @return announcement with SSML markup
   * @since 0.8.0
   */
  public String getSsmlAnnouncement() {
    return ssmlAnnouncement;
  }

  /**
   * Provide the instruction that can be used with Android's TextToSpeech.
   * <p>
   * This string will be in plain text.
   *
   * @return announcement in plain text
   * @since 0.12.0
   */
  public String getAnnouncement() {
    return announcement;
  }

  public static final class Builder extends Milestone.Builder {

    private Trigger.Statement trigger;

    public Builder() {
      super();
    }

    @Override
    Trigger.Statement getTrigger() {
      return trigger;
    }

    @Override
    public Builder setTrigger(Trigger.Statement trigger) {
      this.trigger = trigger;
      return this;
    }

    @Override
    public VoiceInstructionMilestone build() {
      return new VoiceInstructionMilestone(this);
    }
  }

  private boolean updateCurrentAnnouncement(RouteProgress routeProgress) {
    if (!announcement.equals(routeProgress.currentAnnouncement())) {
      announcement = routeProgress.currentAnnouncement();
      ssmlAnnouncement = routeProgress.currentSsmlAnnouncement();
      return true;
    }
    return false;
  }
}