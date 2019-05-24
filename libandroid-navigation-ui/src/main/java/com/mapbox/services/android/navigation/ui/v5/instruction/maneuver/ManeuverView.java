package com.mapbox.services.android.navigation.ui.v5.instruction.maneuver;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ManeuverModifier;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ManeuverType;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT;
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
      add(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT);
      add(STEP_MANEUVER_TYPE_EXIT_ROTARY);
    }
  };
  private static final Set<String> MANEUVER_TYPES_WITH_NULL_MODIFIERS = new HashSet<String>() {
    {
      add(STEP_MANEUVER_TYPE_OFF_RAMP);
      add(STEP_MANEUVER_TYPE_FORK);
      add(STEP_MANEUVER_TYPE_ROUNDABOUT);
      add(STEP_MANEUVER_TYPE_ROUNDABOUT_TURN);
      add(STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT);
      add(STEP_MANEUVER_TYPE_ROTARY);
      add(STEP_MANEUVER_TYPE_EXIT_ROTARY);
    }
  };

  @ManeuverType
  private String maneuverType = null;
  @ManeuverModifier
  private String maneuverModifier = null;
  @ColorInt
  private int primaryColor;
  @ColorInt
  private int secondaryColor;
  private float roundaboutAngle = DEFAULT_ROUNDABOUT_ANGLE;
  private Pair<String, String> maneuverTypeAndModifier = new Pair<>(null, null);
  private PointF size;
  private String drivingSide = STEP_MANEUVER_MODIFIER_RIGHT;

  /**
   * A custom view that can be used with the Mapbox Directions API.
   * <p>
   * By providing a {@link String} maneuver type and maneuver modifier, the
   * corresponding maneuver icon will be rendered in this view.
   *
   * @param context to use when creating a view from code
   */
  public ManeuverView(Context context) {
    super(context);
  }

  /**
   * A custom view that can be used with the Mapbox Directions API.
   * <p>
   * By providing a {@link String} maneuver type and maneuver modifier, the
   * corresponding maneuver icon will be rendered in this view.
   *
   * @param context for inflating a view from XML
   * @param attrs   for inflating a view from XML
   */
  public ManeuverView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initializeColorFrom(attrs);
  }

  /**
   * A custom view that can be used with the Mapbox Directions API.
   * <p>
   * By providing a {@link String} maneuver type and maneuver modifier, the
   * corresponding maneuver icon will be rendered in this view.
   *
   * @param context  for inflation from XML and apply a class-specific base style
   * @param attrs    for inflation from XML and apply a class-specific base style
   * @param defStyle for inflation from XML and apply a class-specific base style
   */
  public ManeuverView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initializeColorFrom(attrs);
  }

  /**
   * Updates the maneuver type and modifier which determine how this view will
   * render itself.
   * <p>
   * If determined the provided maneuver type and modifier will render a new image,
   * the view will invalidate and redraw itself with the new data.
   *
   * @param maneuverType     to determine the maneuver icon to render
   * @param maneuverModifier to determine the maneuver icon to render
   */
  public void setManeuverTypeAndModifier(@NonNull String maneuverType, @Nullable String maneuverModifier) {
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

  /**
   * Updates the angle to render the roundabout maneuver.
   * <p>
   * This value will only be considered if the current maneuver type is
   * a roundabout.
   *
   * @param roundaboutAngle angle to be rendered
   */
  public void setRoundaboutAngle(@FloatRange(from = 60f, to = 300f) float roundaboutAngle) {
    if (ROUNDABOUT_MANEUVER_TYPES.contains(maneuverType) && this.roundaboutAngle != roundaboutAngle) {
      updateRoundaboutAngle(roundaboutAngle);
      invalidate();
    }
  }

  public void setDrivingSide(String drivingSide) {
    if (STEP_MANEUVER_MODIFIER_LEFT.equals(drivingSide) || STEP_MANEUVER_MODIFIER_RIGHT.equals(drivingSide)) {
      updateDrivingSide(drivingSide);
      invalidate();
    }
  }

  /**
   * Updates maneuver view primary color.
   * <p>
   * The {@link ManeuverView} will be invalidated and redrawn
   * with the new color provided.
   *
   * @param primaryColor to be set
   */
  public void setPrimaryColor(@ColorInt int primaryColor) {
    this.primaryColor = primaryColor;
    invalidate();
  }

  /**
   * Updates maneuver view secondary color.
   * <p>
   * The {@link ManeuverView} will be invalidated and redrawn
   * with the new color provided.
   *
   * @param secondaryColor to be set
   */
  public void setSecondaryColor(@ColorInt int secondaryColor) {
    this.secondaryColor = secondaryColor;
    invalidate();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    setLayerType(LAYER_TYPE_SOFTWARE, null);
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
    if (drivingSide.equals(STEP_MANEUVER_MODIFIER_RIGHT)) {
      boolean flip = SHOULD_FLIP_MODIFIERS.contains(maneuverModifier);
      setScaleX(flip ? -1 : 1);
    }
  }

  private void initializeColorFrom(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.ManeuverView);
    primaryColor = typedArray.getColor(R.styleable.ManeuverView_maneuverViewPrimaryColor,
      ContextCompat.getColor(getContext(), R.color.mapbox_navigation_view_color_banner_maneuver_primary));
    secondaryColor = typedArray.getColor(R.styleable.ManeuverView_maneuverViewSecondaryColor,
      ContextCompat.getColor(getContext(), R.color.mapbox_navigation_view_color_banner_maneuver_secondary));
    typedArray.recycle();
  }

  private boolean isNewTypeOrModifier(String maneuverType, String maneuverModifier) {
    if (maneuverType == null) {
      return false;
    }
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

  private void updateDrivingSide(String drivingSide) {
    this.drivingSide = drivingSide;
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
