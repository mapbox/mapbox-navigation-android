package com.mapbox.services.android.navigation.ui.v5.summary.list;

import android.content.res.Configuration;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.maneuver.ManeuverView;

class InstructionViewHolder extends RecyclerView.ViewHolder implements InstructionListView {

  private static final int PRIMARY_MIN_TEXT_SIZE_SP = 26;
  private static final int PRIMARY_MAX_TEXT_SIZE_SP = 28;
  private static final int SECONDARY_MIN_TEXT_SIZE_SP = 20;
  private static final int SECONDARY_MAX_TEXT_SIZE_SP = 26;
  private static final int DISTANCE_MIN_TEXT_SIZE_SP = 16;
  private static final int DISTANCE_MAX_TEXT_SIZE_SP = 20;
  private static final int AUTO_SIZE_STEP_GRANULARITY = 1;

  private ManeuverView maneuverView;
  private TextView distanceText;
  private TextView primaryText;
  private TextView secondaryText;
  private View instructionLayoutText;

  InstructionViewHolder(View itemView) {
    super(itemView);
    maneuverView = itemView.findViewById(R.id.maneuverView);
    distanceText = itemView.findViewById(R.id.stepDistanceText);
    primaryText = itemView.findViewById(R.id.stepPrimaryText);
    secondaryText = itemView.findViewById(R.id.stepSecondaryText);
    instructionLayoutText = itemView.findViewById(R.id.instructionLayoutText);
    initInstructionAutoSize();
  }

  @Override
  public void updateManeuverViewTypeAndModifier(String maneuverType, String maneuverModifier) {
    maneuverView.setManeuverTypeAndModifier(maneuverType, maneuverModifier);
  }

  @Override
  public void updateManeuverViewRoundaboutDegrees(float roundaboutAngle) {
    maneuverView.setRoundaboutAngle(roundaboutAngle);
  }

  @Override
  public void updateDistanceText(SpannableString distanceText) {
    this.distanceText.setText(distanceText);
  }

  @Override
  public void updatePrimaryText(String primaryText) {
    this.primaryText.setText(primaryText);
  }

  @Override
  public void updatePrimaryMaxLines(int maxLines) {
    primaryText.setMaxLines(maxLines);
  }

  @Override
  public void updateSecondaryText(String secondaryText) {
    this.secondaryText.setText(secondaryText);
  }

  @Override
  public void updateSecondaryVisibility(int visibility) {
    secondaryText.setVisibility(visibility);
  }

  @Override
  public void updateBannerVerticalBias(float percentBias) {
    adjustBannerVerticalBias(percentBias);
  }

  private void initInstructionAutoSize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(primaryText,
      PRIMARY_MIN_TEXT_SIZE_SP, PRIMARY_MAX_TEXT_SIZE_SP, AUTO_SIZE_STEP_GRANULARITY, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(secondaryText,
      SECONDARY_MIN_TEXT_SIZE_SP, SECONDARY_MAX_TEXT_SIZE_SP, AUTO_SIZE_STEP_GRANULARITY, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(distanceText,
      DISTANCE_MIN_TEXT_SIZE_SP, DISTANCE_MAX_TEXT_SIZE_SP, AUTO_SIZE_STEP_GRANULARITY, TypedValue.COMPLEX_UNIT_SP);
  }

  private void adjustBannerVerticalBias(float percentBias) {
    int orientation = itemView.getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) instructionLayoutText.getLayoutParams();
      params.verticalBias = percentBias;
      instructionLayoutText.setLayoutParams(params);
    }
  }
}
