package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.content.res.Configuration;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionText;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class InstructionListAdapter extends RecyclerView.Adapter<InstructionViewHolder> {

  private List<LegStep> stepList;
  private DecimalFormat decimalFormat;

  private RouteLeg currentLeg;
  private LegStep currentStep;
  private LegStep currentUpcomingStep;
  private int unitType;

  public InstructionListAdapter() {
    stepList = new ArrayList<>();
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
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
      InstructionText instructionText = new InstructionText(stepList.get(position));
      updatePrimaryText(holder, SpanUtils.buildInstructionSpanItems(step.bannerInstructions().get(0).primary()));
      updateSecondaryText(holder, instructionText.getSecondaryText());
      updateManeuverView(holder, step);
      holder.stepDistanceText.setText(DistanceUtils
        .distanceFormatter(instructionText.getStepDistance(), decimalFormat, true, unitType));
    }
  }

  @Override
  public int getItemCount() {
    return stepList.size();
  }

  public void updateSteps(RouteProgress routeProgress, @NavigationUnitType.UnitType int unitType) {
    this.unitType = unitType;
    addLegSteps(routeProgress);
    updateStepList(routeProgress);
  }

  public void clear() {
    // Clear remaining stepList
    stepList.clear();
    notifyDataSetChanged();
  }

  private void updatePrimaryText(InstructionViewHolder holder, SpannableStringBuilder primaryText) {
    holder.stepPrimaryText.setText(primaryText);
  }

  private void updateSecondaryText(InstructionViewHolder holder, String secondaryText) {
    if (!TextUtils.isEmpty(secondaryText)) {
      holder.stepPrimaryText.setMaxLines(1);
      holder.stepSecondaryText.setVisibility(View.VISIBLE);
      holder.stepSecondaryText.setText(secondaryText);
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
    if (step.maneuver() != null) {
      holder.maneuverView.setManeuverModifier(step.maneuver().modifier());
      holder.maneuverView.setManeuverType(step.maneuver().type());
    }
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
      removeCurrentUpcomingStep();
    }
  }

  private void removeCurrentStep() {
    int currentStepPosition = stepList.indexOf(currentStep);
    if (currentStepPosition >= 0) {
      stepList.remove(currentStepPosition);
      notifyItemRemoved(currentStepPosition);
    }
  }

  private void removeCurrentUpcomingStep() {
    int currentUpcomingStepPosition = stepList.indexOf(currentUpcomingStep);
    if (currentUpcomingStepPosition >= 0) {
      stepList.remove(currentUpcomingStepPosition);
      notifyItemRemoved(currentUpcomingStepPosition);
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
    currentUpcomingStep = routeProgress.currentLegProgress().upComingStep();
    return newStep;
  }
}
