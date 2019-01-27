package com.mapbox.services.android.navigation.ui.v5.instruction.turnlane;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.mapbox.api.directions.v5.models.BannerComponents;

/**
 * Use this view to render turn lane data.
 * <p>
 * Based on the data provided, a turn lane will render.  It's opacity will be
 * determined by whether or not the lane is "active".
 */
public class TurnLaneView extends AppCompatImageView {

  private static final float HALF_OPACITY = 0.4f;
  private static final float FULL_OPACITY = 1.0f;
  private static final int SCALE_FLIPPED = -1;
  private static final int SCALE_NORMAL = 1;
  private final TurnLaneDrawableMap laneDrawableMap = new TurnLaneDrawableMap();

  public TurnLaneView(Context context) {
    super(context);
  }

  public TurnLaneView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public TurnLaneView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Updates this view based on the banner component lane data and the given maneuver
   * modifier (to highlight which lane should be chosen).
   *
   * @param lane             data {@link BannerComponents}
   * @param maneuverModifier for the given maneuver
   */
  public void updateLaneView(@NonNull BannerComponents lane, @NonNull String maneuverModifier) {
    if (hasInvalidData(lane)) {
      return;
    }

    TurnLaneViewData drawData = buildTurnLaneViewData(lane, maneuverModifier);
    Integer resId = findDrawableResId(drawData);
    if (resId == null) {
      return;
    }
    drawFor(lane, drawData, resId);
  }

  private boolean hasInvalidData(@NonNull BannerComponents lane) {
    return lane.directions() == null || lane.active() == null;
  }

  @NonNull
  private TurnLaneViewData buildTurnLaneViewData(@NonNull BannerComponents lane, @NonNull String maneuverModifier) {
    StringBuilder builder = new StringBuilder();
    for (String indication : lane.directions()) {
      builder.append(indication);
    }
    String laneIndications = builder.toString();
    return new TurnLaneViewData(laneIndications, maneuverModifier);
  }

  @Nullable
  private Integer findDrawableResId(TurnLaneViewData drawData) {
    String drawMethod = drawData.getDrawMethod();
    Integer resId = laneDrawableMap.get(drawMethod);
    if (resId == null) {
      return null;
    }
    return resId;
  }

  private void drawFor(@NonNull BannerComponents lane, TurnLaneViewData drawData, Integer resId) {
    final Drawable turnLaneDrawable = VectorDrawableCompat.create(
      getResources(), resId, getContext().getTheme()
    );
    setImageDrawable(turnLaneDrawable);
    setAlpha(!lane.active() ? HALF_OPACITY : FULL_OPACITY);
    setScaleX(drawData.shouldBeFlipped() ? SCALE_FLIPPED : SCALE_NORMAL);
  }
}
