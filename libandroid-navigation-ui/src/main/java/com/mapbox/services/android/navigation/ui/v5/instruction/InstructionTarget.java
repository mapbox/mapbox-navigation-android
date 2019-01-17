package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

import timber.log.Timber;

public class InstructionTarget implements Target {

  private TextView textView;
  private final TextViewUtils textViewUtils;
  private List<BannerShield> shields;
  private BannerShield shield;
  private InstructionLoadedCallback instructionLoadedCallback;

  InstructionTarget(TextView textView,
                    List<BannerShield> shields, BannerShield shield,
                    InstructionLoadedCallback instructionLoadedCallback) {
    this.textView = textView;
    this.shields = shields;
    this.shield = shield;
    this.instructionLoadedCallback = instructionLoadedCallback;
    this.textViewUtils = new TextViewUtils();
  }

  BannerShield getShield() {
    return shield;
  }

  @Override
  public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    createAndSetImageSpan(bitmap);
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

  private void createAndSetImageSpan(Bitmap bitmap) {
    boolean shouldTruncate = shields.indexOf(shield) == shields.size() - 1;

    textViewUtils.setImageSpan(textView, bitmap, shield.getStartIndex(), shield.getEndIndex(),
      shouldTruncate);
  }

  private void sendInstructionLoadedCallback() {
    if (instructionLoadedCallback != null) {
      instructionLoadedCallback.onInstructionLoaded(this);
    }
  }
}
