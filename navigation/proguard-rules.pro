# --- Navigation SDK Telemetry Events ---
-keep class com.mapbox.navigation.core.telemetry.events.** {*;}

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- com.mapbox.api.directions.v5.MapboxDirections ---
-dontwarn com.sun.xml.internal.ws.spi.db.BindingContextFactory

# please remove this workaround after update of auto-value-gson with the fix https://github.com/mapbox/auto-value-gson/pull/3
# use test case from https://github.com/mapbox/mapbox-navigation-android/pull/6021 to verify that a new config from dependecy works
-keepclassmembers class com.mapbox.auto.value.gson.SerializableJsonElement {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}
