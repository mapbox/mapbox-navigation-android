package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;

import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_RIGHT_ONLY;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_STRAIGHT_ONLY;
import static com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneViewData.DRAW_LANE_UTURN;

public class TurnLaneView extends View {

  private TurnLaneViewData drawData;
  private PointF size;
  private boolean isActive;
  private int primaryColor;
  private int secondaryColor;

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
    setLayerType(LAYER_TYPE_SOFTWARE, null);
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
      LanesStyleKit.drawLaneStraight(canvas, primaryColor, size);
      return;
    }

    if (drawData == null || TextUtils.isEmpty(drawData.getDrawMethod())) {
      return;
    }

    switch (drawData.getDrawMethod()) {
      case DRAW_LANE_STRAIGHT:
        LanesStyleKit.drawLaneStraight(canvas, primaryColor, size);
        break;
      case DRAW_LANE_UTURN:
        LanesStyleKit.drawLaneUturn(canvas, primaryColor, size);
        break;
      case DRAW_LANE_RIGHT:
        LanesStyleKit.drawLaneRight(canvas, primaryColor, size);
        break;
      case DRAW_LANE_SLIGHT_RIGHT:
        LanesStyleKit.drawLaneSlightRight(canvas, primaryColor, size);
        break;
      case DRAW_LANE_RIGHT_ONLY:
        LanesStyleKit.drawLaneRightOnly(canvas, primaryColor, secondaryColor, size);
        break;
      case DRAW_LANE_STRAIGHT_ONLY:
        LanesStyleKit.drawLaneStraightOnly(canvas, primaryColor, secondaryColor, size);
        break;
      default:
        LanesStyleKit.drawLaneStraight(canvas, primaryColor, size);
        break;
    }

    setAlpha(!isActive ? 0.4f : 1.0f);

    setScaleX(drawData.shouldBeFlipped() ? -1 : 1);
  }

  public void updateLaneView(@NonNull BannerComponents lane, @NonNull String maneuverModifier) {
    if (hasInvalidData(lane)) {
      return;
    }
    StringBuilder builder = new StringBuilder();
    for (String indication : lane.directions()) {
      builder.append(indication);
    }
    String laneIndications = builder.toString();
    this.drawData = new TurnLaneViewData(laneIndications, maneuverModifier);
    this.isActive = lane.active();
    invalidate();
  }

  private boolean hasInvalidData(@NonNull BannerComponents lane) {
    return lane.directions() == null || lane.active() == null;
  }

  private void initManeuverColor() {
    this.primaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
      R.attr.navigationViewBannerManeuverPrimary);
    this.secondaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
      R.attr.navigationViewBannerManeuverSecondary);
  }
}
