package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.directions.v5.models.IntersectionLanes;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.commons.utils.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_STRAIGHT;

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
    setLaneImage(holder, lane);
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

  private void setLaneImage(TurnLaneViewHolder holder, IntersectionLanes lanes) {
    StringBuilder builder = new StringBuilder();

    // Indications
    if (lanes.indications() != null) {
      for (String indication : lanes.indications()) {
        builder.append(indication);
      }
      if (builder.toString().contains(TURN_LANE_INDICATION_STRAIGHT) && lanes.valid()) {
        appendModifier(builder);
      }
    }

    if (retrieveTurnLaneResource(builder.toString()) > 0) {
      holder.turnImage.setImageResource(retrieveTurnLaneResource(builder.toString()));
    }

    if (!lanes.valid()) {
      holder.turnImage.setAlpha(0.4f);
    } else {
      holder.turnImage.setAlpha(1.0f);
    }

    // Flip if it's a left indication
    if (builder.toString().contains(TURN_LANE_INDICATION_LEFT)) {
      holder.turnImage.setScaleX(-1);
    } else {
      holder.turnImage.setScaleX(1);
    }
  }

  private void appendModifier(StringBuilder builder) {
    // Maneuver modifier
    if (maneuverModifier.contains(STEP_MANEUVER_MODIFIER_LEFT)) {
      builder.append(STEP_MANEUVER_MODIFIER_LEFT);
    } else if (maneuverModifier.contains(STEP_MANEUVER_MODIFIER_STRAIGHT)) {
      builder.append(STEP_MANEUVER_MODIFIER_STRAIGHT);
    } else if (maneuverModifier.contains(STEP_MANEUVER_MODIFIER_RIGHT)) {
      builder.append(STEP_MANEUVER_MODIFIER_RIGHT);
    }
  }

  private static int retrieveTurnLaneResource(String turnLaneKey) {
    TurnLaneMap turnLaneMap = new TurnLaneMap();
    if (!TextUtils.isEmpty(turnLaneKey) && turnLaneMap.getTurnLaneResource(turnLaneKey) > 0) {
      return turnLaneMap.getTurnLaneResource(turnLaneKey);
    } else {
      return 0;
    }
  }
}