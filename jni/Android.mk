LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libscreenz
LOCAL_SRC_FILES := screenz.cpp Graphics.cpp
LOCAL_C_INCLUDES += \
	bionic \
	bionic/libstdc++ \
	frameworks/native/include \
	frameworks/native/include/gui \
	libcore/include \
	frameworks/base/core/jni/android/graphics \
	external/skia/include/core \
	external/skia/include/images

#LOCAL_LDLIBS    := -lcutils -lutils -lbinder -llog -lui -lgui -lskia -lnativehelper
LOCAL_SHARED_LIBRARIES := libcutils libutils libbinder liblog libui libgui libskia libnativehelper

include $(BUILD_SHARED_LIBRARY)
