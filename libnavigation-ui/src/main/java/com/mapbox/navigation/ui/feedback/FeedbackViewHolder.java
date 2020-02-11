package com.mapbox.navigation.ui.feedback;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.ui.ThemeSwitcher;

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
