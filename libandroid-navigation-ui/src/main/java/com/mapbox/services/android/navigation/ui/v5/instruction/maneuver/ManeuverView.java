package com.mapbox.services.android.navigation.ui.v5.instruction.maneuver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ManeuverModifier;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ManeuverType;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
  .STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
  .STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
  .STEP_MANEUVER_TYPE_ROUNDABOUT_TURN;


/**
 * A view that draws a maneuver arrow indicating the upcoming maneuver.
 *
 * @since 0.6.0
 */
public class ManeuverView extends View {

  private static final float TOP_ROUNDABOUT_ANGLE_LIMIT = 300f;
  private static final float BOTTOM_ROUNDABOUT_ANGLE_LIMIT = 60f;
  private static final float DEFAULT_ROUNDABOUT_ANGLE = 180f;
  private static final Map<Pair<String, String>, ManeuverViewUpdate> MANEUVER_VIEW_UPDATE_MAP = new ManeuverViewMap();
  private static final Set<String> SHOULD_FLIP_MODIFIERS = new HashSet<String>() {
    {
      add(STEP_MANEUVER_MODIFIER_SLIGHT_LEFT);
      add(STEP_MANEUVER_MODIFIER_LEFT);
      add(STEP_MANEUVER_MODIFIER_SHARP_LEFT);
      add(STEP_MANEUVER_MODIFIER_UTURN);
    }
  };
  private static final Set<String> ROUNDABOUT_MANEUVER_TYPES = new HashSet<String>() {
    {
      add(STEP_MANEUVER_TYPE_ROTARY);
      add(STEP_MANEUVER_TYPE_ROUNDABOUT);
      add(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN);
    }
  };
  private static final Set<String> MANEUVER_TYPES_WITH_NULL_MODIFIERS = new HashSet<String>() {
    {
      add(STEP_MANEUVER_TYPE_OFF_RAMP);
      add(STEP_MANEUVER_TYPE_FORK);
      add(STEP_MANEUVER_TYPE_ROUNDABOUT);
      add(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN);
      add(STEP_MANEUVER_TYPE_ROTARY);
      add(STEP_MANEUVER_TYPE_EXIT_ROTARY);
    }
  };

  @ManeuverType
  private String maneuverType = null;
  @ManeuverModifier
  private String maneuverModifier = null;
  private Pair<String, String> maneuverTypeAndModifier = new Pair<>(null, null);
  private int primaryColor;
  private int secondaryColor;
  private float roundaboutAngle = DEFAULT_ROUNDABOUT_ANGLE;
  private PointF size;

  public ManeuverView(Context context) {
    super(context);
  }

  public ManeuverView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ManeuverView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    setLayerType(LAYER_TYPE_SOFTWARE, null);
    initManeuverColor();
  }

  public void setManeuverTypeAndModifier(String maneuverType, String maneuverModifier) {
    if (isNewTypeOrModifier(maneuverType, maneuverModifier)) {
      this.maneuverType = maneuverType;
      this.maneuverModifier = maneuverModifier;
      if (checkManeuverTypeWithNullModifier(maneuverType)) {
        return;
      }
      maneuverType = checkManeuverModifier(maneuverType, maneuverModifier);
      maneuverTypeAndModifier = new Pair<>(maneuverType, maneuverModifier);
      invalidate();
    }
  }

  public void setRoundaboutAngle(float roundaboutAngle) {
    if (ROUNDABOUT_MANEUVER_TYPES.contains(maneuverType) && this.roundaboutAngle != roundaboutAngle) {
      updateRoundaboutAngle(roundaboutAngle);
      invalidate();
    }
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

    // TODO Abstract this "debug" code somehow?
    if (isInEditMode()) {
      ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
      return;
    }

    if (maneuverType == null) {
      return;
    }

    ManeuverViewUpdate maneuverViewUpdate = MANEUVER_VIEW_UPDATE_MAP.get(maneuverTypeAndModifier);
    if (maneuverViewUpdate != null) {
      maneuverViewUpdate.updateManeuverView(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
    }
    boolean flip = SHOULD_FLIP_MODIFIERS.contains(maneuverModifier);
    setScaleX(flip ? -1 : 1);
  }

  private void initManeuverColor() {
    this.primaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
      R.attr.navigationViewBannerManeuverPrimary);
    this.secondaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
      R.attr.navigationViewBannerManeuverSecondary);
  }

  private boolean isNewTypeOrModifier(String maneuverType, String maneuverModifier) {
    return !TextUtils.equals(this.maneuverType, maneuverType)
      || !TextUtils.equals(this.maneuverModifier, maneuverModifier);
  }

  private boolean checkManeuverTypeWithNullModifier(String maneuverType) {
    if (MANEUVER_TYPES_WITH_NULL_MODIFIERS.contains(maneuverType)) {
      maneuverTypeAndModifier = new Pair<>(maneuverType, null);
      invalidate();
      return true;
    }
    return false;
  }

  @Nullable
  private String checkManeuverModifier(String maneuverType, String maneuverModifier) {
    if (!maneuverType.contentEquals(STEP_MANEUVER_TYPE_ARRIVE) && maneuverModifier != null) {
      maneuverType = null;
    }
    return maneuverType;
  }

  private void updateRoundaboutAngle(float roundaboutAngle) {
    if (checkRoundaboutBottomLimit(roundaboutAngle)) {
      return;
    }
    if (checkRoundaboutTopLimit(roundaboutAngle)) {
      return;
    }
    this.roundaboutAngle = roundaboutAngle;
  }

  private boolean checkRoundaboutBottomLimit(float roundaboutAngle) {
    if (roundaboutAngle < BOTTOM_ROUNDABOUT_ANGLE_LIMIT) {
      this.roundaboutAngle = BOTTOM_ROUNDABOUT_ANGLE_LIMIT;
      return true;
    }
    return false;
  }

  private boolean checkRoundaboutTopLimit(float roundaboutAngle) {
    if (roundaboutAngle > TOP_ROUNDABOUT_ANGLE_LIMIT) {
      this.roundaboutAngle = TOP_ROUNDABOUT_ANGLE_LIMIT;
      return true;
    }
    return false;
  }
}
