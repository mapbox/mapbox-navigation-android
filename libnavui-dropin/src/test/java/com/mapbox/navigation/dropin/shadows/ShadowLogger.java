package com.mapbox.navigation.dropin.shadows;

import android.util.Log;

import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.logging.Logger;

import javax.annotation.Nullable;

@Implements(Logger.class)
public class ShadowLogger {

    @Implementation
    public static void e(@Nullable String tag, @NonNull String message) {
        Log.e(message, tag);
    }

    @Implementation
    public static void w(@Nullable String tag, @NonNull String message) {
        Log.w(message, tag);
    }

    @Implementation
    public static void i(@Nullable String tag, @NonNull String message) {
        Log.i(message, tag);
    }
}
