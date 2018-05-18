package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InstructionLoaderTest {

  @Test
  public void onInstructionLoaderCreated_priorityInfoIsAdded() {
    TextView textView = mock(TextView.class);
    ImageCoordinator imageCoordinator = mock(ImageCoordinator.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .abbreviationPriority(1)
      .abbreviation("abbreviation text")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);

    new InstructionLoader(textView, bannerComponentsList, imageCoordinator, abbreviationCoordinator);

    verify(abbreviationCoordinator).addPriorityInfo(bannerComponents, 0);
  }

  @Test
  public void onInstructionLoaderCreated_shieldInfoIsAdded() {
    TextView textView = mock(TextView.class);
    ImageCoordinator imageCoordinator = mock(ImageCoordinator.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .imageBaseUrl("string url")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);

    new InstructionLoader(textView, bannerComponentsList, imageCoordinator, abbreviationCoordinator);

    verify(imageCoordinator).addShieldInfo(bannerComponents, 0);
  }

  @Test
  public void onLoadInstruction_textIsAbbreviated() {
    TextView textView = mock(TextView.class);
    ImageCoordinator imageCoordinator = mock(ImageCoordinator.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .abbreviationPriority(1)
      .abbreviation("abbrv text")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);
    String abbreviatedText = "abbreviated text";
    when(abbreviationCoordinator.abbreviateBannerText(any(List.class), any(TextView.class))).thenReturn(abbreviatedText);
    InstructionLoader instructionLoader = new InstructionLoader(textView, bannerComponentsList, imageCoordinator, abbreviationCoordinator);

    instructionLoader.loadInstruction();

    verify(textView).setText(abbreviatedText);
  }

  @Test
  public void onLoadInstruction_imagesAreLoaded() {
    TextView textView = mock(TextView.class);
    ImageCoordinator imageCoordinator = mock(ImageCoordinator.class);
    AbbreviationCoordinator abbreviationCoordinator = mock(AbbreviationCoordinator.class);
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents()
      .imageBaseUrl("string url")
      .build();
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    bannerComponentsList.add(bannerComponents);
    String abbreviatedText = "abbreviated text";
    when(abbreviationCoordinator.abbreviateBannerText(any(List.class), any(TextView.class))).thenReturn(abbreviatedText);
    InstructionLoader instructionLoader = new InstructionLoader(textView, bannerComponentsList, imageCoordinator, abbreviationCoordinator);

    instructionLoader.loadInstruction();

    verify(imageCoordinator).loadImages(any(TextView.class), any(List.class));
  }
}