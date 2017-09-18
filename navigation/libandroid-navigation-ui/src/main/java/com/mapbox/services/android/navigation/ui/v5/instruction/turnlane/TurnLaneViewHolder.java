package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.mapbox.services.android.navigation.ui.v5.R;

class TurnLaneViewHolder extends RecyclerView.ViewHolder {

  ImageView turnImage;

  TurnLaneViewHolder(View itemView) {
    super(itemView);
    turnImage = itemView.findViewById(R.id.turnImage);
  }
}
