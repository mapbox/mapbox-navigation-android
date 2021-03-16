package com.mapbox.navigation.ui.speed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleObserver;

import com.mapbox.navigation.base.speed.model.SpeedLimit;
import com.mapbox.navigation.base.speed.model.SpeedLimitSign;
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit;
import com.mapbox.navigation.ui.R;

import static android.graphics.Typeface.BOLD;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

public class SpeedLimitView extends ConstraintLayout implements LifecycleObserver {

  private static final double KILO_MILES_FACTOR = 0.621371;
  private static final float RADIUS = 10f;
  private static final int VIENNA_MUTCD_BORDER_INSET = 4;
  private static final int MUTCD_INNER_BACKGROUND_INSET = 7;
  private static final int VIENNA_INNER_BACKGROUND_INSET = 16;
  private static final int VIENNA_MUTCD_OUTER_BACKGROUND_INSET = 0;
  private TextView speedLimitText;
  private ImageView speedLimitView;
  private ConstraintLayout speedView;
  private int speedLimitBackgroundColor;
  private int speedLimitMutcdBorderColor;
  private int speedLimitViennaBorderColor;

  public SpeedLimitView(Context context) {
    this(context, null);
  }

  public SpeedLimitView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public SpeedLimitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(getContext(), R.layout.layout_speed_limit, this);
    initAttributes(attrs);
  }

  /**
   * Invoke to update speed limit view to show appropriate speed limit text considering the sign and unit.
   * @param speedLimit object storing speed limit data
   */
  public void setSpeedLimit(@NonNull SpeedLimit speedLimit) {
    Integer speedLimitKmph = speedLimit.getSpeedKmph();
    if (speedLimitKmph != null) {
      SpeedLimitSign speedLimitSign = speedLimit.getSpeedLimitSign();
      speedLimitView.setImageDrawable(getSpeedDrawable(speedLimitSign));
      SpeedLimitUnit speedLimitUnit = speedLimit.getSpeedLimitUnit();
      String speed = getSpeedLimit(speedLimitSign, speedLimitUnit, speedLimitKmph);
      int speedLength = speed.length();
      SpannableStringBuilder span = new SpannableStringBuilder(speed);
      span.setSpan(new StyleSpan(BOLD), 0, speedLength, SPAN_EXCLUSIVE_EXCLUSIVE);
      span.setSpan(new RelativeSizeSpan(1.9f), speedLength - 2, speedLength, 0);
      speedLimitText.setText(span);
    }
  }

  /**
   * Invoke to show speed limit indicator.
   */
  public void show() {
    speedView.setVisibility(VISIBLE);
  }

  /**
   * Invoke to hide speed limit indicator.
   */
  public void hide() {
    speedView.setVisibility(GONE);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
  }

  private void bind() {
    speedView = findViewById(R.id.speedView);
    speedLimitView = findViewById(R.id.speedLimit);
    speedLimitText = findViewById(R.id.speedLimitText);
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.MapboxStyleSpeedLimitView);
    speedLimitMutcdBorderColor = typedArray.getColor(R.styleable.MapboxStyleSpeedLimitView_speedLimitMutcdBorderColor,
        getResources().getColor(R.color.mapbox_speed_limit_view_mutcd_border));
    speedLimitViennaBorderColor = typedArray.getColor(R.styleable.MapboxStyleSpeedLimitView_speedLimitViennaBorderColor,
        getResources().getColor(R.color.mapbox_speed_limit_view_vienna_border));
    speedLimitBackgroundColor = typedArray.getColor(R.styleable.MapboxStyleSpeedLimitView_speedLimitBackgroundColor,
        getResources().getColor(R.color.mapbox_speed_limit_view_background));
    typedArray.recycle();
  }

  private GradientDrawable backgroundDrawable(int shape) {
    GradientDrawable background = new GradientDrawable();
    background.setColor(speedLimitBackgroundColor);
    background.setShape(shape);
    if (shape == GradientDrawable.RECTANGLE) {
      background.setCornerRadius(RADIUS);
    }
    return background;
  }

  private GradientDrawable borderDrawable(int shape) {
    GradientDrawable border = new GradientDrawable();
    border.setShape(shape);
    if (shape == GradientDrawable.RECTANGLE) {
      border.setCornerRadius(RADIUS);
      border.setColor(speedLimitMutcdBorderColor);
    } else {
      border.setColor(speedLimitViennaBorderColor);
    }
    return border;
  }

  private LayerDrawable getSpeedDrawable(SpeedLimitSign speedLimitSign) {
    LayerDrawable layerDrawable;
    if (speedLimitSign == SpeedLimitSign.MUTCD) {
      GradientDrawable[] layers = {
          backgroundDrawable(GradientDrawable.RECTANGLE),
          borderDrawable(GradientDrawable.RECTANGLE),
          backgroundDrawable(GradientDrawable.RECTANGLE)
      };
      layerDrawable = new LayerDrawable(layers);
      layerDrawable.setLayerInset(
          0,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET
      );
      layerDrawable.setLayerInset(
          1,
          VIENNA_MUTCD_BORDER_INSET,
          VIENNA_MUTCD_BORDER_INSET,
          VIENNA_MUTCD_BORDER_INSET,
          VIENNA_MUTCD_BORDER_INSET
      );
      layerDrawable.setLayerInset(
          2,
          MUTCD_INNER_BACKGROUND_INSET,
          MUTCD_INNER_BACKGROUND_INSET,
          MUTCD_INNER_BACKGROUND_INSET,
          MUTCD_INNER_BACKGROUND_INSET
      );
    } else {
      GradientDrawable[] layers = {
          backgroundDrawable(GradientDrawable.OVAL),
          borderDrawable(GradientDrawable.OVAL),
          backgroundDrawable(GradientDrawable.OVAL)
      };
      layerDrawable = new LayerDrawable(layers);
      layerDrawable.setLayerInset(
          0,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET,
          VIENNA_MUTCD_OUTER_BACKGROUND_INSET
      );
      layerDrawable.setLayerInset(
          1,
          VIENNA_MUTCD_BORDER_INSET,
          VIENNA_MUTCD_BORDER_INSET,
          VIENNA_MUTCD_BORDER_INSET,
          VIENNA_MUTCD_BORDER_INSET
      );
      layerDrawable.setLayerInset(
          2,
          VIENNA_INNER_BACKGROUND_INSET,
          VIENNA_INNER_BACKGROUND_INSET,
          VIENNA_INNER_BACKGROUND_INSET,
          VIENNA_INNER_BACKGROUND_INSET
      );
    }
    return layerDrawable;
  }

  @SuppressLint("DefaultLocale")
  private String getSpeedLimit(SpeedLimitSign sign, SpeedLimitUnit unit, int speedLimitKmph) {
    if (sign == SpeedLimitSign.MUTCD) {
      if (unit == SpeedLimitUnit.KILOMETRES_PER_HOUR) {
        return getContext().getString(R.string.max_speed, speedLimitKmph);
      } else {
        double speed = 5 * (Math.round((speedLimitKmph * KILO_MILES_FACTOR) / 5));
        String formattedSpeed = String.format("%.0f", speed);
        return getContext().getString(R.string.max_speed, Integer.parseInt(formattedSpeed));
      }
    } else {
      if (unit == SpeedLimitUnit.KILOMETRES_PER_HOUR) {
        return String.valueOf(speedLimitKmph);
      } else {
        double speed = 5 * (Math.round((speedLimitKmph * KILO_MILES_FACTOR) / 5));
        return String.format("%.0f", speed);
      }
    }
  }
}
