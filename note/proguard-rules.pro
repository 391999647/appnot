# Kuikly framework keep rules
-keep class com.tencent.kuikly.** { *; }
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
-keep class * annotated with com.tencent.kuikly.core.annotations.Page { *; }

# Keep App classes
-keep class com.noteapp.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }
