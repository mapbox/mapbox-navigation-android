package com.mapbox.services.android.navigation.ui.v5.instruction.maneuver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ManeuverModifier;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ManeuverType;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN;


/**
 * A view that draws a maneuver arrow indicating the upcoming maneuver.
 *
 * @since 0.6.0
 */
public class ManeuverView extends View {

  @ManeuverType
  String maneuverType = "";
  @ManeuverModifier
  String maneuverModifier = "";

  private int primaryColor;
  private int secondaryColor;

  private float roundaboutAngle;
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

  private void initManeuverColor() {
    this.primaryColor = ThemeSwitcher.retrieveNavigationViewThemeColor(getContext(),
      R.attr.navigationViewBannerManeuverPrimary);
    this.secondaryColor = ThemeSwitcher.retrieveNavigationViewThemeColor(getContext(),
      R.attr.navigationViewBannerManeuverSecondary);
  }

  public void setManeuverType(String maneuverType) {
    if (!TextUtils.equals(this.maneuverType, maneuverType)) {
      this.maneuverType = maneuverType;
      invalidate();
    }
  }

  public void setManeuverModifier(String maneuverModifier) {
    if (!TextUtils.equals(this.maneuverModifier, maneuverModifier)) {
      this.maneuverModifier = maneuverModifier;
      invalidate();
    }
  }

  public void setRoundaboutAngle(float roundaboutAngle) {
    if (isRoundabout() && this.roundaboutAngle != roundaboutAngle) {
      this.roundaboutAngle = roundaboutAngle;
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

    if (isInEditMode()) {
      ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
      return;
    }

    if (maneuverType == null) {
      return;
    }

    boolean flip = false;

    switch (maneuverType) {
      case NavigationConstants.STEP_MANEUVER_TYPE_MERGE:
        ManeuversStyleKit.drawMerge(canvas, primaryColor, secondaryColor, size);
        flip = shouldFlip(maneuverModifier);
        break;

      case STEP_MANEUVER_TYPE_OFF_RAMP:
        ManeuversStyleKit.drawOffRamp(canvas, primaryColor, secondaryColor, size);
        flip = shouldFlip(maneuverModifier);
        break;

      case STEP_MANEUVER_TYPE_FORK:
        ManeuversStyleKit.drawFork(canvas, primaryColor, secondaryColor, size);
        flip = shouldFlip(maneuverModifier);
        break;

      case STEP_MANEUVER_TYPE_ROUNDABOUT:
      case STEP_MANEUVER_TYPE_ROUNDABOUT_TURN:
      case STEP_MANEUVER_TYPE_ROTARY:
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, 90f);
        break;

      case STEP_MANEUVER_TYPE_ARRIVE:
        if (maneuverModifier != null) {
          switch (maneuverModifier) {
            case STEP_MANEUVER_MODIFIER_RIGHT:
              ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size);
              flip = false;
              break;

            case STEP_MANEUVER_MODIFIER_LEFT:
              ManeuversStyleKit.drawArriveRight(canvas, primaryColor, size);
              flip = true;
              break;

            default:
              ManeuversStyleKit.drawArrive(canvas, primaryColor, size);
          }
        } else {
          ManeuversStyleKit.drawArrive(canvas, primaryColor, size);
        }
        break;

      default:
        if (maneuverModifier != null) {
          switch (maneuverModifier) {
            case STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
              ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size);
              flip = false;
              break;

            case STEP_MANEUVER_MODIFIER_RIGHT:
              ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size);
              flip = false;
              break;

            case STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
              ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size);
              flip = false;
              break;

            case STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
              ManeuversStyleKit.drawArrowSlightRight(canvas, primaryColor, size);
              flip = true;
              break;

            case STEP_MANEUVER_MODIFIER_LEFT:
              ManeuversStyleKit.drawArrowRight(canvas, primaryColor, size);
              flip = true;
              break;

            case STEP_MANEUVER_MODIFIER_SHARP_LEFT:
              ManeuversStyleKit.drawArrowSharpRight(canvas, primaryColor, size);
              flip = true;
              break;

            case STEP_MANEUVER_MODIFIER_UTURN:
              ManeuversStyleKit.drawArrow180Right(canvas, primaryColor, size);
              flip = true;
              break;

            default:
              ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
          }
        } else {
          ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
        }
    }
    setScaleX(flip ? -1 : 1);
  }

  private boolean shouldFlip(String modifier) {
    return modifier.contains(STEP_MANEUVER_MODIFIER_SLIGHT_LEFT)
      || modifier.contains(STEP_MANEUVER_MODIFIER_LEFT)
      || modifier.contains(STEP_MANEUVER_MODIFIER_SHARP_LEFT);
  }

  private boolean isRoundabout() {
    return maneuverType.equals(NavigationConstants.STEP_MANEUVER_TYPE_ROTARY)
      || maneuverType.equals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT)
      || maneuverType.equals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN);
  }
}
