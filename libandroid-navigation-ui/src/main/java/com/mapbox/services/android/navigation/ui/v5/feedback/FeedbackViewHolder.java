package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;

class FeedbackViewHolder extends RecyclerView.ViewHolder {

  private ImageView feedbackImage;
  private TextView feedbackText;

  FeedbackViewHolder(View itemView) {
    super(itemView);
    feedbackImage = itemView.findViewById(R.id.feedbackImage);
    feedbackText = itemView.findViewById(R.id.feedbackText);
    initTextColor(feedbackText);
  }

  void setFeedbackImage(int feedbackImageId) {
    feedbackImage.setImageDrawable(AppCompatResources.getDrawable(feedbackImage.getContext(), feedbackImageId));
  }

  void setFeedbackText(String feedbackText) {
    this.feedbackText.setText(feedbackText);
  }

  private void initTextColor(TextView feedbackText) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int navigationViewSecondaryColor = ThemeSwitcher.retrieveThemeColor(feedbackText.getContext(),
        R.attr.navigationViewSecondary);
      // Text color
      feedbackText.setTextColor(navigationViewSecondaryColor);
    }
  }
}
