package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.directions.v5.models.IntersectionLanes;

import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;

public class TurnLaneView extends View {

  private String laneIndications;
  private String maneuverModifier;

  private int primaryColor;
  private int secondaryColor;
  private PointF size;

  public TurnLaneView(Context context) {
    super(context);
  }

  public TurnLaneView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public TurnLaneView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    initManeuverColor();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (size == null) {
      size = new PointF(getMeasuredWidth(), getMeasuredHeight());
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (isInEditMode()) {
      TurnLaneStyleKit.drawLaneRight(canvas, primaryColor);
      return;
    }

    boolean flip = false;

    // TODO call correct stylekit method here

    setScaleX(flip ? -1 : 1);
  }

  public void setLaneIndications(@NonNull List<IntersectionLanes> lanes) {
    StringBuilder builder = new StringBuilder();
    for (String indication : lanes.indications()) {
      builder.append(indication);
    }
  }

  public void setManeuverModifier(@NonNull String maneuverModifier) {
    this.maneuverModifier = maneuverModifier;
  }

  private boolean shouldFlip(String modifier) {
    return modifier.contains(STEP_MANEUVER_MODIFIER_SLIGHT_LEFT)
      || modifier.contains(STEP_MANEUVER_MODIFIER_LEFT)
      || modifier.contains(STEP_MANEUVER_MODIFIER_SHARP_LEFT);
  }

  private void initManeuverColor() {
    this.primaryColor = ThemeSwitcher.retrieveNavigationViewBannerManeuverPrimaryColor(getContext());
    this.secondaryColor = ThemeSwitcher.retrieveNavigationViewBannerManeuverSecondaryColor(getContext());
  }
}
