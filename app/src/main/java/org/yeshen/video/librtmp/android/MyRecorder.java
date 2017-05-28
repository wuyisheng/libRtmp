package org.yeshen.video.librtmp.android;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import org.yeshen.video.librtmp.afix.AndroidUntil;
import org.yeshen.video.librtmp.afix.interfaces.OnVideoEncodeListener;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(18)
class MyRecorder {
    private MediaCodec mMediaCodec;
    private InputSurface mInputSurface;
    private OnVideoEncodeListener mListener;
    private boolean mPause;
    private MediaCodec.BufferInfo mBufferInfo;
    private HandlerThread mHandlerThread;
    private Handler mEncoderHandler;
    private ReentrantLock encodeLock = new ReentrantLock();
    private volatile boolean isStarted;

    MyRecorder() {
    }

    void setVideoEncodeListener(OnVideoEncodeListener listener) {
        mListener = listener;
    }

    void setPause(boolean pause) {
        mPause = pause;
    }

    void prepareEncoder() {
        if (mMediaCodec != null || mInputSurface != null) {
            throw new RuntimeException("prepareEncoder called twice?");
        }
        mMediaCodec = AndroidUntil.getVideoMediaCodec();
        mHandlerThread = new HandlerThread("SopCastEncode");
        mHandlerThread.start();
        mEncoderHandler = new Handler(mHandlerThread.getLooper());
        mBufferInfo = new MediaCodec.BufferInfo();
        isStarted = true;
    }

    boolean firstTimeSetup() {
        if (mMediaCodec == null || mInputSurface != null) {
            return false;
        }
        try {
            mInputSurface = new InputSurface(mMediaCodec.createInputSurface());
            mMediaCodec.start();
        } catch (Exception e) {
            releaseEncoder();
            throw (RuntimeException) e;
        }
        return true;
    }

    void startSwapData() {
        mEncoderHandler.post(swapDataRunnable);
    }

    void makeCurrent() {
        mInputSurface.makeCurrent();
    }

    void swapBuffers() {
        if (mMediaCodec == null || mPause) {
            return;
        }
        mInputSurface.swapBuffers();
        mInputSurface.setPresentationTime(System.nanoTime());
    }

    private Runnable swapDataRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    public void stop() {
        if (!isStarted) {
            return;
        }
        isStarted = false;
        mEncoderHandler.removeCallbacks(null);
        mHandlerThread.quit();
        encodeLock.lock();
        releaseEncoder();
        encodeLock.unlock();
    }

    private void releaseEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.signalEndOfInputStream();
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    boolean setRecorderBps(int bps) {
        if (mMediaCodec == null || mInputSurface == null) {
            return false;
        }
        Bundle bitrate = new Bundle();
        bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
        mMediaCodec.setParameters(bitrate);
        return true;
    }

    private void drainEncoder() {
        ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
        while (isStarted) {
            encodeLock.lock();
            if (mMediaCodec != null) {
                int outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
                if (outBufferIndex >= 0) {
                    ByteBuffer bb = outBuffers[outBufferIndex];
                    if (mListener != null) {
                        mListener.onVideoEncode(bb, mBufferInfo);
                    }
                    mMediaCodec.releaseOutputBuffer(outBufferIndex, false);
                } else {
                    try {
                        // wait 10ms
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                encodeLock.unlock();
            } else {
                encodeLock.unlock();
                break;
            }
        }
    }

    private static class InputSurface {
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        private Surface mSurface = null;
        private EGLDisplay mEGLDisplay = null;
        private EGLContext mEGLContext = null;
        private EGLSurface mEGLSurface = null;

        InputSurface(Surface surface) {
            if (surface == null) {
                throw new NullPointerException();
            }
            mSurface = surface;
            eglSetup();
        }

        void release() {
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);

            mSurface.release();

            mSurface = null;
            mEGLDisplay = null;
            mEGLContext = null;
            mEGLSurface = null;
        }

        public Surface getSurface() {
            return mSurface;
        }

        void makeCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }

        boolean swapBuffers() {
            return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        }

        void setPresentationTime(long nsecs) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        }

        private void eglSetup() {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                mEGLDisplay = null;
                throw new RuntimeException("unable to initialize EGL14");
            }

            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                    numConfigs, 0)) {
                throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
            }

            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.eglGetCurrentContext(),
                    attrib_list, 0);
            AndroidUntil.checkEglError("eglCreateContext");
            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }

            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                    surfaceAttribs, 0);
            AndroidUntil.checkEglError("eglCreateWindowSurface");
            if (mEGLSurface == null) {
                throw new RuntimeException("surface was null");
            }
        }
    }
}
