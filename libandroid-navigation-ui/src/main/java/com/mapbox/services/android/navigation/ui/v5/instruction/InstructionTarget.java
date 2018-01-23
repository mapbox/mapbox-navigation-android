package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
  private List<BannerShieldInfo> shields;
  private BannerShieldInfo shield;
  private InstructionLoadedCallback instructionLoadedCallback;

  InstructionTarget(TextView textView, Spannable instructionSpannable,
                    List<BannerShieldInfo> shields, BannerShieldInfo shield,
                    InstructionLoadedCallback instructionLoadedCallback) {
    this.textView = textView;
    this.instructionSpannable = instructionSpannable;
    this.shields = shields;
    this.shield = shield;
    this.instructionLoadedCallback = instructionLoadedCallback;
  }

  BannerShieldInfo getShield() {
    return shield;
  }

  @Override
  public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    // Create a new Drawable with intrinsic bounds
    Drawable drawable = new BitmapDrawable(textView.getContext().getResources(), bitmap);

    // width == (right - left), and height == (bottom - top)
    int bottom = textView.getLineHeight();
    int right = bottom * bitmap.getWidth() / bitmap.getHeight();
    drawable.setBounds(0, 0, right, bottom);

    // Create and set a new ImageSpan at the given index with the Drawable
    instructionSpannable.setSpan(new ImageSpan(drawable),
      shield.getStartIndex(), shield.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    // Check if last in array, if so, set the text with the spannable
    if (shields.indexOf(shield) == shields.size() - 1) {
      // Make sure cut-off images aren't displayed at the end of the spannable
      CharSequence truncatedSequence = truncateImageSpan(instructionSpannable, textView);
      textView.setText(truncatedSequence);
    }
    sendInstructionLoadedCallback();
  }

  @Override
  public void onBitmapFailed(Drawable errorDrawable) {
    // Set the backup text
    textView.setText(shield.getText());
    sendInstructionLoadedCallback();
    Timber.e("Shield bitmap failed to load.");
  }

  @Override
  public void onPrepareLoad(Drawable placeHolderDrawable) {

  }

  interface InstructionLoadedCallback {
    void onInstructionLoaded(InstructionTarget target);
  }

  private void sendInstructionLoadedCallback() {
    if (instructionLoadedCallback != null) {
      instructionLoadedCallback.onInstructionLoaded(this);
    }
  }

  private static CharSequence truncateImageSpan(Spannable instructionSpannable, TextView textView) {
    int availableSpace = textView.getWidth() - textView.getPaddingRight() - textView.getPaddingLeft();
    return TextUtils.ellipsize(instructionSpannable, textView.getPaint(), availableSpace, TextUtils.TruncateAt.END);
  }
}
