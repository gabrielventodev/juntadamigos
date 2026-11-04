# kotlinx.serialization: conservar serializers generados de los DTO de core.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.juntadamigos.core.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.juntadamigos.core.**$$serializer { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
