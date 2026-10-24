# kotlinx.serialization: keep the generated serializers of the app's data classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.veyraeditor.flights.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.veyraeditor.flights.**$$serializer { *; }
