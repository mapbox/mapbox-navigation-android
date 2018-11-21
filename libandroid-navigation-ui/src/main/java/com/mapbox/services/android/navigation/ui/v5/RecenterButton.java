package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.AsyncLayoutInflater;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

/**
 * Button used to re-activate following user location during navigation.
 * <p>
 * If a user scrolls the map while navigating, the
 * {@link com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet}
 * is set to hidden and this button is shown.
 * <p>
 * This button uses a custom {@link TranslateAnimation} with {@link OvershootInterpolator}
 * to be shown.
 *
 * @since 0.6.0
 */
public class RecenterButton extends FrameLayout implements NavigationButton {

  private MultiOnClickListener multiOnClickListener = new MultiOnClickListener();
  private CustomLayoutUpdater layoutUpdater;
  private Animation slideUpBottom;

  public RecenterButton(@NonNull Context context) {
    super(context);
    initialize();
  }

  public RecenterButton(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public RecenterButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  /**
   * Sets visibility to VISIBLE and starts custom animation.
   *
   * @since 0.6.0
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
   *
   * @since 0.6.0
   */
  @Override
  public void hide() {
    if (getVisibility() == VISIBLE) {
      setVisibility(INVISIBLE);
    }
  }

  /**
   * Replace this component with a pre-built {@link View}.
   *
   * @param view to be used in place of the component.
   */
  @Override
  public void replaceWith(View view) {
    CustomLayoutUpdater layoutUpdater = retrieveLayoutUpdater();
    layoutUpdater.update(this, view);
  }

  /**
   * Replace this component with a layout resource ID.  The component
   * will inflate and add the layout once it is ready.
   *
   * @param layoutResId to be inflated and added
   * @param listener    to notify when the replacement is finished
   */
  @Override
  public void replaceWith(int layoutResId, OnLayoutReplacedListener listener) {
    CustomLayoutUpdater layoutUpdater = retrieveLayoutUpdater();
    layoutUpdater.update(this, layoutResId, listener);
  }

  /**
   * Once inflation of the view has finished,
   * create the custom animation.
   */
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    initializeAnimation();
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
    setOnClickListener(multiOnClickListener);
  }

  private void clearListeners() {
    multiOnClickListener.clearListeners();
    multiOnClickListener = null;
    setOnClickListener(null);
  }

  /**
   * Inflates the layout.
   */
  private void initialize() {
    inflate(getContext(), R.layout.recenter_btn_layout, this);
  }

  /**
   * Creates the custom animation used to show this button.
   */
  private void initializeAnimation() {
    slideUpBottom = new TranslateAnimation(0f, 0f, 125f, 0f);
    slideUpBottom.setDuration(300);
    slideUpBottom.setInterpolator(new OvershootInterpolator(2.0f));
  }

  private CustomLayoutUpdater retrieveLayoutUpdater() {
    if (layoutUpdater != null) {
      return layoutUpdater;
    }
    AsyncLayoutInflater layoutInflater = new AsyncLayoutInflater(getContext());
    layoutUpdater = new CustomLayoutUpdater(layoutInflater);
    return layoutUpdater;
  }
}
