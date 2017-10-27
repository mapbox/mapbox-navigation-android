package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.directions.v5.models.LegStep;
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

  private List<TextInstruction> instructions;
  private DecimalFormat decimalFormat;

  private int currentLegIndex = -1;
  private int currentStepIndex = -1;

  public InstructionListAdapter() {
    instructions = new ArrayList<>();
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
    TextInstruction textInstruction = instructions.get(position);
    if (textInstruction != null) {
      updatePrimaryText(holder, textInstruction.getPrimaryText());
      updateSecondaryText(holder, textInstruction.getSecondaryText());
      holder.maneuverImage.setImageResource(getManeuverResource(textInstruction.getStep()));
      holder.stepDistanceText.setText(DistanceUtils
        .distanceFormatterBold(textInstruction.getStepDistance(), decimalFormat));
    }
  }

  @Override
  public int getItemCount() {
    return instructions.size();
  }

  public void updateSteps(RouteProgress routeProgress) {
    addLegSteps(routeProgress);
    updateStepList(routeProgress);
  }

  public void clear() {
    instructions.clear();
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
    if ((newLeg(routeProgress) || instructions.size() == 0) && legHasSteps(routeProgress)) {
      List<LegStep> steps = routeProgress.directionsRoute().legs().get(0).steps();
      instructions.clear();
      if (steps != null) {
        for (LegStep step : steps) {
          instructions.add(new TextInstruction(step));
        }
      }
      notifyDataSetChanged();
      currentLegIndex = routeProgress.legIndex();
    }
  }

  private void updateStepList(RouteProgress routeProgress) {
    int currentStepIndex = routeProgress.currentLegProgress().stepIndex();
    if (newStep(currentStepIndex)) {
      removeFirstStep();
      this.currentStepIndex = currentStepIndex;
    }
  }

  private void removeFirstStep() {
    instructions.remove(0);
    notifyItemRemoved(0);
  }

  private boolean legHasSteps(RouteProgress routeProgress) {
    return routeProgress.directionsRoute() != null && routeProgress.directionsRoute().legs().size() > 0;
  }

  private boolean newLeg(RouteProgress routeProgress) {
    return this.currentLegIndex != routeProgress.legIndex();
  }

  private boolean newStep(int currentStepIndex) {
    return this.currentStepIndex != currentStepIndex && instructions.size() > 0;
  }
}
