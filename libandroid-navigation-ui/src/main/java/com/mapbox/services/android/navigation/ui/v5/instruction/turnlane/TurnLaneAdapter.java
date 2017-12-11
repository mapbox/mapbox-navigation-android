package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.ArrayList;
import java.util.List;

public class TurnLaneAdapter extends RecyclerView.Adapter<TurnLaneViewHolder> {

  private List<IntersectionLanes> lanes;
  private String maneuverModifier;

  public TurnLaneAdapter() {
    lanes = new ArrayList<>();
  }

  @Override
  public TurnLaneViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.turn_lane_listitem_layout, parent, false);

    return new TurnLaneViewHolder(view);
  }

  @Override
  public void onBindViewHolder(TurnLaneViewHolder holder, int position) {
    IntersectionLanes lane = lanes.get(position);
    holder.turnLaneView.updateLaneView(lane, maneuverModifier);
  }

  @Override
  public int getItemCount() {
    return lanes.size();
  }

  public void addTurnLanes(List<IntersectionLanes> lanes, String maneuverModifier) {
    this.maneuverModifier = maneuverModifier;
    this.lanes.clear();
    this.lanes = lanes;
    notifyDataSetChanged();
  }
}