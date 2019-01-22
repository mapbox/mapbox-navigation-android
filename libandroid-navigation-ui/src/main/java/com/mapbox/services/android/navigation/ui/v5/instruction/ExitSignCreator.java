package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
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

  ExitSignCreator() {
    super(new ExitSignVerifier());
    textViewUtils = new TextViewUtils();
  }

  @Override
  BannerComponentNode setupNode(BannerComponents components, int index, int startIndex, String
    modifier) {
    if (components.type().equals("exit")) {
      return null;
    } else if (components.type().equals("exit-number")) {
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

      TextView exitSignView;

      if (modifier.equals("left")) {
        exitSignView = (TextView) inflater.inflate(R.layout.exit_sign_view_left, root, false);
      } else {
        exitSignView = (TextView) inflater.inflate(R.layout.exit_sign_view_right, root, false);
      }

      exitSignView.setText(exitNumber);

      textViewUtils.setImageSpan(textView, exitSignView, startIndex, startIndex + exitNumber
        .length());
    }
  }
}
