package com.mapbox.navigation.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.libnavigation.ui.R;

/**
 * Button used to re-activate following user location during navigation.
 * <p>
 * If a user scrolls the map while navigating, the
 * {@link com.mapbox.navigation.ui.summary.SummaryBottomSheet}
 * is set to hidden and this button is shown.
 * <p>
 * This button uses a custom {@link TranslateAnimation} with {@link OvershootInterpolator}
 * to be shown.
 */
public class RecenterButton extends ConstraintLayout implements NavigationButton {
  private MultiOnClickListener multiOnClickListener = new MultiOnClickListener();
  private Animation slideUpBottom;

  private FloatingActionButton recenterFab;

  private int primaryColor;
  private int secondaryColor;

  public RecenterButton(Context context) {
    this(context, null);
  }

  public RecenterButton(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public RecenterButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initAttributes(attrs);
    initialize(context);
  }

  /**
   * Sets visibility to VISIBLE and starts custom animation.
   */
  @Override
  public void show() {
    if (getVisibility() == INVISIBLE) {
      setVisibility(VISIBLE);
      startAnimation(slideUpBottom);
    }
  }

  /**
   * Adds an onClickListener to the button
   *
   * @param onClickListener to add
   */
  @Override
  public void addOnClickListener(OnClickListener onClickListener) {
    multiOnClickListener.addListener(onClickListener);
  }

  /**
   * Removes an onClickListener from the button
   *
   * @param onClickListener to remove
   */
  @Override
  public void removeOnClickListener(OnClickListener onClickListener) {
    multiOnClickListener.removeListener(onClickListener);
  }

  /**
   * Sets visibility to INVISIBLE.
   */
  @Override
  public void hide() {
    if (getVisibility() == VISIBLE) {
      setVisibility(INVISIBLE);
    }
  }

  /**
   * Use it to update the view style
   *
   * @param styleRes style resource
   */
  @Override
  public void updateStyle(int styleRes) {
    TypedArray typedArray = getContext().obtainStyledAttributes(styleRes, R.styleable.RecenterButton);

    primaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.RecenterButton_recenterButtonPrimaryColor, R.color.mapbox_recenter_button_primary));
    secondaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.RecenterButton_recenterButtonSecondaryColor, R.color.mapbox_recenter_button_secondary));

    typedArray.recycle();

    applyAttributes();
  }

  /**
   * Once inflation of the view has finished,
   * create the custom animation.
   */
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    applyAttributes();
    initAnimation();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    setupOnClickListeners();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    clearListeners();
  }

  private void setupOnClickListeners() {
    recenterFab.setOnClickListener(multiOnClickListener);
  }

  private void clearListeners() {
    multiOnClickListener.clearListeners();
    multiOnClickListener = null;
    recenterFab.setOnClickListener(null);
  }

  /**
   * Inflates the layout.
   */
  private void initialize(Context context) {
    inflate(context, R.layout.recenter_button_layout, this);
  }

  private void bind() {
    recenterFab = findViewById(R.id.recenterFab);
  }

  private void applyAttributes() {
    recenterFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    recenterFab.setColorFilter(secondaryColor);
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.RecenterButton);
    primaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(R.styleable.RecenterButton_recenterButtonPrimaryColor,
            R.color.mapbox_recenter_button_primary));
    secondaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(R.styleable.RecenterButton_recenterButtonSecondaryColor,
            R.color.mapbox_recenter_button_secondary));

    typedArray.recycle();
  }

  /**
   * Creates the custom animation used to show this button.
   */
  private void initAnimation() {
    slideUpBottom = new TranslateAnimation(0f, 0f, 125f, 0f);
    slideUpBottom.setDuration(300);
    slideUpBottom.setInterpolator(new OvershootInterpolator(2.0f));
  }
}
