package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.maneuver.ManeuverView;

class InstructionViewHolder extends RecyclerView.ViewHolder {

  ManeuverView maneuverView;
  TextView stepDistanceText;
  TextView stepPrimaryText;
  TextView stepSecondaryText;
  View instructionLayoutText;

  InstructionViewHolder(View itemView) {
    super(itemView);
    maneuverView = itemView.findViewById(R.id.maneuverView);
    stepDistanceText = itemView.findViewById(R.id.stepDistanceText);
    stepPrimaryText = itemView.findViewById(R.id.stepPrimaryText);
    stepSecondaryText = itemView.findViewById(R.id.stepSecondaryText);
    instructionLayoutText = itemView.findViewById(R.id.instructionLayoutText);
    initInstructionAutoSize();
  }

  /**
   * Called after we bind the views, this will allow the step instruction {@link TextView}
   * to automatically re-size based on the length of the text.
   */
  private void initInstructionAutoSize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepPrimaryText,
      26, 28, 1, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepSecondaryText,
      20, 26, 1, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepDistanceText,
      16, 20, 1, TypedValue.COMPLEX_UNIT_SP);
  }
}
