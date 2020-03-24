package com.mapbox.navigation.ui.feedback;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.libnavigation.ui.R;

class FeedbackViewHolder extends RecyclerView.ViewHolder {

  private ImageView feedbackImage;
  private TextView feedbackText;

  FeedbackViewHolder(View itemView, int textColor) {
    super(itemView);
    feedbackImage = itemView.findViewById(R.id.feedbackImage);
    feedbackText = itemView.findViewById(R.id.feedbackText);
    feedbackText.setTextColor(textColor);
  }

  void setFeedbackImage(int feedbackImageId) {
    feedbackImage.setImageDrawable(AppCompatResources.getDrawable(feedbackImage.getContext(), feedbackImageId));
  }

  void setFeedbackText(String feedbackText) {
    this.feedbackText.setText(feedbackText);
  }

}
