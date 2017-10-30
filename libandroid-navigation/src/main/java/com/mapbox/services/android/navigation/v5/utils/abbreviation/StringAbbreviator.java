package com.mapbox.services.android.navigation.v5.utils.abbreviation;

import android.text.TextUtils;

public class StringAbbreviator {

  private static final String REGEX_IGNORE_CASE_START = "(?i)\\b";
  private static final String REGEX_IGNORE_CASE_END = "\\b";
  private static final String REGEX_COLON_SEMICOLON_COMMA = "[:;,]";

  public static String abbreviate(String inputStr) {
    if (!TextUtils.isEmpty(inputStr)) {
      if (inputStr.length() < 25) {
        return inputStr;
      }

      AbbreviationArray abbreviations = new AbbreviationArray();
      for (int i = 0; i < abbreviations.size(); i++) {
        AbbreviationArray.Abbreviation abbv = abbreviations.get(i);
        inputStr = inputStr.replaceAll(REGEX_IGNORE_CASE_START + abbv.string + REGEX_IGNORE_CASE_END,
          abbv.abbreviatedString);
      }
    }
    return inputStr;
  }

  public static String deliminator(String inputStr) {
    return inputStr.replaceAll(REGEX_COLON_SEMICOLON_COMMA, "/");
  }

  public static String[] splitter(String inputStr) {
    return inputStr.split(REGEX_COLON_SEMICOLON_COMMA);
  }
}