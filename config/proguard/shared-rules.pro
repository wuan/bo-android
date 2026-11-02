# Blitzortung Lightning Monitor - shared R8/ProGuard rules
#
# This is the single rule set used by both the phone (:app) and automotive
# (:automotive) release builds (design D9). Module-specific rules, if ever
# needed, belong in that module's small `proguard-rules.pro`, which is applied
# after this file.

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
# Dagger 2 KSP-generated code is referenced statically (e.g. DaggerAppComponent),
# so R8 keeps what is used; nothing reflectively instantiates the graph. The
# **_Factory / **_MembersInjector / **Dagger*.* keeps are unnecessary, as are the
# legacy Dagger 1 relics (dagger.internal.Binding / ModuleAdapter).

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
# AndroidX ships its own consumer rules for its reflection (fragment
# constructors, ListenableWorker, startup Initializer, window SidecarCompat,
# kotlinx.serialization, media Parcelable). The app relies on those plus AGP's
# defaults and its own constructor keeps below instead of retaining the whole
# namespace.
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
}

# ===== OSMDroid Maps Library =====
# The app reaches into OSMDroid by name in exactly two places, both
# private-field reads (design D2). Everything else is a normal static reference.
#   - map/OwnMapView.kt reads MapController.mCurrentAnimator
#   - map/MapFragment.kt reads ScaleBarOverlay.centred
-keepclassmembers class org.osmdroid.views.MapController {
    private android.animation.Animator mCurrentAnimator;
}
-keepclassmembers class org.osmdroid.views.overlay.ScaleBarOverlay {
    private boolean centred;
}
-dontwarn org.osmdroid.**

# ===== Enum Support =====
# Keep enum methods that are accessed via reflection
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
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
-keep class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# ===== Fragment Support =====
# Fragments are created by name by FragmentManager/FragmentFactory and from XML
# <fragment> declarations, so their class names must survive. Keep constructors
# only; the AndroidX fragment consumer rules and R8 handle the rest.
-keep class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# ===== Event/Protocol Classes =====
# Plain classes passed around via ConsumerContainer's statically typed callbacks;
# there is no reflection, so keep the class names only.
-keep class org.blitzortung.android.data.provider.result.**
-keep class org.blitzortung.android.location.LocationEvent

# ===== Location Providers =====
# Providers are selected statically by LocationProviderFactory; keep names only.
-keep class org.blitzortung.android.location.provider.**

# ===== Alert Classes =====
# Plain data holders created and consumed in code; keep names only.
-keep class org.blitzortung.android.alert.AlertParameters
-keep class org.blitzortung.android.alert.data.**

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

# ===== Keep Logging in Release =====
# Logging is intentionally retained in release builds so that user-submitted
# log reports (LogDialog) and on-device diagnosis contain app information.
# Do not re-add an `-assumenosideeffects class android.util.Log` block here.

# ===== General Android Rules =====
# Keep Activity onCreate methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
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
