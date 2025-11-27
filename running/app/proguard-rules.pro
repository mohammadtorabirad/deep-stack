# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep DJI SDK classes
-keep class dji.v5.** { *; }
-keep interface dji.v5.** { *; }
-keep enum dji.v5.** { *; }

# Keep DJI SDK KeyValue classes
-keep class dji.sdk.keyvalue.** { *; }
-keep interface dji.sdk.keyvalue.** { *; }
-keep enum dji.sdk.keyvalue.** { *; }
