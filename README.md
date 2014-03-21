Experimented with various aspects of Android, including gestures, sensors,
button overrides, frame buffer, JNI, etc.

This is from a couple of years ago, but I just updated it for Kitkat and
decided to push to Github for posterity.

Screen capture only works on select rooted devices, and the JNI portion
only builds as part of the AOSP platform (*not* with the NDK, so don't
be fooled by the directory structure). The apk should be push under
/system/app and the .so under /system/lib.
 