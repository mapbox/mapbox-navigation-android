package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InstructionLoaderTest {

  @Test
  public void testPriorityInfoIsAdded() {
    TextView textView = mock(TextView.class);
    BannerText bannerText = mock(BannerText.class);
    InstructionImageLoader instructionImageLoader = mock(InstructionImageLoader.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .abbreviationPriority(1)
      .abbreviation("abbreviation text")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);
    when(bannerText.components()).thenReturn(bannerComponentsList);

    new InstructionLoader(textView, bannerText, instructionImageLoader, abbreviationCoordinator);

    verify(abbreviationCoordinator).addPriorityInfo(bannerComponents, 0);
  }

  @Test
  public void testShieldInfoIsAdded() {
    TextView textView = mock(TextView.class);
    BannerText bannerText = mock(BannerText.class);
    InstructionImageLoader instructionImageLoader = mock(InstructionImageLoader.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .imageBaseUrl("string url")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);
    when(bannerText.components()).thenReturn(bannerComponentsList);

    new InstructionLoader(textView, bannerText, instructionImageLoader, abbreviationCoordinator);

    verify(instructionImageLoader).addShieldInfo(bannerComponents, 0);
  }

  @Test
  public void testSetAbbreviatedText() {
    TextView textView = mock(TextView.class);
    BannerText bannerText = mock(BannerText.class);
    InstructionImageLoader instructionImageLoader = mock(InstructionImageLoader.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .abbreviationPriority(1)
      .abbreviation("abbrv text")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);
    when(bannerText.components()).thenReturn(bannerComponentsList);
    String abbreviatedText = "abbreviated text";
    when(abbreviationCoordinator.abbreviateBannerText(any(List.class), any(TextView.class))).thenReturn(abbreviatedText);
    InstructionLoader instructionLoader = new InstructionLoader(textView, bannerText, instructionImageLoader, abbreviationCoordinator);

    instructionLoader.loadInstruction();

    verify(textView).setText(abbreviatedText);
  }

  @Test
  public void testLoadImages() {
    TextView textView = mock(TextView.class);
    BannerText bannerText = mock(BannerText.class);
    InstructionImageLoader instructionImageLoader = mock(InstructionImageLoader.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .imageBaseUrl("string url")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);
    when(bannerText.components()).thenReturn(bannerComponentsList);
    String abbreviatedText = "abbreviated text";
    when(abbreviationCoordinator.abbreviateBannerText(any(List.class), any(TextView.class))).thenReturn(abbreviatedText);
    InstructionLoader instructionLoader = new InstructionLoader(textView, bannerText, instructionImageLoader, abbreviationCoordinator);

    instructionLoader.loadInstruction();

    verify(instructionImageLoader).loadImages(any(TextView.class), any(List.class));
  }
}