package com.mapbox.navigation.ui.internal.summary;

import android.text.SpannableString;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

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
  @Nullable
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

  void update(@NonNull RouteProgress routeProgress, @NonNull InstructionListAdapter adapter) {
    drivingSide = getDrivingSide(routeProgress);
    final List<BannerInstructions> routeInstructions = getBannerInstructionsFromRouteProgress(routeProgress);
    final List<BannerInstructions> filteredRouteInstructions = filterListAfterStep(routeProgress, routeInstructions);

    final List<BannerInstructions> oldItems = new ArrayList<>(instructions);
    final DiffUtil.DiffResult result = DiffUtil.calculateDiff(getDiffCallback(oldItems, filteredRouteInstructions));
    instructions.clear();
    instructions.addAll(filteredRouteInstructions);
    result.dispatchUpdatesTo(adapter);
  }

  void updateDistanceFormatter(DistanceFormatter distanceFormatter) {
    if (shouldUpdate(distanceFormatter)) {
      this.distanceFormatter = distanceFormatter;
    }
  }

  private boolean shouldUpdate(@Nullable DistanceFormatter distanceFormatter) {
    return distanceFormatter != null
        && (this.distanceFormatter == null || !this.distanceFormatter.equals(distanceFormatter));
  }

  private void updateListView(@NonNull InstructionListView listView, @NonNull BannerInstructions bannerInstructions,
                              SpannableString distanceText) {
    listView.updatePrimaryText(bannerInstructions.primary().text());
    updateSecondaryInstruction(listView, bannerInstructions);
    updateManeuverView(listView, bannerInstructions);
    listView.updateDistanceText(distanceText);
  }

  private void updateSecondaryInstruction(@NonNull InstructionListView listView,
                                          @NonNull BannerInstructions bannerInstructions) {
    boolean hasSecondaryInstructions = bannerInstructions.secondary() != null;
    adjustListViewForSecondaryInstructions(listView, hasSecondaryInstructions);
    if (hasSecondaryInstructions) {
      listView.updateSecondaryText(bannerInstructions.secondary().text());
    }
  }

  private void adjustListViewForSecondaryInstructions(
          @NonNull InstructionListView listView,
          boolean hasSecondaryInstructions
  ) {
    if (hasSecondaryInstructions) {
      hasSecondaryInstructions(listView);
    } else {
      hasNoSecondaryInstructions(listView);
    }
  }

  private void hasSecondaryInstructions(@NonNull InstructionListView listView) {
    listView.updatePrimaryMaxLines(ONE_LINE);
    listView.updateSecondaryVisibility(View.VISIBLE);
    listView.updateBannerVerticalBias(TWO_LINE_BIAS);
  }

  private void hasNoSecondaryInstructions(@NonNull InstructionListView listView) {
    listView.updatePrimaryMaxLines(TWO_LINES);
    listView.updateSecondaryVisibility(View.GONE);
    listView.updateBannerVerticalBias(ONE_LINE_BIAS);
  }

  private void updateManeuverView(
          @NonNull InstructionListView listView,
          @NonNull BannerInstructions bannerInstructions) {
    String maneuverType = bannerInstructions.primary().type();
    String maneuverModifier = bannerInstructions.primary().modifier();
    listView.updateManeuverViewTypeAndModifier(maneuverType, maneuverModifier);

    Double roundaboutDegrees = bannerInstructions.primary().degrees();
    if (roundaboutDegrees != null) {
      listView.updateManeuverViewRoundaboutDegrees(roundaboutDegrees.floatValue());
    }
    listView.updateManeuverViewDrivingSide(drivingSide);
  }

  @Nullable
  private String getDrivingSide(@NonNull final RouteProgress routeProgress) {
    if (routeProgress.getCurrentLegProgress().getCurrentStepProgress() != null
            && routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep() != null) {
      return routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep().drivingSide();
    } else {
      return "";
    }
  }

  @NonNull
  private List<BannerInstructions> getBannerInstructionsFromRouteProgress(@NonNull final RouteProgress routeProgress) {
    final List<BannerInstructions> instructions = new ArrayList<>();
    final RouteLeg routeLeg = routeProgress.getCurrentLegProgress().getRouteLeg();
    if (routeLeg != null) {
      final List<LegStep> steps = routeLeg.steps();
      if (steps != null) {
        for (LegStep step : steps) {
          final List<BannerInstructions> bannerInstructions = step.bannerInstructions();
          if (bannerInstructions != null) {
            instructions.addAll(bannerInstructions);
          }
        }
      }
    }
    return instructions;
  }

  @NonNull
  private List<BannerInstructions> filterListAfterStep(
          @NonNull final RouteProgress routeProgress,
          @NonNull final List<BannerInstructions> bannerInstructions
  ) {
    final RouteLegProgress legProgress = routeProgress.getCurrentLegProgress();
    final LegStep currentStep = legProgress.getCurrentStepProgress().getStep();
    final double stepDistanceRemaining = legProgress.getCurrentStepProgress().getDistanceRemaining();
    final BannerInstructions currentBannerInstructions = findCurrentBannerInstructions(
            currentStep, stepDistanceRemaining
    );

    if (!bannerInstructions.contains(currentBannerInstructions)) {
      return bannerInstructions;
    } else {
      int currentInstructionIndex = bannerInstructions.indexOf(currentBannerInstructions);
      if (currentInstructionIndex + 1 <= bannerInstructions.size()) {
        return bannerInstructions.subList(currentInstructionIndex + 1, bannerInstructions.size());
      } else {
        return new ArrayList<>();
      }
    }
  }

  /**
   * Given the current step / current step distance remaining, this function will
   * find the current instructions to be shown.
   *
   * @param currentStep holding the current banner instructions
   * @param stepDistanceRemaining to determine progress along the currentStep
   * @return the current banner instructions based on the current distance along the step
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

  @NonNull
  private List<BannerInstructions> sortBannerInstructions(@NonNull List<BannerInstructions> instructions) {
    List<BannerInstructions> sortedInstructions = new ArrayList<>(instructions);
    Collections.sort(sortedInstructions, bannerInstructionsComparator);
    return sortedInstructions;
  }

  @NonNull
  private Comparator<BannerInstructions> bannerInstructionsComparator = (
          instructions,
          nextInstructions
  ) -> Double.compare(instructions.distanceAlongGeometry(), nextInstructions.distanceAlongGeometry());

  @NonNull
  private DiffUtil.Callback getDiffCallback(
          @NonNull final List<BannerInstructions> oldItems,
          @NonNull final List<BannerInstructions> updatedInstructions
  ) {
    return new DiffUtil.Callback() {
      @Override
      public int getOldListSize() {
        return oldItems.size();
      }

      @Override
      public int getNewListSize() {
        return updatedInstructions.size();
      }

      @Override
      public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return bannerInstructionsComparator.compare(
                oldItems.get(oldItemPosition),
                updatedInstructions.get(newItemPosition)
        ) == 0;
      }

      @Override
      public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return bannerInstructionsComparator.compare(
                oldItems.get(oldItemPosition),
                updatedInstructions.get(newItemPosition)
        ) == 0;
      }
    };
  }
}
