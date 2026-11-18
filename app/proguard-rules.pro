# Reglas de R8 para la versión de lanzamiento (minifyEnabled + shrinkResources).
# Room, WorkManager, Glide y AndroidX ya incluyen sus propias reglas.

# Trazas de error legibles en los reportes de fallos
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Google Drive (respaldo en la nube) ---
# google-api-client rellena los modelos (File, FileList...) por reflexión usando los campos @Key
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# Clases opcionales que estas bibliotecas referencian pero que no existen en Android
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn com.google.appengine.**
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn org.apache.http.**
-dontwarn org.ietf.jgss.**
-dontwarn org.joda.time.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
