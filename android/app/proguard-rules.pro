# ── ONNX Runtime ──────────────────────────────────────────────────────────────
# Keep the JNI bridge and public API — ProGuard can't trace native calls.
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ── sqlite-vec (JNI) ─────────────────────────────────────────────────────────
# The native lib is loaded via System.loadLibrary("vec0").
# Keep any Java-side glue if we add a wrapper later.
-keepclassmembers class * {
    native <methods>;
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ── Google Play Billing ───────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ── Data classes used with Room (keep constructor + fields) ───────────────────
-keepclassmembers class com.stashed.app.data.local.** {
    <init>(...);
    <fields>;
}

# ── General Android ───────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
