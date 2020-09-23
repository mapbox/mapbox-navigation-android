-verbose

# We should at least keep the Exceptions, InnerClasses,
# and Signature attributes when processing a library.
-keepattributes Exceptions,InnerClasses,Signature,RuntimeVisibleParameterAnnotations,RuntimeVisibleAnnotations

-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
