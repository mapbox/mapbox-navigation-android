package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.TextInstruction;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.utils.ManeuverUtils.getManeuverResource;

public class InstructionListAdapter extends RecyclerView.Adapter<DirectionViewHolder> {

  private List<LegStep> stepList;
  private DecimalFormat decimalFormat;

  private RouteLeg currentLeg;
  private LegStep currentStep;
  private LegStep currentUpcomingStep;

  public InstructionListAdapter() {
    stepList = new ArrayList<>();
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
  }

  @Override
  public DirectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.instruction_viewholder_layout, parent, false);
    return new DirectionViewHolder(view);
  }

  @Override
  public void onBindViewHolder(DirectionViewHolder holder, int position) {
    if (stepList.get(position) != null) {
      TextInstruction textInstruction = new TextInstruction(stepList.get(position));
      updatePrimaryText(holder, textInstruction.getPrimaryText());
      updateSecondaryText(holder, textInstruction.getSecondaryText());
      holder.maneuverImage.setImageResource(getManeuverResource(textInstruction.getStep()));
      holder.stepDistanceText.setText(DistanceUtils
        .distanceFormatterBold(textInstruction.getStepDistance(), decimalFormat));
    }
  }

  @Override
  public int getItemCount() {
    return stepList.size();
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

  private void updatePrimaryText(DirectionViewHolder holder, String primaryText) {
    holder.stepPrimaryText.setText(primaryText);
  }

  private void updateSecondaryText(DirectionViewHolder holder, String secondaryText) {
    if (!TextUtils.isEmpty(secondaryText)) {
      holder.stepPrimaryText.setMaxLines(1);
      holder.stepSecondaryText.setVisibility(View.VISIBLE);
      holder.stepSecondaryText.setText(secondaryText);
    } else {
      holder.stepPrimaryText.setMaxLines(2);
      holder.stepSecondaryText.setVisibility(View.GONE);
    }
  }

  private void addLegSteps(RouteProgress routeProgress) {
    if ((newLeg(routeProgress) || stepList.size() == 0) && hasLegSteps(routeProgress)) {
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

  private boolean newStep(RouteProgress routeProgress) {
    boolean newStep = currentStep == null || !currentStep.equals(routeProgress.currentLegProgress().stepIndex());
    currentStep = routeProgress.currentLegProgress().currentStep();
    currentUpcomingStep = routeProgress.currentLegProgress().upComingStep();
    return newStep;
  }
}
