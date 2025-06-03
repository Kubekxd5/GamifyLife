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

# Keep model classes for Firebase Firestore
-keepclassmembers class com.example.gamifylife.models.Achievement {
    @com.google.firebase.firestore.DocumentId <fields>; # Zachowaj pola z adnotacją DocumentId
    <init>(); # Zachowaj konstruktory
    public <fields>; # Zachowaj publiczne pola
    public <methods>; # Zachowaj publiczne metody
}
-keep class com.example.gamifylife.models.Achievement { *; } # Zachowaj całą klasę

# Zrób to samo dla UserProfile i innych modeli Firestore
-keepclassmembers class com.example.gamifylife.models.UserProfile {
    <init>();
    public <fields>;
    public <methods>;
}
-keep class com.example.gamifylife.models.UserProfile { *; }

# Ogólna reguła dla klas z adnotacjami Firebase (ostrożnie, może być zbyt szeroka)
# -keepattributes Signature
# -keepattributes *Annotation*
# -keepnames @com.google.firebase.firestore.PropertyName class *
# -keepnames @com.google.firebase.firestore.DocumentId class *