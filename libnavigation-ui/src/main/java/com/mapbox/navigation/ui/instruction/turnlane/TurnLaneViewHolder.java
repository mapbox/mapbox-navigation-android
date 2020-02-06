package com.mapbox.navigation.ui.instruction.turnlane;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.libnavigation.ui.R;

class TurnLaneViewHolder extends RecyclerView.ViewHolder {

  TurnLaneView turnLaneView;

  TurnLaneViewHolder(View itemView) {
    super(itemView);
    turnLaneView = itemView.findViewById(R.id.turnLaneView);
  }
}
