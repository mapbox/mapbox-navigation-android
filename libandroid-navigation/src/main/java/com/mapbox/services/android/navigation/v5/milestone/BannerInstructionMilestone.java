package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.navigator.BannerComponent;
import com.mapbox.navigator.BannerInstruction;
import com.mapbox.navigator.BannerSection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * A default milestone that is added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}
 * when default milestones are enabled.
 * <p>
 * Please note, this milestone has a custom trigger based on location progress along a route.  If you
 * set custom triggers, they will be ignored in favor of this logic.
 */
public class BannerInstructionMilestone extends Milestone {

  private BannerInstructions instructions;

  private BannerInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return updateCurrentBanner(routeProgress);
  }

  private boolean updateCurrentBanner(RouteProgress routeProgress) {
    BannerInstruction currentBannerInstruction = routeProgress.bannerInstruction();
    if (currentBannerInstruction != null) {
      BannerSection currentPrimary = currentBannerInstruction.getPrimary();
      BannerText primary = retrieveBannerFrom(currentPrimary);
      BannerSection currentSecondary = currentBannerInstruction.getSecondary();
      BannerText secondary = retrieveBannerFrom(currentSecondary);
      BannerSection currentSub = currentBannerInstruction.getSub();
      BannerText sub = retrieveBannerFrom(currentSub);

      this.instructions = BannerInstructions.builder()
        .primary(primary)
        .secondary(secondary)
        .sub(sub)
        .distanceAlongGeometry(currentBannerInstruction.getRemainingStepDistance())
        .build();
      return true;
    }
    return false;
  }

  private BannerText retrieveBannerFrom(BannerSection bannerSection) {
    BannerText banner = null;
    if (bannerSection == null) {
      return banner;
    }
    List<BannerComponent> currentComponents = bannerSection.getComponents();
    if (currentComponents != null) {
      List<BannerComponents> primaryComponents = new ArrayList<>();
      for (BannerComponent bannerComponent : currentComponents) {
        BannerComponents bannerComponents = BannerComponents.builder()
          .text(bannerComponent.getText())
          .type(bannerComponent.getType())
          .abbreviation(bannerComponent.getAbbr())
          .abbreviationPriority(bannerComponent.getAbbrPriority())
          .imageBaseUrl(bannerComponent.getImageBaseurl())
          .directions(bannerComponent.getDirections())
          .active(bannerComponent.getActive())
          .build();
        primaryComponents.add(bannerComponents);
      }
      Integer bannerSectionDegrees = bannerSection.getDegrees();
      Double degrees = null;
      if (bannerSectionDegrees != null) {
        degrees = Double.valueOf(bannerSectionDegrees);
      }
      banner = BannerText.builder()
        .text(bannerSection.getText())
        .type(bannerSection.getType())
        .modifier(bannerSection.getModifier())
        .degrees(degrees)
        .drivingSide(bannerSection.getDrivingSide())
        .components(primaryComponents)
        .build();
    }
    return banner;
  }

  /**
   * Returns the given {@link BannerInstructions} for the time that the milestone is triggered.
   *
   * @return current banner instructions based on distance along the current step
   * @since 0.13.0
   */
  public BannerInstructions getBannerInstructions() {
    return instructions;
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
    public BannerInstructionMilestone build() {
      return new BannerInstructionMilestone(this);
    }
  }
}