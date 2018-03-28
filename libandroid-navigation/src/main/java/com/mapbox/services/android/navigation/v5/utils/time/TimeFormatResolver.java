package com.mapbox.services.android.navigation.v5.utils.time;


import java.util.Calendar;

interface TimeFormatResolver {
  void nextChain(TimeFormatResolver chain);

  String obtainTimeFormatted(int type, Calendar time);
}