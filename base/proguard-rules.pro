# region Retrofit
# Retrofit consumer rules are not sufficient for R8 full mode.
# Since Retrofit is delivered with mapbox-java and mapbox-java is not an Android library,
# therefore it can't include consumer proguard rules, we include them here, as this module uses mapbox-java as a transitive dependency
# and this is the base module for all other modules in the NavSDK.
# Rules are copied from https://github.com/square/retrofit/blob/trunk/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# endregion Retrofit