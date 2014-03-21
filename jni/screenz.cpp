
#define LOG_TAG "screenz"

#include <stdio.h>

#include <jni.h>
#include <GraphicsJNI.h>
#include <binder/IMemory.h>
#include <binder/IBinder.h>

#include <gui/SurfaceComposerClient.h>
#include <gui/ISurfaceComposer.h>

#include <ui/PixelFormat.h>

#include <SkCanvas.h>
#include <SkBitmap.h>
#include <SkRegion.h>
#include <SkPixelRef.h>

#include <utils/misc.h>
#include <cutils/log.h>

//using namespace android;

static inline SkBitmap::Config convertPixelFormat(android::PixelFormat format)
{
    switch(format) {
        case android::PIXEL_FORMAT_RGB_565:
            return SkBitmap::kRGB_565_Config;
        default:
            return SkBitmap::kARGB_8888_Config;
    }
}

class ScreenshotPixelRef : public SkPixelRef {
public:
    ScreenshotPixelRef(SkColorTable* ctable) {
        fCTable = ctable;
        SkSafeRef(ctable);
        setImmutable();
    }
    virtual ~ScreenshotPixelRef() {
        SkSafeUnref(fCTable);
    }

    android::status_t update(int width, int height, int minLayer, int maxLayer, bool allLayers) {
        android::sp<android::IBinder> display =
            android::SurfaceComposerClient::getBuiltInDisplay(android::ISurfaceComposer::eDisplayIdMain);
        // layers not yet supported
        ALOGD("ScreenshotPixelRef update() width: %d, height: %d", width, height);
        android::status_t res = (width > 0 && height > 0)
                ? mScreenshot.update(display, width, height)
                : mScreenshot.update(display);
        if (res != android::NO_ERROR) {
            ALOGE("ScreenshotClient update() returned error: %d", res);
            return res;
        }

        return android::NO_ERROR;
    }

    uint32_t getWidth() const {
        return mScreenshot.getWidth();
    }

    uint32_t getHeight() const {
        return mScreenshot.getHeight();
    }

    uint32_t getStride() const {
        return mScreenshot.getStride();
    }

    uint32_t getFormat() const {
        return mScreenshot.getFormat();
    }
	
	size_t getSize() const {
	    return mScreenshot.getSize();
	}

    virtual Factory getFactory() { return 0; }

protected:
    // overrides from SkPixelRef
    virtual void* onLockPixels(SkColorTable** ct) {
        *ct = fCTable;
        return (void*)mScreenshot.getPixels();
    }

    virtual void onUnlockPixels() {
    }

private:
    android::ScreenshotClient mScreenshot;
    SkColorTable*    fCTable;

    typedef SkPixelRef INHERITED;
};

static jobject doScreenshot(JNIEnv* env, jobject clazz, jint width, jint height,
        jint minLayer, jint maxLayer, bool allLayers)
{
    // layers not yet supported
    if (minLayer != 0 || maxLayer != 0 || allLayers == false) {
        return 0;
    }

    void const* mapbase = MAP_FAILED;
    ssize_t mapsize = -1;
    void const* base = 0;
    uint32_t w, s, h, f;
    size_t size = 0;	
	
    ScreenshotPixelRef* pixels = new ScreenshotPixelRef(NULL);
    if (pixels->update(width, height, minLayer, maxLayer, allLayers) == android::NO_ERROR) {
        w = pixels->getWidth();
        h = pixels->getHeight();
        s = pixels->getStride();
        f = pixels->getFormat();
		size = pixels->getSize();
    } else {
	    delete pixels;
        const char* fbpath = "/dev/graphics/fb0";
        int fb = open(fbpath, O_RDONLY);
        if (fb >= 0) {
            struct fb_var_screeninfo vinfo;
            if (ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) == 0) {
                uint32_t bytespp;
                if (vinfoToPixelFormat(vinfo, &bytespp, &f) == NO_ERROR) {
                    size_t offset = (vinfo.xoffset + vinfo.yoffset*vinfo.xres) * bytespp;
                    w = vinfo.xres;
                    h = vinfo.yres;
                    s = vinfo.xres;
                    size = w*h*bytespp;
                    mapsize = offset + size;
                    mapbase = mmap(0, mapsize, PROT_READ, MAP_PRIVATE, fb, 0);
                    if (mapbase != MAP_FAILED) {
					    base = (void const *)((char const *)mapbase + offset);
                    }
                }
            }
            close(fb);
        }		
	}

    ALOGD("creating bitmap!");
    SkBitmap* bitmap = new SkBitmap();
	// bpr = stride * bytes per pixel
    bitmap->setConfig(convertPixelFormat(f), w, h, s*android::bytesPerPixel(f));
    if (f == android::PIXEL_FORMAT_RGBX_8888) {
        bitmap->setIsOpaque(true);
    }

    if (w > 0 && h > 0) {
        bitmap->setPixelRef(pixels)->unref();
        bitmap->lockPixels();
    } else {
        // be safe with an empty bitmap
        // delete pixels;
        // bitmap->setPixels(NULL);
        ALOGE("empty bitmap, returning null!");
        return 0;
    }

	// free memory map later!
	// if (mapbase != MAP_FAILED) {
	//     munmap((void*)mapbase, mapsize);
	// }
    return GraphicsJNI::createBitmap(env, bitmap, false, NULL);
}

extern "C" JNIEXPORT jobject JNICALL Java_com_jones_screenz_ScreenZService_screenshot(
    JNIEnv* env, jobject clazz, jint width, jint height) {
       
    ALOGD("native_screenshot()");
    return doScreenshot(env, clazz, width, height, 0, 0, true);
}
