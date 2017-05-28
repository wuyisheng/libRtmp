package org.yeshen.video.librtmp.afix;

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
import android.support.annotation.NonNull;
import android.view.Surface;

import org.yeshen.video.librtmp.core.IRecorder;
import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;
import org.yeshen.video.librtmp.tools.AndroidUntil;
import org.yeshen.video.librtmp.tools.Lg;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(18)
public class MyRecorder implements IRecorder {
    private MediaCodec mMediaCodec;
    private InputSurface mInputSurface;
    private IVideoEncodeDelegate mListener;
    private volatile boolean mPause;
    private MediaCodec.BufferInfo mBufferInfo;
    private HandlerThread mHandlerThread;
    private Handler mEncoderHandler;
    private ReentrantLock encodeLock = new ReentrantLock();
    private volatile boolean isStarted;

    MyRecorder() {
    }

    @Override
    public void pause() {
        mPause = true;
    }

    @Override
    public void resume() {
        mPause = false;
    }

    @Override
    public void prepare(@NonNull IVideoEncodeDelegate delegate) {
        this.mListener = delegate;
        if (mMediaCodec != null || mInputSurface != null) {
            throw new RuntimeException("prepare called twice?");
        }
        mMediaCodec = AndroidUntil.getVideoMediaCodec();
        mHandlerThread = new HandlerThread("SopCastEncode");
        mHandlerThread.start();
        mEncoderHandler = new Handler(mHandlerThread.getLooper());
        mBufferInfo = new MediaCodec.BufferInfo();
        isStarted = true;
    }

    @Override
    public boolean isFirstSetup() {
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

    @Override
    public void startSwapAsync() {
        mEncoderHandler.post(swapDataRunnable);
    }

    @Override
    public void makeCurrent() {
        mInputSurface.makeCurrent();
    }

    @Override
    public void swapBuffers() {
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

    @Override
    public void stop() {
        this.mListener = null;
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

    @Override
    public boolean resetVideoBps(int bps) {
        if (mMediaCodec == null || mInputSurface == null) {
            return false;
        }
        //重新设置硬编bps，在低于19的版本需要重启编码器
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //由于重启硬编编码器效果不好，此次不做处理
            Lg.d( "Bps need change, but MediaCodec do not support.");
            return false;
        }else {
            Bundle bitrate = new Bundle();
            bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
            mMediaCodec.setParameters(bitrate);
            return true;
        }
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
