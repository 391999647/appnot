# Kuikly framework keep rules
-keep class com.tencent.kuikly.core.android.KuiklyCoreEntry { *; }
-keep class com.tencent.kuikly.core.IKuiklyCoreEntry { *; }
-keep class com.tencent.kuikly.core.IKuiklyCoreEntry$Delegate { *; }
-keep class com.tencent.kuikly.core.log.KLog { *; }
-keep class com.tencent.kuikly.core.nvi.serialization.json.JSONObject { *; }
-keep class com.tencent.kuikly.core.module.RouterModule { *; }
-keep class com.tencent.kuikly.core.pager.Pager { *; }
-keep class com.tencent.kuikly.core.render.** { *; }

# Keep Kuikly annotations
-keep @interface com.tencent.kuikly.core.annotations.** { *; }

# Keep all page classes annotated with @Page
-keep @com.tencent.kuikly.core.annotations.Page class * { *; }

# Keep App classes
-keep class com.noteapp.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }

# Security crypto
-keep class androidx.security.crypto.** { *; }

# Tink / Google crypto annotations (not required at runtime)
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn javax.annotation.concurrent.GuardedBy
