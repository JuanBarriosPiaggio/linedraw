# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.linedraw.game.**$$serializer { *; }
-keepclassmembers class com.linedraw.game.** {
    *** Companion;
}
-keepclasseswithmembers class com.linedraw.game.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google Play Billing
-keep class com.android.vending.billing.** { *; }
