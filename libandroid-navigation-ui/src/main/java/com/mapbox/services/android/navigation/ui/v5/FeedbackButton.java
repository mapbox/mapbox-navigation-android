package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.AsyncLayoutInflater;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class FeedbackButton extends FrameLayout implements NavigationButton {

  private MultiOnClickListener multiOnClickListener = new MultiOnClickListener();
  private FloatingActionButton feedbackFab;
  private CustomLayoutUpdater layoutUpdater;

  public FeedbackButton(@NonNull Context context) {
    super(context);
    initialize(context);
  }

  public FeedbackButton(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize(context);
  }

  public FeedbackButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context);
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
   * Hides the button
   */
  @Override
  public void hide() {
    setVisibility(GONE);
  }

  /**
   * Shows the button
   */
  @Override
  public void show() {
    setVisibility(VISIBLE);
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

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
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

  private void initialize(Context context) {
    inflate(context, R.layout.feedback_button_layout, this);
  }

  private void bind() {
    feedbackFab = findViewById(R.id.feedbackFab);
  }

  private void setupOnClickListeners() {
    feedbackFab.setOnClickListener(multiOnClickListener);
  }

  private void clearListeners() {
    multiOnClickListener.clearListeners();
    multiOnClickListener = null;
    setOnClickListener(null);
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
