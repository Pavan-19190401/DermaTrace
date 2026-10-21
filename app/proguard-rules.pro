# DermaTrace Proguard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.dermatrace.app.network.** { *; }
