package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.ArrayList;
import java.util.List;

public class TurnLaneAdapter extends RecyclerView.Adapter<TurnLaneViewHolder> {

  private static final String EMPTY_STRING = "";
  private String maneuverModifier = EMPTY_STRING;
  private List<BannerComponents> laneComponents = new ArrayList<>();

  @NonNull
  @Override
  public TurnLaneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.turn_lane_listitem_layout, parent, false);

    return new TurnLaneViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull TurnLaneViewHolder holder, int position) {
    BannerComponents lane = laneComponents.get(position);
    holder.turnLaneView.updateLaneView(lane, maneuverModifier);
  }

  @Override
  public int getItemCount() {
    return laneComponents.size();
  }

  public void addTurnLanes(List<BannerComponents> laneComponents,  String maneuverModifier) {
    this.laneComponents.clear();
    this.laneComponents.addAll(laneComponents);
    this.maneuverModifier = maneuverModifier;
    notifyDataSetChanged();
  }
}