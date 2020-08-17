package com.mapbox.navigation.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet;

/**
 * Button that can be used to inflate the {@link FeedbackBottomSheet}.
 * @see #addOnClickListener(OnClickListener)
 */
public class FeedbackButton extends ConstraintLayout implements NavigationButton {
  private FloatingActionButton feedbackFab;
  @Nullable
  private MultiOnClickListener multiOnClickListener = new MultiOnClickListener();

  private int primaryColor;
  private int secondaryColor;

  public FeedbackButton(Context context) {
    this(context, null);
  }

  public FeedbackButton(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public FeedbackButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initAttributes(attrs);
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
   * Use it to update the view style.
   *
   * @param styleRes style resource
   */
  @Override
  public void updateStyle(@StyleRes int styleRes) {
    TypedArray typedArray = getContext().obtainStyledAttributes(styleRes, R.styleable.MapboxStyleFeedbackButton);

    primaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.MapboxStyleFeedbackButton_feedbackButtonPrimaryColor, R.color.mapbox_feedback_button_primary));
    secondaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.MapboxStyleFeedbackButton_feedbackButtonSecondaryColor,
            R.color.mapbox_feedback_button_secondary));

    typedArray.recycle();

    applyAttributes();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    applyAttributes();
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
    feedbackFab.setOnClickListener(multiOnClickListener);
  }

  private void clearListeners() {
    multiOnClickListener.clearListeners();
    multiOnClickListener = null;
    setOnClickListener(null);
  }

  private void initialize(Context context) {
    inflate(context, R.layout.mapbox_button_feedback, this);
  }

  private void bind() {
    feedbackFab = findViewById(R.id.feedbackFab);
  }

  private void applyAttributes() {
    feedbackFab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    feedbackFab.setColorFilter(secondaryColor);
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.MapboxStyleFeedbackButton);

    primaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(R.styleable.MapboxStyleFeedbackButton_feedbackButtonPrimaryColor,
            R.color.mapbox_feedback_button_primary));
    secondaryColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(R.styleable.MapboxStyleFeedbackButton_feedbackButtonSecondaryColor,
            R.color.mapbox_feedback_button_secondary));

    typedArray.recycle();
  }
}
