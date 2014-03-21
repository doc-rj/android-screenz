LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/jones/screenz/IScreenZProvider.aidl

LOCAL_PACKAGE_NAME := screenz
LOCAL_CERTIFICATE := platform

LOCAL_REQUIRED_MODULES  := libscreenz

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
