package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

class DirectionViewHolder extends RecyclerView.ViewHolder {

  ImageView maneuverImage;
  TextView stepDistanceText;
  TextView stepPrimaryText;
  TextView stepSecondaryText;

  DirectionViewHolder(View itemView) {
    super(itemView);
    maneuverImage = itemView.findViewById(R.id.maneuverImageView);
    stepDistanceText = itemView.findViewById(R.id.stepDistanceText);
    stepPrimaryText = itemView.findViewById(R.id.stepPrimaryText);
    stepSecondaryText = itemView.findViewById(R.id.stepSecondaryText);
    initInstructionAutoSize();
  }

  /**
   * Called after we bind the views, this will allow the step instruction {@link TextView}
   * to automatically re-size based on the length of the text.
   */
  private void initInstructionAutoSize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepPrimaryText,
      24, 30, 1, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepSecondaryText,
      20, 26, 1, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepDistanceText,
      16, 20, 1, TypedValue.COMPLEX_UNIT_SP);
  }
}
