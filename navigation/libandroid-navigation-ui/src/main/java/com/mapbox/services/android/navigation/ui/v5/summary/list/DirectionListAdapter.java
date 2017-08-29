package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.api.directions.v5.models.LegStep;

import java.util.ArrayList;
import java.util.List;

public class DirectionListAdapter extends RecyclerView.Adapter<DirectionViewHolder> {

  private List<LegStep> legSteps;

  public DirectionListAdapter() {
    this.legSteps = new ArrayList<>();
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

    holder.instructionText.setText(legStep.getManeuver().getInstruction());
    holder.directionIcon.setImageResource(ManeuverUtils.getManeuverResource(legStep));

    if (legStep.getDistance() > 0) {
      holder.distanceText.setText(DistanceUtils.distanceFormatterBold(legStep.getDistance()));
    } else {
      holder.distanceDivider.setVisibility(View.GONE);
    }
  }

  @Override
  public int getItemCount() {
    return legSteps.size();
  }

  public void addLegSteps(List<LegStep> steps) {
    legSteps.clear();
    legSteps.addAll(steps);
    notifyDataSetChanged();
  }
}
