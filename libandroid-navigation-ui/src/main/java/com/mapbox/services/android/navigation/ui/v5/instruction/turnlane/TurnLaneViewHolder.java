package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.stylekit.LaneView;

class TurnLaneViewHolder extends RecyclerView.ViewHolder {

  public LaneView laneView;

  TurnLaneViewHolder(View itemView) {
    super(itemView);
    laneView = itemView.findViewById(R.id.laneView);
  }
}
