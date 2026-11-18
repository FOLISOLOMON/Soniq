# SONIQ R8 / ProGuard rules.
#
# Most AndroidX, Media3, Room, Hilt and Coil artifacts ship consumer rules, so only
# project-specific rules belong here.

# Keep line numbers for readable crash traces; hide original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: navigation routes are @Serializable objects/classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.soniq.player.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.soniq.player.** {
    kotlinx.serialization.KSerializer serializer(...);
}
