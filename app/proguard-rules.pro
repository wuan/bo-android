# Blitzortung Lightning Monitor - ProGuard Rules
# This file contains R8/ProGuard rules to prevent code shrinking issues

# ===== Debugging Support =====
# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for reflection
-keepattributes *Annotation*

# ===== Data Classes (JSON Parsing) =====
# These classes are serialized/deserialized from JSON via org.json APIs
# Must keep all fields and their names for JSON parsing to work
-keep class org.blitzortung.android.data.beans.** { *; }
-keepclassmembers class org.blitzortung.android.data.beans.** { *; }

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== Dagger 2 Dependency Injection =====
# Keep all Dagger generated classes
-keep class **Dagger*.* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter

# Keep Dagger annotations
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
}

# Keep Dagger modules and components
-keep @dagger.Module class * { *; }
-keep @dagger.Component class * { *; }
-keep @dagger.Subcomponent class * { *; }

# ===== AndroidX and Android Components =====
# Keep AndroidX classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Keep native methods (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep view constructors for XML inflation
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
    *** get*();
}

# ===== OSMDroid Maps Library =====
# Keep OSMDroid classes for map functionality
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep map overlay classes
-keep class * extends org.osmdroid.views.overlay.Overlay { *; }

# ===== Enum Support =====
# Keep enum methods that are accessed via reflection
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ===== Work Manager =====
# Keep Worker classes
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ===== Services and Receivers =====
# Keep Service and BroadcastReceiver classes
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep AppService and BootReceiver
-keep class org.blitzortung.android.app.AppService { *; }
-keep class org.blitzortung.android.app.BootReceiver { *; }

# ===== Preferences =====
# Keep SharedPreferences related classes
-keep class * extends android.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# ===== Fragment Support =====
-keep class * extends androidx.fragment.app.Fragment { *; }
-keep class * extends android.app.Fragment { *; }

# ===== Event/Protocol Classes =====
# Keep event classes that are passed around via ConsumerContainer
-keep class org.blitzortung.android.protocol.Event { *; }
-keep class * implements org.blitzortung.android.protocol.Event { *; }
-keep class org.blitzortung.android.data.provider.result.** { *; }
-keep class org.blitzortung.android.alert.event.** { *; }
-keep class org.blitzortung.android.location.LocationEvent { *; }

# ===== Location Providers =====
# Keep location provider classes
-keep class org.blitzortung.android.location.provider.** { *; }

# ===== Alert Classes =====
# Keep alert-related classes that use reflection or serialization
-keep class org.blitzortung.android.alert.AlertResult { *; }
-keep class org.blitzortung.android.alert.AlertParameters { *; }
-keep class org.blitzortung.android.alert.data.** { *; }

# ===== Widget Provider =====
# Keep widget provider for home screen widgets
-keep class org.blitzortung.android.app.WidgetProvider { *; }

# ===== Kotlin Specific =====
# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== Remove Logging in Release =====
# Strip out logging calls for smaller APK (optional - comment out if you want logs)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===== General Android Rules =====
# Keep Activity onCreate methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Keep R class and fields
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep application class
-keep class org.blitzortung.android.app.BOApplication { *; }

# ===== Warnings to Ignore =====
# Suppress warnings for missing classes from optional dependencies
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# ===== Optimization =====
# Allow R8 to optimize (already enabled with proguard-android-optimize.txt)
-optimizationpasses 5
-allowaccessmodification

# Don't obfuscate for easier debugging (comment out for more obfuscation)
# -dontobfuscate
