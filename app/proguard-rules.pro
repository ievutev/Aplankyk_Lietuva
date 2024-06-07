# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in Cbuild.gradle.
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

# Preserve class members that are accessed by reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
    public <fields>;
    public <methods>;
}

# Keep annotations
-keepattributes *Annotation*

# Keep all classes extending specific classes
-keep class * extends androidx.appcompat.app.AppCompatActivity
-keep class * extends android.app.Service
-keep class * extends androidx.lifecycle.ViewModel

# Keep Firebase model classes
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <methods>;
}

# Keep classes with specific annotations
-keepclasseswithmembernames class * {
    native <methods>;
}

# Add any other necessary rules specific to your app and libraries