package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

public class DirectionViewHolder extends RecyclerView.ViewHolder {

  private boolean firstViewHolder;

  TextView instructionText;
  TextView distanceText;
  ImageView directionIcon;
  View distanceDivider;

  DirectionViewHolder(View itemView) {
    super(itemView);
    instructionText = itemView.findViewById(R.id.instructionText);
    distanceText = itemView.findViewById(R.id.distanceText);
    directionIcon = itemView.findViewById(R.id.directionIcon);
    distanceDivider = itemView.findViewById(R.id.distanceDivider);
  }

  public void updateFirstViewHolder() {
    if (!firstViewHolder) {
      instructionText.setTypeface(Typeface.DEFAULT_BOLD);
      firstViewHolder = true;
    }
  }

  void resetViewSizes() {
    instructionText.setTypeface(Typeface.DEFAULT);
    firstViewHolder = false;
  }
}
