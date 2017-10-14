package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DirectionListAdapter extends RecyclerView.Adapter<DirectionViewHolder> {

  private List<LegStep> legSteps;
  private DecimalFormat decimalFormat;

  private int currentLegIndex = -1;
  private int currentStepIndex = -1;

  public DirectionListAdapter() {
    legSteps = new ArrayList<>();
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
  }

  @Override
  public DirectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.direction_viewholder_layout, parent, false);
    return new DirectionViewHolder(view);
  }

  @Override
  public void onBindViewHolder(DirectionViewHolder holder, int position) {
    LegStep legStep = legSteps.get(position);
    holder.instructionText.setText(StringAbbreviator.abbreviate(legStep.maneuver().instruction()));
    holder.directionIcon.setImageResource(ManeuverUtils.getManeuverResource(legStep));

    if (legStep.distance() > 0) {
      holder.distanceText.setText(DistanceUtils.distanceFormatterBold(legStep.distance(), decimalFormat));
    } else {
      holder.distanceDivider.setVisibility(View.GONE);
    }
  }

  @Override
  public int getItemCount() {
    return legSteps.size();
  }

  @Override
  public void onViewAttachedToWindow(DirectionViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    if (holder.getAdapterPosition() != 0) {
      holder.resetViewSizes();
    }
  }

  public void updateSteps(RouteProgress routeProgress) {
    addLegSteps(routeProgress);
    updateStepList(routeProgress);
  }

  public void clear() {
    legSteps.clear();
    notifyDataSetChanged();
  }

  private void addLegSteps(RouteProgress routeProgress) {
    if ((newLeg(routeProgress) || legSteps.size() == 0) && legHasSteps(routeProgress)) {
      List<LegStep> steps = routeProgress.directionsRoute().legs().get(0).steps();
      addStepsNotifyAdapter(steps);
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

  private void addStepsNotifyAdapter(List<LegStep> steps) {
    legSteps.clear();
    legSteps.addAll(steps);
    notifyDataSetChanged();
  }

  private void removeFirstStep() {
    legSteps.remove(0);
    notifyItemRemoved(0);
  }

  private boolean legHasSteps(RouteProgress routeProgress) {
    return routeProgress.directionsRoute() != null && routeProgress.directionsRoute().legs().size() > 0;
  }

  private boolean newLeg(RouteProgress routeProgress) {
    return this.currentLegIndex != routeProgress.legIndex();
  }

  private boolean newStep(int currentStepIndex) {
    return this.currentStepIndex != currentStepIndex && legSteps.size() > 0;
  }
}
