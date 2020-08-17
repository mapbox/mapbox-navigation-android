package com.mapbox.navigation.ui.internal.instruction.turnlane;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.navigation.ui.R;

import java.util.ArrayList;
import java.util.List;

public class TurnLaneAdapter extends RecyclerView.Adapter<TurnLaneViewHolder> {

  private static final String EMPTY_STRING = "";
  private String maneuverModifier = EMPTY_STRING;
  @NonNull
  private List<BannerComponents> laneComponents = new ArrayList<>();
  @StyleRes
  private int turnLaneViewStyle;

  public TurnLaneAdapter(@StyleRes int turnLaneViewStyle) {
    this.turnLaneViewStyle = turnLaneViewStyle;
  }

  @NonNull
  @Override
  public TurnLaneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.mapbox_item_instruction_turn_lane, parent, false);

    return new TurnLaneViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull TurnLaneViewHolder holder, int position) {
    BannerComponents lane = laneComponents.get(position);
    holder.turnLaneView.updateLaneView(lane, maneuverModifier, turnLaneViewStyle);
  }

  @Override
  public int getItemCount() {
    return laneComponents.size();
  }

  public void addTurnLanes(@NonNull List<BannerComponents> laneComponents, String maneuverModifier) {
    this.laneComponents.clear();
    this.laneComponents.addAll(laneComponents);
    this.maneuverModifier = maneuverModifier;
    notifyDataSetChanged();
  }
}