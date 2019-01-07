package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;

public class InstructionTextView extends LinearLayout {
  private TextView instructionText;
  private TextView exitView;


  public InstructionTextView(Context context) {
    this(context, null);
  }

  public InstructionTextView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public InstructionTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(attrs);
  }

  private void initialize(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.instruction_text_view, this);
    instructionText = findViewById(R.id.instruction_text);
    exitView = findViewById(R.id.exit_view);
    setupAttributes(attrs);
  }

  private void setupAttributes(@Nullable AttributeSet attrs) {
    TypedArray styledAttributes = getContext().getTheme().obtainStyledAttributes(
      attrs, R.styleable.InstructionTextView, 0, 0);

    try {
      // todo add minHeight and lineSpacingMultiplier
      float textSize = styledAttributes.getDimension(R.styleable.InstructionTextView_textSize, -1);
      textSize = textSize / getResources().getDisplayMetrics().scaledDensity;

      if (textSize > -1) {
        instructionText.setTextSize(textSize);
      }

      ColorStateList color = styledAttributes.getColorStateList(R.styleable.InstructionTextView_textColor);
      if (color != null) {
        instructionText.setTextColor(color);
      }
    } finally {
      styledAttributes.recycle();
    }
  }

  public void setInstructionText(String text) {
    instructionText.setText(text);
  }

  public void showExitView(String text) {
    exitView.setText(text);
    exitView.setVisibility(VISIBLE);
  }

  public void hideExitView() {
    exitView.setVisibility(GONE);
    exitView.setText("");
  }

  public float getFullWidth() {
    measure(0, 0);
    return getMeasuredWidth();
  }

  public void setMaxLines(int maxLines) {
    instructionText.setMaxLines(maxLines);
  }

  public boolean textFits(String text) { //todo make sure this works
    Paint paint = new Paint(instructionText.getPaint());
    float width = paint.measureText(text);
    return width < instructionText.getWidth();
  }

  public TextView getInstructionTextView() {
    return instructionText;
  }
}
