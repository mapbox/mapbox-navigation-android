package com.mapbox.services.android.navigation.v5.milestone;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.span.ImageSpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.SpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;
import com.mapbox.services.android.navigation.v5.utils.span.TextSpanItem;

import java.util.ArrayList;
import java.util.List;

public class BannerInstructionMilestone extends Milestone {

  private SpannableStringBuilder primaryInstruction;
  private SpannableStringBuilder secondaryInstruction;
  private DirectionsRoute currentRoute;
  private LegStep currentStep;
  private List<BannerInstructions> stepBannerInstructions;

  BannerInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (newRoute(routeProgress)) {
      clearInstructionList();
    }
    if (shouldAddInstructions(routeProgress)) {
      stepBannerInstructions = routeProgress.currentLegProgress().currentStep().bannerInstructions();
    }
    for (BannerInstructions instructions : stepBannerInstructions) {
      if (shouldBeVoiced(routeProgress, instructions)) {
        buildInstructions(instructions);
        stepBannerInstructions.remove(instructions);
        return true;
      }
    }
    return false;
  }

  private void buildInstructions(BannerInstructions instructions) {
    primaryInstruction = SpanUtils.combineSpans(buildSpanItems(instructions.primary()));
    secondaryInstruction = SpanUtils.combineSpans(buildSpanItems(instructions.secondary()));
  }

  private List<SpanItem> buildSpanItems(BannerText bannerText) {
    List<SpanItem> spanItems = new ArrayList<>();
    if (hasComponents(bannerText)) {
      for (BannerComponents components : bannerText.components()) {
        if (!TextUtils.isEmpty(components.imageBaseUrl())) {
          spanItems.add(new ImageSpanItem(components.imageBaseUrl()));
        } else {
          spanItems.add(new TextSpanItem(new StyleSpan(Typeface.BOLD), components.text()));
        }
      }
    }
    return spanItems;
  }

  private boolean hasComponents(BannerText text) {
    return text.components() != null && !text.components().isEmpty();
  }

  SpannableStringBuilder getPrimaryInstruction() {
    return primaryInstruction;
  }

  SpannableStringBuilder getSecondaryInstruction() {
    return secondaryInstruction;
  }

  /**
   * Check if a new set of step instructions should be set.
   *
   * @param routeProgress the current route progress
   * @return true if new instructions should be added to the list, false if not
   */
  private boolean shouldAddInstructions(RouteProgress routeProgress) {
    return newStep(routeProgress) || stepBannerInstructions == null;
  }

  /**
   * Called when adding new instructions to the list.
   * <p>
   * Make sure old announcements are not called (can happen in reroute scenarios).
   */
  private void clearInstructionList() {
    if (stepBannerInstructions != null && !stepBannerInstructions.isEmpty()) {
      stepBannerInstructions.clear();
    }
  }

  /**
   * Looks to see if we have a new step.
   *
   * @param routeProgress provides updated step information
   * @return true if new step, false if not
   */
  private boolean newStep(RouteProgress routeProgress) {
    boolean newStep = currentStep == null || !currentStep.equals(routeProgress.currentLegProgress().currentStep());
    currentStep = routeProgress.currentLegProgress().currentStep();
    return newStep;
  }

  /**
   * Looks to see if we have a new route.
   *
   * @param routeProgress provides updated route information
   * @return true if new route, false if not
   */
  private boolean newRoute(RouteProgress routeProgress) {
    boolean newRoute = currentRoute == null || !currentRoute.equals(routeProgress.directionsRoute());
    currentRoute = routeProgress.directionsRoute();
    return newRoute;
  }

  /**
   * Uses the current step distance remaining to check against banner instructions distance.
   *
   * @param routeProgress      the current route progress
   * @param bannerInstructions given banner instructions from the list of step instructions
   * @return true if time to show the instructions, false if not
   */
  private boolean shouldBeVoiced(RouteProgress routeProgress, BannerInstructions bannerInstructions) {
    return bannerInstructions.distanceAlongGeometry()
      >= routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
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