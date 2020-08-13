package com.mapbox.navigation.ui.internal.instruction.turnlane;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.ui.instruction.turnlane.TurnLaneView;

class TurnLaneViewHolder extends RecyclerView.ViewHolder {

  TurnLaneView turnLaneView;

  TurnLaneViewHolder(View itemView) {
    super(itemView);
    turnLaneView = itemView.findViewById(R.id.turnLaneView);
  }
}
