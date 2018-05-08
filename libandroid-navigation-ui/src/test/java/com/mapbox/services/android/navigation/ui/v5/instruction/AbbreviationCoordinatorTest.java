package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.ui.v5.utils.TextViewUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbbreviationCoordinatorTest extends BaseTest {
  @Test
  public void textIsAbbreviatedWhenItDoesntFit() {
    String abbreviation = "smtxt";
    BannerComponents bannerComponents =
      BannerComponentsFaker.bannerComponents()
        .abbreviation(abbreviation)
        .abbreviationPriority(0)
        .build();
    TextViewUtils textViewUtils = mock(TextViewUtils.class);
    TextView textView = mock(TextView.class);
    when(textViewUtils.textFits(textView, abbreviation)).thenReturn(true);
    when(textViewUtils.textFits(textView, bannerComponents.text())).thenReturn(false);

    AbbreviationCoordinator abbreviationCoordinator = new AbbreviationCoordinator(textViewUtils);
    abbreviationCoordinator.addPriorityInfo(bannerComponents, 0);

    List<InstructionLoader.BannerComponentNode> bannerComponentNodes = new ArrayList<>();
    bannerComponentNodes.add(new AbbreviationCoordinator.AbbreviationNode(bannerComponents, 0));

    assertEquals(abbreviation, abbreviationCoordinator.abbreviateBannerText(bannerComponentNodes, textView));
  }

  @Test
  public void textIsNotAbbreviatedWhenItDoesFit() {
    String abbreviation = "smtxt";
    String text = "some text";

    BannerComponents bannerComponents =
      BannerComponentsFaker.bannerComponents()
        .abbreviation(abbreviation)
        .abbreviationPriority(0)
        .text(text)
        .build();
    TextViewUtils textViewUtils = mock(TextViewUtils.class);
    TextView textView = mock(TextView.class);
    when(textViewUtils.textFits(textView, bannerComponents.text())).thenReturn(true);

    AbbreviationCoordinator abbreviationCoordinator = new AbbreviationCoordinator(textViewUtils);
    abbreviationCoordinator.addPriorityInfo(bannerComponents, 0);

    List<InstructionLoader.BannerComponentNode> bannerComponentNodes = new ArrayList<>();
    bannerComponentNodes.add(new AbbreviationCoordinator.AbbreviationNode(bannerComponents, 0));

    assertEquals(text, abbreviationCoordinator.abbreviateBannerText(bannerComponentNodes, textView));
  }
}
