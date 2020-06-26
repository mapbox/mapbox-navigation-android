# --- Navigation SDK Telemetry Events ---
-keep class com.mapbox.navigation.core.telemetry.events.** {*;}

# --- Navigation SDK configurable modules provided via reflection ---
-keep class com.mapbox.navigation.route.hybrid.internal.MapboxHybridRouter {*;}
-keep class com.mapbox.navigation.route.onboard.internal.MapboxOnboardRouter {*;}
-keep class com.mapbox.navigation.route.offboard.internal.MapboxOffboardRouter {*;}
-keep class com.mapbox.navigation.trip.notification.internal.MapboxTripNotification {*;}

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- com.mapbox.api.directions.v5.MapboxDirections ---
-dontwarn com.sun.xml.internal.ws.spi.db.BindingContextFactory
