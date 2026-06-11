# The SDK uses no reflection for its own models. Preserve public API names for
# Java consumers and stack traces while allowing internal implementation code
# to be optimized by the host application.
-keep public class com.mydrama.sdk.MyDramaSDK { public *; }
-keep public class com.mydrama.sdk.MyDramaClient { public *; }
-keep public class com.mydrama.sdk.MyDramaConfig { public *; }
-keep public class com.mydrama.sdk.SessionRegistration { public *; }
-keep public class com.mydrama.sdk.SessionCallback { public *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
