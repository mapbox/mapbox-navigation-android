package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.content.Context;
import android.content.res.Configuration;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstructionListAdapter extends RecyclerView.Adapter<InstructionViewHolder> {
  private List<LegStep> stepList;
  private RouteLeg currentLeg;
  private LegStep currentStep;
  private DistanceUtils distanceUtils;

  public InstructionListAdapter(Context context,
                                Locale locale, @NavigationUnitType.UnitType int unitType) {
    stepList = new ArrayList<>();
    distanceUtils = new DistanceUtils(context, locale, unitType);
  }

  @Override
  public InstructionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.instruction_viewholder_layout, parent, false);
    return new InstructionViewHolder(view);
  }

  @Override
  public void onBindViewHolder(InstructionViewHolder holder, int position) {
    if (stepList.get(position) != null) {
      LegStep step = stepList.get(position);
      if (hasBannerInstructions(step)) {
        updatePrimaryText(holder, step.bannerInstructions().get(0).primary());
        updateSecondaryText(holder, step.bannerInstructions().get(0).secondary());
      } else {
        holder.stepPrimaryText.setText(step.maneuver().instruction());
        updateSecondaryText(holder, null);
      }
      updateManeuverView(holder, step);

      SpannableString distanceText = distanceUtils.formatDistance(step.distance());
      holder.stepDistanceText.setText(distanceText);
    }
  }

  @Override
  public int getItemCount() {
    return stepList.size();
  }

  @Override
  public void onViewDetachedFromWindow(InstructionViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
    holder.itemView.clearAnimation();
  }

  public void updateSteps(RouteProgress routeProgress) {
    addLegSteps(routeProgress);
    updateStepList(routeProgress);
  }

  public void clear() {
    // Clear remaining stepList
    stepList.clear();
    notifyDataSetChanged();
  }

  private boolean hasBannerInstructions(LegStep step) {
    return step.bannerInstructions() != null && !step.bannerInstructions().isEmpty();
  }

  private void updatePrimaryText(InstructionViewHolder holder, BannerText primaryText) {
    holder.stepPrimaryText.setText(primaryText.text());
  }

  private void updateSecondaryText(InstructionViewHolder holder, BannerText secondaryText) {
    if (secondaryText != null) {
      holder.stepPrimaryText.setMaxLines(1);
      holder.stepSecondaryText.setVisibility(View.VISIBLE);
      holder.stepSecondaryText.setText(secondaryText.text());
      adjustBannerTextVerticalBias(holder, 0.65f);
    } else {
      holder.stepPrimaryText.setMaxLines(2);
      holder.stepSecondaryText.setVisibility(View.GONE);
      adjustBannerTextVerticalBias(holder, 0.5f);
    }
  }

  /**
   * Adjust the banner text layout {@link ConstraintLayout} vertical bias.
   *
   * @param percentBias to be set to the text layout
   */
  private void adjustBannerTextVerticalBias(InstructionViewHolder holder, float percentBias) {
    int orientation = holder.itemView.getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      ConstraintLayout.LayoutParams params =
        (ConstraintLayout.LayoutParams) holder.instructionLayoutText.getLayoutParams();
      params.verticalBias = percentBias;
      holder.instructionLayoutText.setLayoutParams(params);
    }
  }

  private void updateManeuverView(InstructionViewHolder holder, LegStep step) {
    LegStep maneuverStep = step;
    // Get the upcoming LegStep for the ManeuverView if there is one
    int upcomingStepIndex = stepList.indexOf(step) + 1;
    if (upcomingStepIndex < stepList.size()) {
      maneuverStep = stepList.get(upcomingStepIndex);
    }
    holder.maneuverView.setManeuverModifier(maneuverStep.maneuver().modifier());
    holder.maneuverView.setManeuverType(maneuverStep.maneuver().type());
  }

  private void addLegSteps(RouteProgress routeProgress) {
    if ((newLeg(routeProgress) || rerouteTriggered(routeProgress)) && hasLegSteps(routeProgress)) {
      List<LegStep> steps = routeProgress.directionsRoute().legs().get(0).steps();
      stepList.clear();
      stepList.addAll(steps);
      notifyDataSetChanged();
    }
  }

  private void updateStepList(RouteProgress routeProgress) {
    if (newStep(routeProgress)) {
      removeCurrentStep();
    }
  }

  private void removeCurrentStep() {
    int currentStepPosition = stepList.indexOf(currentStep);
    if (currentStepPosition >= 0) {
      stepList.remove(currentStepPosition);
      notifyItemRemoved(currentStepPosition);
    }
  }

  private boolean hasLegSteps(RouteProgress routeProgress) {
    return routeProgress.directionsRoute() != null
      && routeProgress.directionsRoute().legs() != null
      && routeProgress.directionsRoute().legs().size() > 0;
  }

  private boolean newLeg(RouteProgress routeProgress) {
    boolean newLeg = currentLeg == null || !currentLeg.equals(routeProgress.currentLeg());
    currentLeg = routeProgress.currentLeg();
    return newLeg;
  }

  private boolean rerouteTriggered(RouteProgress routeProgress) {
    return stepList.size() == 0
      && !routeProgress.currentLegProgress().currentStep().maneuver().type()
      .equals(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE);
  }

  private boolean newStep(RouteProgress routeProgress) {
    boolean newStep = currentStep == null || !currentStep.equals(routeProgress.currentLegProgress().currentStep());
    currentStep = routeProgress.currentLegProgress().currentStep();
    return newStep;
  }
}
