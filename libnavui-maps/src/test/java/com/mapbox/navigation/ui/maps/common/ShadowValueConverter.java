package com.mapbox.navigation.ui.maps.common;

import androidx.annotation.NonNull;
import com.mapbox.bindgen.Expected;
import com.mapbox.bindgen.ExpectedFactory;
import com.mapbox.bindgen.Value;
import com.mapbox.common.ValueConverter;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(ValueConverter.class)
public class ShadowValueConverter {

  @Implementation
  @NonNull
  public static Expected<Value, String> fromJson(@NonNull String var0) {
    return ExpectedFactory.createValue(new Value(var0));
  }

  @Implementation
  @NonNull
  public static String toJson(@NonNull Value var0) {
    return "";
  }

}
