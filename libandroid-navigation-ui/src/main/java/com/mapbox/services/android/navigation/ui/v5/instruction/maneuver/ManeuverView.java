package com.mapbox.services.android.navigation.ui.v5.instruction.maneuver;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;
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
  String maneuverType = null;

  @ManeuverModifier
  String maneuverModifier = null;
  int primaryColor = Color.BLACK;
  int secondaryColor = Color.LTGRAY;
  private float roundaboutAngle;
  private PointF size;

  public ManeuverView(Context context) {
    super(context);
    init(null, 0);
  }

  public ManeuverView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public ManeuverView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  private void init(AttributeSet attrs, int defStyle) {
    final TypedArray styledAttributes = getContext().obtainStyledAttributes(
      attrs, R.styleable.ManeuverView, defStyle, 0);

    this.primaryColor = styledAttributes.getColor(R.styleable.ManeuverView_primaryColor, Color.BLACK);
    this.secondaryColor = styledAttributes.getColor(R.styleable.ManeuverView_secondaryColor, Color.LTGRAY);

    styledAttributes.recycle();
  }

  public void setManeuverType(String maneuverType) {
    this.maneuverType = maneuverType;
    invalidate();

  }

  public void setManeuverModifier(String maneuverModifier) {
    this.maneuverModifier = maneuverModifier;
    invalidate();
  }

  public void setRoundaboutAngle(float roundaboutAngle) {
    this.roundaboutAngle = roundaboutAngle;
    invalidate();
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
      ManeuversStyleKit.drawArrow0(canvas, primaryColor, size);
      return;
    }

    if (maneuverType == null || maneuverModifier == null) {
      return;
    }

    boolean flip = false;

    switch (maneuverType) {
      case NavigationConstants.STEP_MANEUVER_TYPE_MERGE:
        ManeuversStyleKit.drawMerge(canvas, primaryColor, secondaryColor, size);
        flip = shouldFlip(maneuverModifier);
        break;

      case STEP_MANEUVER_TYPE_OFF_RAMP:
        ManeuversStyleKit.drawOfframp(canvas, primaryColor, secondaryColor, size);
        flip = shouldFlip(maneuverModifier);
        break;

      case STEP_MANEUVER_TYPE_FORK:
        ManeuversStyleKit.drawFork(canvas, primaryColor, secondaryColor, size);
        flip = shouldFlip(maneuverModifier);
        break;

      case STEP_MANEUVER_TYPE_ROUNDABOUT:
      case STEP_MANEUVER_TYPE_ROUNDABOUT_TURN:
      case STEP_MANEUVER_TYPE_ROTARY:
        ManeuversStyleKit.drawRoundabout(canvas, primaryColor, secondaryColor, size, 90);
        break;

      case STEP_MANEUVER_TYPE_ARRIVE:
        switch (maneuverModifier) {
          case STEP_MANEUVER_MODIFIER_RIGHT:
            ManeuversStyleKit.drawArriveright2(canvas, primaryColor, size);
            flip = false;
            break;

          case STEP_MANEUVER_MODIFIER_LEFT:
            ManeuversStyleKit.drawArriveright2(canvas, primaryColor, size);
            flip = true;
            break;

          default:
            ManeuversStyleKit.drawArriveright2(canvas, primaryColor, size);
        }
        break;

      default:
        switch (maneuverModifier) {
          case STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
            ManeuversStyleKit.drawArrow30(canvas, primaryColor, size);
            flip = false;
            break;

          case STEP_MANEUVER_MODIFIER_RIGHT:
            ManeuversStyleKit.drawArrow45(canvas, primaryColor, size);
            flip = false;
            break;

          case STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
            ManeuversStyleKit.drawArrow75(canvas, primaryColor, size);
            flip = false;
            break;

          case STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
            ManeuversStyleKit.drawArrow30(canvas, primaryColor, size);
            flip = true;
            break;

          case STEP_MANEUVER_MODIFIER_LEFT:
            ManeuversStyleKit.drawArrow45(canvas, primaryColor, size);
            flip = true;
            break;

          case STEP_MANEUVER_MODIFIER_SHARP_LEFT:
            ManeuversStyleKit.drawArrow75(canvas, primaryColor, size);
            flip = true;
            break;

          case STEP_MANEUVER_MODIFIER_UTURN:
            ManeuversStyleKit.drawArrow180(canvas, primaryColor, size);
            break;

          default:
            ManeuversStyleKit.drawArrow0(canvas, primaryColor, size);
        }
    }

    setScaleX(flip ? -1 : 1);
  }

  private boolean shouldFlip(String modifier) {
    return modifier.contains(STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT)
      || modifier.contains(STEP_MANEUVER_MODIFIER_RIGHT)
      || modifier.contains(STEP_MANEUVER_MODIFIER_SHARP_RIGHT);
  }
}
