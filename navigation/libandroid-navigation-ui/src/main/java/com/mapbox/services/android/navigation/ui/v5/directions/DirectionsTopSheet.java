package com.mapbox.services.android.navigation.ui.v5.directions;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.StepManeuver;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfHelpers;
import com.mapbox.services.commons.utils.TextUtils;

import java.text.DecimalFormat;
import java.util.Locale;

public class DirectionsTopSheet extends RelativeLayout implements ProgressChangeListener {

  private static final String DECIMAL_FORMAT = "###.#";
  private static final String MILES_STRING_FORMAT = "%s miles";
  private static final String FEET_STRING_FORMAT = "%s feet";

  private ImageView maneuverImage;
  private TextView distanceText;
  private TextView stepText;

  private Animation slideUpTop;
  private Animation slideDownTop;

  public DirectionsTopSheet(Context context) {
    this(context, null);
  }

  public DirectionsTopSheet(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public DirectionsTopSheet(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bindViews();
    initAnimations(getContext());
  }

  @Override
  public Parcelable onSaveInstanceState() {
    super.onSaveInstanceState();
    return createSavedState();
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    if (state instanceof Bundle) {
      setRestoredState((Bundle) state);
    } else {
      super.onRestoreInstanceState(state);
    }
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    distanceText.setText(distanceFormatter(routeProgress.getCurrentLegProgress()
      .getCurrentStepProgress().getDistanceRemaining()));
    LegStep upComingStep = routeProgress.getCurrentLegProgress().getUpComingStep();
    if (upComingStep != null) {
      maneuverImage.setImageResource(getManeuverResource(upComingStep));
      if (upComingStep.getManeuver() != null) {
        if (!TextUtils.isEmpty(upComingStep.getName())) {
          stepText.setText(upComingStep.getName());
        } else if (!TextUtils.isEmpty(upComingStep.getManeuver().getInstruction())) {
          stepText.setText(upComingStep.getManeuver().getInstruction());
        }
      }
    }
  }

  public void show() {
    if (this.getVisibility() == INVISIBLE) {
      this.setVisibility(VISIBLE);
      this.bringToFront();
      this.startAnimation(slideDownTop);
    }
  }

  public void hide() {
    if (this.getVisibility() == VISIBLE) {
      this.startAnimation(slideUpTop);
      this.setVisibility(INVISIBLE);
    }
  }

  private void init() {
    inflate(getContext(), R.layout.direction_top_sheet_layout, this);
  }

  private void bindViews() {
    maneuverImage = (ImageView) getRootView().findViewById(R.id.maneuverImageView);
    distanceText = (TextView) getRootView().findViewById(R.id.distanceText);
    stepText = (TextView) getRootView().findViewById(R.id.stepText);
  }

  private void initAnimations(Context context) {
    slideUpTop = AnimationUtils.loadAnimation(context, R.anim.slide_up_top);
    slideDownTop = AnimationUtils.loadAnimation(context, R.anim.slide_down_top);

    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(300);

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setStartOffset(1000);
    fadeOut.setDuration(1000);
  }

  @NonNull
  private Parcelable createSavedState() {
    Bundle state = new Bundle();
    state.putParcelable(getResources().getString(R.string.super_state), super.onSaveInstanceState());
    state.putInt(getResources().getString(R.string.view_visibility), getVisibility());
    return state;
  }

  @SuppressWarnings("WrongConstant")
  private void setRestoredState(Bundle state) {
    this.setVisibility(state.getInt(getResources().getString(R.string.view_visibility), getVisibility()));
    Parcelable superState = state.getParcelable(getResources().getString(R.string.super_state));
    super.onRestoreInstanceState(superState);
  }

  private static String distanceFormatter(double distance) {
    String formattedString;
    if (TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET) > 1099) {
      distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES);
      if (distance > 10) {
        int roundedNumber = (int) (distance / 100 * 100);
        formattedString = String.format(Locale.US, MILES_STRING_FORMAT, roundedNumber);
      } else {
        DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT);
        double roundedNumber = (distance / 100 * 100);
        formattedString = String.format(Locale.US, MILES_STRING_FORMAT, df.format(roundedNumber));
      }
    } else {
      distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET);
      int roundedNumber = ((int) Math.round(distance)) / 100 * 100;
      formattedString = String.format(Locale.US, FEET_STRING_FORMAT, roundedNumber);
    }
    return formattedString;
  }

  private static int getManeuverResource(LegStep step) {
    ManeuverMap maneuverMap = new ManeuverMap();
    if (step != null && step.getManeuver() != null) {
      StepManeuver maneuver = step.getManeuver();
      if (!TextUtils.isEmpty(maneuver.getModifier())) {
        return maneuverMap.getManeuverResource(maneuver.getType() + maneuver.getModifier());
      } else {
        return maneuverMap.getManeuverResource(maneuver.getType());
      }
    }
    return R.drawable.direction_continue_straight;
  }
}

