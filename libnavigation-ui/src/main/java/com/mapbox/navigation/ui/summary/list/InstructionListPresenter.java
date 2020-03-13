package com.mapbox.navigation.ui.summary.list;

import android.text.SpannableString;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class InstructionListPresenter {

  private static final int TWO_LINES = 2;
  private static final int ONE_LINE = 1;
  private static final float TWO_LINE_BIAS = 0.65f;
  private static final float ONE_LINE_BIAS = 0.5f;
  private static final int FIRST_INSTRUCTION_INDEX = 0;
  private DistanceFormatter distanceFormatter;
  private List<BannerInstructions> instructions;
  private RouteLeg currentLeg;
  private String drivingSide;

  InstructionListPresenter(DistanceFormatter distanceFormatter) {
    this.distanceFormatter = distanceFormatter;
    instructions = new ArrayList<>();
  }

  void onBindInstructionListViewAtPosition(int position, @NonNull InstructionListView listView) {
    BannerInstructions bannerInstructions = instructions.get(position);
    double distance = bannerInstructions.distanceAlongGeometry();
    SpannableString distanceText = distanceFormatter.formatDistance(distance);
    updateListView(listView, bannerInstructions, distanceText);
  }

  int retrieveBannerInstructionListSize() {
    return instructions.size();
  }

  boolean updateBannerListWith(RouteProgress routeProgress) {
    addBannerInstructions(routeProgress);
    return updateInstructionList(routeProgress);
  }

  void updateDistanceFormatter(DistanceFormatter distanceFormatter) {
    if (shouldUpdate(distanceFormatter)) {
      this.distanceFormatter = distanceFormatter;
    }
  }

  private boolean shouldUpdate(DistanceFormatter distanceFormatter) {
    return distanceFormatter != null
        && (this.distanceFormatter == null || !this.distanceFormatter.equals(distanceFormatter));
  }

  private void updateListView(@NonNull InstructionListView listView, BannerInstructions bannerInstructions,
      SpannableString distanceText) {
    listView.updatePrimaryText(bannerInstructions.primary().text());
    updateSecondaryInstruction(listView, bannerInstructions);
    updateManeuverView(listView, bannerInstructions);
    listView.updateDistanceText(distanceText);
  }

  private void updateSecondaryInstruction(@NonNull InstructionListView listView,
      BannerInstructions bannerInstructions) {
    boolean hasSecondaryInstructions = bannerInstructions.secondary() != null;
    adjustListViewForSecondaryInstructions(listView, hasSecondaryInstructions);
    if (hasSecondaryInstructions) {
      listView.updateSecondaryText(bannerInstructions.secondary().text());
    }
  }

  private void adjustListViewForSecondaryInstructions(InstructionListView listView, boolean hasSecondaryInstructions) {
    if (hasSecondaryInstructions) {
      hasSecondaryInstructions(listView);
    } else {
      hasNoSecondaryInstructions(listView);
    }
  }

  private void hasSecondaryInstructions(InstructionListView listView) {
    listView.updatePrimaryMaxLines(ONE_LINE);
    listView.updateSecondaryVisibility(View.VISIBLE);
    listView.updateBannerVerticalBias(TWO_LINE_BIAS);
  }

  private void hasNoSecondaryInstructions(InstructionListView listView) {
    listView.updatePrimaryMaxLines(TWO_LINES);
    listView.updateSecondaryVisibility(View.GONE);
    listView.updateBannerVerticalBias(ONE_LINE_BIAS);
  }

  private void updateManeuverView(@NonNull InstructionListView listView, BannerInstructions bannerInstructions) {
    String maneuverType = bannerInstructions.primary().type();
    String maneuverModifier = bannerInstructions.primary().modifier();
    listView.updateManeuverViewTypeAndModifier(maneuverType, maneuverModifier);

    Double roundaboutDegrees = bannerInstructions.primary().degrees();
    if (roundaboutDegrees != null) {
      listView.updateManeuverViewRoundaboutDegrees(roundaboutDegrees.floatValue());
    }
    listView.updateManeuverViewDrivingSide(drivingSide);
  }

  private void addBannerInstructions(RouteProgress routeProgress) {
    if (isNewLeg(routeProgress)) {
      instructions = new ArrayList<>();
      currentLeg = routeProgress.currentLegProgress().routeLeg();
      if (routeProgress.currentLegProgress().currentStepProgress() != null
          && routeProgress.currentLegProgress().currentStepProgress().step() != null) {
        drivingSide = routeProgress.currentLegProgress().currentStepProgress().step().drivingSide();
      }
      if (currentLeg != null) {
        List<LegStep> steps = currentLeg.steps();
        for (LegStep step : steps) {
          List<BannerInstructions> bannerInstructions = step.bannerInstructions();
          if (bannerInstructions != null && !bannerInstructions.isEmpty()) {
            instructions.addAll(bannerInstructions);
          }
        }
      }
    }
  }

  private boolean isNewLeg(RouteProgress routeProgress) {
    return currentLeg == null || !currentLeg.equals(routeProgress.currentLegProgress());
  }

  private boolean updateInstructionList(RouteProgress routeProgress) {
    if (instructions.isEmpty()) {
      return false;
    }
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    LegStep currentStep = legProgress.currentStepProgress().step();
    double stepDistanceRemaining = legProgress.currentStepProgress().distanceRemaining();
    BannerInstructions currentBannerInstructions = findCurrentBannerInstructions(
        currentStep, stepDistanceRemaining
    );
    if (!instructions.contains(currentBannerInstructions)) {
      return false;
    }
    int currentInstructionIndex = instructions.indexOf(currentBannerInstructions);
    return removeInstructionsFrom(currentInstructionIndex);
  }

  private boolean removeInstructionsFrom(int currentInstructionIndex) {
    if (currentInstructionIndex == FIRST_INSTRUCTION_INDEX) {
      instructions.remove(FIRST_INSTRUCTION_INDEX);
      return true;
    } else if (currentInstructionIndex <= instructions.size()) {
      instructions.subList(FIRST_INSTRUCTION_INDEX, currentInstructionIndex).clear();
      return true;
    }
    return false;
  }

  /**
   * Given the current step / current step distance remaining, this function will
   * find the current instructions to be shown.
   *
   * @param currentStep holding the current banner instructions
   * @param stepDistanceRemaining to determine progress along the currentStep
   * @return the current banner instructions based on the current distance along the step
   * @since 0.13.0
   */
  @Nullable
  BannerInstructions findCurrentBannerInstructions(@Nullable LegStep currentStep, double stepDistanceRemaining) {
    if (currentStep == null) {
      return null;
    }
    List<BannerInstructions> instructions = currentStep.bannerInstructions();
    if (instructions != null && !instructions.isEmpty()) {
      List<BannerInstructions> sortedInstructions = sortBannerInstructions(instructions);
      for (BannerInstructions bannerInstructions : sortedInstructions) {
        int distanceAlongGeometry = (int) bannerInstructions.distanceAlongGeometry();
        if (distanceAlongGeometry >= (int) stepDistanceRemaining) {
          return bannerInstructions;
        }
      }
      return instructions.get(FIRST_INSTRUCTION_INDEX);
    } else {
      return null;
    }
  }

  private List<BannerInstructions> sortBannerInstructions(List<BannerInstructions> instructions) {
    List<BannerInstructions> sortedInstructions = new ArrayList<>(instructions);
    Collections.sort(sortedInstructions, new Comparator<BannerInstructions>() {
      @Override
      public int compare(BannerInstructions instructions, BannerInstructions nextInstructions) {
        return Double.compare(instructions.distanceAlongGeometry(), nextInstructions.distanceAlongGeometry());
      }
    });
    return sortedInstructions;
  }
}
