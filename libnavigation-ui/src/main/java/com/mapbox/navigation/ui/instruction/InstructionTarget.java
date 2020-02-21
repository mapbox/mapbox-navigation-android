package com.mapbox.navigation.ui.instruction;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

import timber.log.Timber;

public class InstructionTarget implements Target {

  private TextView textView;
  private Spannable instructionSpannable;
  private List<BannerShield> shields;
  private BannerShield shield;
  private InstructionLoadedCallback instructionLoadedCallback;
  private TextViewUtils textViewUtils;

  InstructionTarget(TextView textView, Spannable instructionSpannable, List<BannerShield> shields,
                    BannerShield shield, InstructionLoadedCallback instructionLoadedCallback) {
    this(textView, instructionSpannable, shields, shield, new TextViewUtils(), instructionLoadedCallback);
  }

  private InstructionTarget(TextView textView, Spannable instructionSpannable, List<BannerShield> shields,
                            BannerShield shield, TextViewUtils textViewUtils,
                            InstructionLoadedCallback instructionLoadedCallback) {
    this.textView = textView;
    this.instructionSpannable = instructionSpannable;
    this.shields = shields;
    this.shield = shield;
    this.instructionLoadedCallback = instructionLoadedCallback;
    this.textViewUtils = textViewUtils;
  }

  BannerShield getShield() {
    return shield;
  }

  @Override
  public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    Drawable drawable = textViewUtils.createDrawable(textView, bitmap);
    createAndSetImageSpan(drawable);
    sendInstructionLoadedCallback();
  }

  @Override
  public void onBitmapFailed(Exception exception, Drawable errorDrawable) {
    setBackupText();
    sendInstructionLoadedCallback();
    Timber.e(exception);
  }

  @Override
  public void onPrepareLoad(Drawable placeHolderDrawable) {
    // no op
  }

  interface InstructionLoadedCallback {
    void onInstructionLoaded(InstructionTarget target);
  }

  private void setBackupText() {
    textView.setText(shield.getText());
  }

  private void createAndSetImageSpan(Drawable drawable) {
    instructionSpannable.setSpan(new ImageSpan(drawable),
      shield.getStartIndex(), shield.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    if (shields.indexOf(shield) == shields.size() - 1) {
      CharSequence truncatedSequence = truncateImageSpan(instructionSpannable, textView);
      textView.setText(truncatedSequence);
    }
  }

  private void sendInstructionLoadedCallback() {
    if (instructionLoadedCallback != null) {
      instructionLoadedCallback.onInstructionLoaded(this);
    }
  }

  private static CharSequence truncateImageSpan(Spannable instructionSpannable, TextView textView) {
    int availableSpace = textView.getWidth() - textView.getPaddingRight() - textView.getPaddingLeft();
    if (availableSpace > 0) {
      return TextUtils.ellipsize(instructionSpannable, textView.getPaint(), availableSpace, TextUtils.TruncateAt.END);
    } else {
      return instructionSpannable;
    }
  }
}
