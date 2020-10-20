package com.mapbox.navigation.ui.instruction.maneuver;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.mapbox.api.directions.v5.models.ManeuverModifier;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.utils.internal.maneuver.ManeuverIconDrawer;
import com.mapbox.navigation.utils.internal.maneuver.ManeuverIconHelper;
import com.mapbox.navigation.utils.internal.maneuver.ManeuversStyleKit;

import static com.mapbox.navigation.utils.internal.maneuver.ManeuverIconHelper.MANEUVER_ICON_DRAWER_MAP;
import static com.mapbox.navigation.utils.internal.maneuver.ManeuverIconHelper.MANEUVER_TYPES_WITH_NULL_MODIFIERS;
import static com.mapbox.navigation.utils.internal.maneuver.ManeuverIconHelper.ROUNDABOUT_MANEUVER_TYPES;


/**
 * A view that draws a maneuver arrow indicating the upcoming maneuver.
 */
public class ManeuverView extends View {

  @Nullable
  @StepManeuver.StepManeuverType
  private String maneuverType = null;
  @Nullable
  @ManeuverModifier.Type
  private String maneuverModifier = null;
  @ColorInt
  private int primaryColor;
  @ColorInt
  private int secondaryColor;
  private float roundaboutAngle = ManeuverIconHelper.DEFAULT_ROUNDABOUT_ANGLE;
  @Nullable
  private Pair<String, String> maneuverTypeAndModifier = new Pair<>(null, null);
  private PointF size;
  private String drivingSide = ManeuverModifier.RIGHT;

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
    if (ManeuverModifier.LEFT.equals(drivingSide) || ManeuverModifier.RIGHT.equals(drivingSide)) {
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
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    // TODO Abstract this "debug" code somehow?
    if (isInEditMode()) {
      ManeuversStyleKit.drawArrowStraight(canvas, primaryColor, size);
      return;
    }

    if (maneuverType == null) {
      return;
    }

    ManeuverIconDrawer maneuverIconDrawer = MANEUVER_ICON_DRAWER_MAP.get(maneuverTypeAndModifier);
    if (maneuverIconDrawer != null) {
      maneuverIconDrawer.drawManeuverIcon(canvas, primaryColor, secondaryColor, size, roundaboutAngle);
    }

    boolean flip = ManeuverIconHelper.isManeuverIconNeedFlip(maneuverType, maneuverModifier, drivingSide);
    setScaleX(flip ? -1 : 1);
  }

  private void initializeColorFrom(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.MapboxStyleManeuverView);
    primaryColor = typedArray.getColor(R.styleable.MapboxStyleManeuverView_maneuverViewPrimaryColor,
      ContextCompat.getColor(getContext(), R.color.mapbox_navigation_view_color_banner_maneuver_primary));
    secondaryColor = typedArray.getColor(R.styleable.MapboxStyleManeuverView_maneuverViewSecondaryColor,
      ContextCompat.getColor(getContext(), R.color.mapbox_navigation_view_color_banner_maneuver_secondary));
    typedArray.recycle();
  }

  private boolean isNewTypeOrModifier(@Nullable String maneuverType, String maneuverModifier) {
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
  private String checkManeuverModifier(@NonNull String maneuverType, @Nullable String maneuverModifier) {
    if (!maneuverType.contentEquals(StepManeuver.ARRIVE) && maneuverModifier != null) {
      maneuverType = null;
    }
    return maneuverType;
  }

  private void updateRoundaboutAngle(float roundaboutAngle) {
    this.roundaboutAngle = ManeuverIconHelper.adjustRoundaboutAngle(roundaboutAngle);
  }

  private void updateDrivingSide(String drivingSide) {
    this.drivingSide = drivingSide;
  }
}
