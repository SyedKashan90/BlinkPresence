# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Retrofit / OkHttp / Gson models
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.smartattendance.app.data.remote.dto.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
