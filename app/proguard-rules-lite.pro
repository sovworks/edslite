-dontobfuscate
-optimizations !code/allocation/variable,!code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

-keep public class com.sovworks.eds.android.EdsApplication { public *; }

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep public class com.sovworks.eds.fs.FileSystem
-keep public class com.sovworks.eds.fs.Path
-keep public class com.sovworks.eds.fs.RandomAccessIO
-keep interface com.sovworks.eds.settings.Settings

-keep public class com.sovworks.eds.android.views.GestureImageView$NavigListener
-keep public class com.sovworks.eds.android.views.GestureImageView$OptimImageRequiredListener

-keepclassmembers class * implements java.io.Externalizable {
    public void readExternal(java.io.ObjectInput);
    public void writeExternal(java.io.ObjectOutput);
}

-keep class android.support.** { *; }

-keep class org.apache.** { *; }

-dontwarn org.apache.**
-dontwarn javax.servlet.**

-dontwarn android.support.**
-dontnote android.support.**

-dontwarn java.awt.*
