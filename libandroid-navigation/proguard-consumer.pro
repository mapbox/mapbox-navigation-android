# Consumer proguard rules for libandroid-navigation

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Java ---
-dontwarn java.awt.Color

# --- AutoValue ---
# AutoValue annotations are retained but dependency is compileOnly.
-dontwarn com.google.auto.value.**

# --- Navigator ---
-keep class com.mapbox.navigator.** { *; }

# --- Telemetry ---
-keep class com.mapbox.android.telemetry.** { *; }