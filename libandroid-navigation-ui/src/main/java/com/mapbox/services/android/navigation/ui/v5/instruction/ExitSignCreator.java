package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.R;

import java.util.List;

class ExitSignCreator extends NodeCreator<BannerComponentNode, ExitSignVerifier> {
  private String exitNumber;
  private int startIndex;
  private TextViewUtils textViewUtils;
  private String modifier;
  private static final String EXIT = "exit";
  private static final String EXIT_NUMBER = "exit-number";
  private static final String LEFT = "left";

  ExitSignCreator() {
    super(new ExitSignVerifier());
    textViewUtils = new TextViewUtils();
  }

  @Override
  BannerComponentNode setupNode(BannerComponents components, int index, int startIndex,
                                String modifier) {
    if (components.type().equals(EXIT)) {
      return null;
    } else if (components.type().equals(EXIT_NUMBER)) {
      exitNumber = components.text();
      this.startIndex = startIndex;
      this.modifier = modifier;
    }

    return new BannerComponentNode(components, startIndex);
  }

  /**
   * One coordinator should override this method, and this should be the coordinator which populates
   * the textView with text.
   *
   * @param textView             to populate
   * @param bannerComponentNodes containing instructions
   */
  @Override
  void postProcess(TextView textView, List<BannerComponentNode> bannerComponentNodes) {
    if (exitNumber != null) {
      LayoutInflater inflater = (LayoutInflater) textView.getContext().getSystemService(Context
        .LAYOUT_INFLATER_SERVICE);

      ViewGroup root = (ViewGroup) textView.getParent();

      AppCompatTextView exitSignView = (AppCompatTextView) inflater.inflate(R.layout.exit_sign_view, root,
        false);

      // todo figure out why this isn't working pre-lolipop
//      VectorDrawableCompat background = VectorDrawableCompat.create(exitSignView.getResources(),
//        R.drawable.styled_rounded_corners, exitSignView.getContext().getTheme());
//
//      exitSignView.setBackgroundDrawable(background);

      if (modifier.equals(LEFT)) {
        VectorDrawableCompat left = VectorDrawableCompat.create(textView.getResources(),
          R.drawable.ic_exit_arrow_left, textView.getContext().getTheme());

        exitSignView.setCompoundDrawablesWithIntrinsicBounds(left, null, null, null);
      } else {
        VectorDrawableCompat right = VectorDrawableCompat.create(textView.getResources(),
          R.drawable.ic_exit_arrow_right, textView.getContext().getTheme());
        exitSignView.setCompoundDrawablesWithIntrinsicBounds(null, null, right, null);
      }

      exitSignView.setText(exitNumber);

      textViewUtils.setImageSpan(textView, exitSignView, startIndex, startIndex + exitNumber
        .length());
    }
  }
}
