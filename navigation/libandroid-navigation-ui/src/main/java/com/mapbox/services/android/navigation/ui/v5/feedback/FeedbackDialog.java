package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Window;

import com.mapbox.services.android.navigation.ui.v5.R;

public class FeedbackDialog extends Dialog {

  public FeedbackDialog(@NonNull Context context) {
    super(context);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initDialog();
  }

  private void initDialog() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.feedback_dialog_layout);
  }
}
