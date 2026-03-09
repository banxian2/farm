# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/guo/Library/Android/sdk/tools/proguard/proguard-android-optimize.txt

# Keep JavascriptInterface methods for WebView (CRITICAL)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebAppInterface and its members explicitly
-keep class com.farm.seeker.jsbridge.WebAppInterface {
    public *;
}

# Keep Solana libraries (covering all related packages)
-keep class com.solana.** { *; }
-keep class com.solanamobile.** { *; }

# Keep Crypto libraries (EdDSA)
-keep class net.i2p.crypto.** { *; }

# Keep Funkatronics (multimult) if used
-keep class io.github.funkatronics.** { *; }

# Keep Serialization classes
-keepattributes *Annotation*, Signature
-keepclassmembers class kotlinx.serialization.** { *; }

# Keep generic JNI/Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep View constructors for layout inflation
-keepclassmembers class * extends android.view.View {
   public <init>(android.content.Context);
   public <init>(android.content.Context, android.util.AttributeSet);
   public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Fragment constructors
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# Keep Activity constructors
-keep public class * extends android.app.Activity {
    public <init>();
}

# Keep data classes used in JSON serialization/deserialization
# (Adjust package if models are moved)
-keep class com.farm.seeker.model.** { *; }

# Rules to suppress warnings for missing classes (from R8 missing_rules.txt)
-dontwarn com.ditchoom.buffer.BufferFactoryJvm
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn sun.security.x509.X509Key
