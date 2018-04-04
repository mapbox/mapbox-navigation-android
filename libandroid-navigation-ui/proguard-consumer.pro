# Consumer proguard rules for libandroid-navigation-ui

# --- OkHttp ---
-dontwarn okhttp3.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Picasso ---
-dontwarn com.squareup.okhttp.**

# --- Java ---
-dontwarn java.awt.Color

# --- com.mapbox.api.directions.v5.MapboxDirections ---
-dontwarn com.sun.xml.internal.ws.spi.db.BindingContextFactory

# --- com.amazonaws.util.json.JacksonFactory ---
-dontwarn com.fasterxml.jackson.core.**
