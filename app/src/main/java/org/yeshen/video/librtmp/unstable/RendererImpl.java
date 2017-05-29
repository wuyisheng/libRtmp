package org.yeshen.video.librtmp.unstable;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.yeshen.video.librtmp.core.IRenderer;
import org.yeshen.video.librtmp.core.delegate.ICameraOpenDelegate;
import org.yeshen.video.librtmp.unstable.exception.CameraDisabledException;
import org.yeshen.video.librtmp.unstable.exception.CameraHardwareException;
import org.yeshen.video.librtmp.unstable.exception.CameraNotSupportException;
import org.yeshen.video.librtmp.unstable.exception.NoCameraException;
import org.yeshen.video.librtmp.unstable.tools.AndroidUntil;
import org.yeshen.video.librtmp.unstable.tools.Lg;
import org.yeshen.video.librtmp.unstable.tools.Options;
import org.yeshen.video.librtmp.unstable.tools.WeakHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class RendererImpl implements IRenderer {
    private int mSurfaceTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private RenderScreen mRenderScreen;
    private RenderRecord mRenderRecord;
    private ICameraOpenDelegate mCameraOpenListener;
    private WeakHandler mHandler = new WeakHandler(Looper.getMainLooper());
    private GLSurfaceView mView;
    private boolean isCameraOpen;
    private Effect mEffect;
    private int mEffectTextureId;
    private boolean updateSurface = false;
    private final float[] mTexMtx = AndroidUntil.createIdentityMtx();
    private int mVideoWidth;
    private int mVideoHeight;

    RendererImpl(GLSurfaceView view) {
        mView = view;
        mEffect = Effect.getDefault(mView.getContext());
    }

    @Override
    public void setCameraOpenListener(ICameraOpenDelegate cameraOpenListener) {
        this.mCameraOpenListener = cameraOpenListener;
    }

    @Override
    public void syncVideoConfig() {
        mVideoWidth = AndroidUntil.getVideoSize(Options.getInstance().video.width);
        mVideoHeight = AndroidUntil.getVideoSize(Options.getInstance().video.height);
        Options.getInstance().video.width = mVideoWidth;
        Options.getInstance().video.height = mVideoHeight;
    }

    @Override
    public void enableRecord(@NonNull RecorderImpl recorderImpl) {
        if (mRenderRecord != null) return;
        mRenderRecord = new RenderRecord(mEffectTextureId, recorderImpl);
        mRenderRecord.setVideoSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void disableRecord() {
        mRenderRecord = null;
    }

    @Override
    public int checkStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Lg.d("Android sdk version error");
            return SDK_VERSION_ERROR;
        }
        if (!checkAec()) {
            Lg.d("Doesn't support audio aec");
            return AUDIO_AEC_ERROR;
        }
        if (!isCameraOpen) {
            Lg.d("The camera have not open");
            return CAMERA_ERROR;
        }
        if (AndroidUntil.selectCodec(Options.getInstance().video.mime) == null) {
            Lg.d("Video type error");
            return VIDEO_TYPE_ERROR;
        }
        if (AndroidUntil.selectCodec(Options.getInstance().audio.mime) == null) {
            Lg.d("Audio type error");
            return AUDIO_TYPE_ERROR;
        }
        if (AndroidUntil.getVideoMediaCodec() == null) {
            Lg.d("Video mediacodec configuration error");
            return VIDEO_CONFIGURATION_ERROR;
        }
        if (AndroidUntil.getAudioMediaCodec() == null) {
            Lg.d("Audio mediacodec configuration error");
            return AUDIO_CONFIGURATION_ERROR;
        }
        if (!AndroidUntil.checkMicSupport()) {
            Lg.d("Can not record the audio");
            return AUDIO_ERROR;
        }
        return NO_ERROR;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            updateSurface = true;
        }
        mView.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initSurfaceTexture();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        startCameraPreview();
        if (isCameraOpen) {
            if (mRenderScreen == null) {
                initScreenTexture();
            }
            mRenderScreen.setScreenSize(width, height);
            Options.getInstance().video.width = mVideoWidth;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTexMtx);
                updateSurface = false;
            }
        }
        mEffect.drawFromCameraPreview(mTexMtx);
        if (mRenderScreen != null) mRenderScreen.draw();
        if (mRenderRecord != null) mRenderRecord.draw();
    }

    private boolean checkAec() {
        if (Options.getInstance().audio.aec) {
            if (Options.getInstance().audio.frequency == 8000 ||
                    Options.getInstance().audio.frequency == 16000) {
                if (Options.getInstance().audio.channelCount == 1) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private void initSurfaceTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mSurfaceTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void initScreenTexture() {
        mEffect.setTextureId(mSurfaceTextureId);
        mEffect.prepare();
        mEffectTextureId = mEffect.getEffectedTextureId();
        mRenderScreen = new RenderScreen(mEffectTextureId);
    }

    private void startCameraPreview() {
        try {
            AndroidUntil.checkCameraService(mView.getContext());
        } catch (CameraDisabledException e) {
            postOpenCameraError(ICameraOpenDelegate.CAMERA_DISABLED);
            e.printStackTrace();
            return;
        } catch (NoCameraException e) {
            postOpenCameraError(ICameraOpenDelegate.NO_CAMERA);
            e.printStackTrace();
            return;
        }
        Cameras.State state = Cameras.instance().getState();
        Cameras.instance().placePreview(mSurfaceTexture);
        if (state != Cameras.State.PREVIEW) {
            try {
                Cameras.instance().openCamera();
                Cameras.instance().startPreview();
                if (mCameraOpenListener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCameraOpenListener.onOpenSuccess();
                        }
                    });
                }
                isCameraOpen = true;
            } catch (CameraHardwareException e) {
                e.printStackTrace();
                postOpenCameraError(ICameraOpenDelegate.CAMERA_OPEN_FAILED);
            } catch (CameraNotSupportException e) {
                e.printStackTrace();
                postOpenCameraError(ICameraOpenDelegate.CAMERA_NOT_SUPPORT);
            }
        }
    }

    private void postOpenCameraError(final int error) {
        if (mCameraOpenListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCameraOpenListener != null) {
                        mCameraOpenListener.onOpenFail(error);
                    }
                }
            });
        }
    }

    @TargetApi(18)
    private static class RenderRecord {
        private final FloatBuffer mNormalVtxBuf = AndroidUntil.createVertexBuffer();

        private int mFboTexId;
        private final RecorderImpl mRecorderImpl;
        private final float[] mSymmetryMtx = AndroidUntil.createIdentityMtx();
        private final float[] mNormalMtx = AndroidUntil.createIdentityMtx();
        private int mProgram = -1;
        private int maPositionHandle = -1;
        private int maTextCodeHandle = -1;
        private int muSamplerHandle = -1;
        private int muPosMtxHandle = -1;
        private int mVideoWidth = 0;
        private int mVideoHeight = 0;

        private FloatBuffer mCameraVertexCoordinatesBuffer;

        RenderRecord(int id, RecorderImpl recorderImpl) {
            mFboTexId = id;
            mRecorderImpl = recorderImpl;
        }

        void setVideoSize(int width, int height) {
            mVideoWidth = width;
            mVideoHeight = height;
            int cameraWidth;
            int cameraHeight;
            Cameras.CameraMessage cameraData = Cameras.instance().getCurrentCamera();
            int temp_width = cameraData.cameraWidth;
            int temp_height = cameraData.cameraHeight;
            if (Cameras.instance().isLandscape()) {
                cameraWidth = Math.max(temp_width, temp_height);
                cameraHeight = Math.min(temp_width, temp_height);
            } else {
                cameraWidth = Math.min(temp_width, temp_height);
                cameraHeight = Math.max(temp_width, temp_height);
            }

            float hRatio = mVideoWidth / ((float) cameraWidth);
            float vRatio = mVideoHeight / ((float) cameraHeight);

            float ratio;
            if (hRatio > vRatio) {
                ratio = mVideoHeight / (cameraHeight * hRatio);
                final float vtx[] = {
                        //UV
                        0f, 0.5f + ratio / 2,
                        0f, 0.5f - ratio / 2,
                        1f, 0.5f + ratio / 2,
                        1f, 0.5f - ratio / 2,
                };
                ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
                bb.order(ByteOrder.nativeOrder());
                mCameraVertexCoordinatesBuffer = bb.asFloatBuffer();
                mCameraVertexCoordinatesBuffer.put(vtx);
                mCameraVertexCoordinatesBuffer.position(0);
            } else {
                ratio = mVideoWidth / (cameraWidth * vRatio);
                final float vtx[] = {
                        //UV
                        0.5f - ratio / 2, 1f,
                        0.5f - ratio / 2, 0f,
                        0.5f + ratio / 2, 1f,
                        0.5f + ratio / 2, 0f,
                };
                ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
                bb.order(ByteOrder.nativeOrder());
                mCameraVertexCoordinatesBuffer = bb.asFloatBuffer();
                mCameraVertexCoordinatesBuffer.put(vtx);
                mCameraVertexCoordinatesBuffer.position(0);
            }
        }

        void draw() {
            EGLDisplay mSavedEglDisplay = EGL14.eglGetCurrentDisplay();
            EGLSurface mSavedEglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
            EGLSurface mSavedEglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
            EGLContext mSavedEglContext = EGL14.eglGetCurrentContext();
            {
                AndroidUntil.checkGlError("draw_S");
                if (mRecorderImpl.isFirstSetup()) {
                    mRecorderImpl.startSwapAsync();
                    mRecorderImpl.makeCurrent();
                    AndroidUntil.checkGlError("initGL_S");

                    mProgram = AndroidUntil.createProgram();
                    maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
                    maTextCodeHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
                    muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");
                    muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");

                    Matrix.scaleM(mSymmetryMtx, 0, -1, 1, 1);
                    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                    GLES20.glDisable(GLES20.GL_CULL_FACE);
                    GLES20.glDisable(GLES20.GL_BLEND);
                    AndroidUntil.checkGlError("initGL_E");
                } else {
                    mRecorderImpl.makeCurrent();
                }
                GLES20.glViewport(0, 0, mVideoWidth, mVideoHeight);
                GLES20.glClearColor(0f, 0f, 0f, 1f);
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mProgram);
                mNormalVtxBuf.position(0);
                GLES20.glVertexAttribPointer(maPositionHandle,
                        3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBuf);
                GLES20.glEnableVertexAttribArray(maPositionHandle);
                mCameraVertexCoordinatesBuffer.position(0);
                GLES20.glVertexAttribPointer(maTextCodeHandle,
                        2, GLES20.GL_FLOAT, false, 4 * 2, mCameraVertexCoordinatesBuffer);
                GLES20.glEnableVertexAttribArray(maTextCodeHandle);
                GLES20.glUniform1i(muSamplerHandle, 0);
                GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mNormalMtx, 0);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                mRecorderImpl.swapBuffers();
                AndroidUntil.checkGlError("draw_E");
            }
            if (!EGL14.eglMakeCurrent(
                    mSavedEglDisplay,
                    mSavedEglDrawSurface,
                    mSavedEglReadSurface,
                    mSavedEglContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }
    }

    private static class RenderScreen {
        private final FloatBuffer mNormalVtxBuf = AndroidUntil.createVertexBuffer();
        private final float[] mPosMtx = AndroidUntil.createIdentityMtx();
        private int mFboTexId;
        private int mProgram = -1;
        private int maPositionHandle = -1;
        private int maTextCodeHandle = -1;
        private int muPosMtxHandle = -1;
        private int muSamplerHandle = -1;
        private int mScreenW = -1;
        private int mScreenH = -1;
        private FloatBuffer mCameraVertexCoordinatesBuffer;

        RenderScreen(int id) {
            mFboTexId = id;

            AndroidUntil.checkGlError("initGL_S");

            mProgram = AndroidUntil.createProgram();
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
            maTextCodeHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
            muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
            muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");

            AndroidUntil.checkGlError("initGL_E");
        }

        void setScreenSize(int width, int height) {
            mScreenW = width;
            mScreenH = height;

            int cameraWidth;
            int cameraHeight;
            Cameras.CameraMessage cameraData = Cameras.instance().getCurrentCamera();
            int widths = cameraData.cameraWidth;
            int heights = cameraData.cameraHeight;
            if (Cameras.instance().isLandscape()) {
                cameraWidth = Math.max(widths, heights);
                cameraHeight = Math.min(widths, heights);
            } else {
                cameraWidth = Math.min(widths, heights);
                cameraHeight = Math.max(widths, heights);
            }

            float hRatio = mScreenW / ((float) cameraWidth);
            float vRatio = mScreenH / ((float) cameraHeight);

            float ratio;
            if (hRatio > vRatio) {
                ratio = mScreenH / (cameraHeight * hRatio);
                final float vtx[] = {
                        //UV
                        0f, 0.5f + ratio / 2,
                        0f, 0.5f - ratio / 2,
                        1f, 0.5f + ratio / 2,
                        1f, 0.5f - ratio / 2,
                };
                ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
                bb.order(ByteOrder.nativeOrder());
                mCameraVertexCoordinatesBuffer = bb.asFloatBuffer();
                mCameraVertexCoordinatesBuffer.put(vtx);
                mCameraVertexCoordinatesBuffer.position(0);
            } else {
                ratio = mScreenW / (cameraWidth * vRatio);
                final float vtx[] = {
                        //UV
                        0.5f - ratio / 2, 1f,
                        0.5f - ratio / 2, 0f,
                        0.5f + ratio / 2, 1f,
                        0.5f + ratio / 2, 0f,
                };
                ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
                bb.order(ByteOrder.nativeOrder());
                mCameraVertexCoordinatesBuffer = bb.asFloatBuffer();
                mCameraVertexCoordinatesBuffer.put(vtx);
                mCameraVertexCoordinatesBuffer.position(0);
            }
        }

        void draw() {
            if (mScreenW <= 0 || mScreenH <= 0) {
                return;
            }
            AndroidUntil.checkGlError("draw_S");
            GLES20.glViewport(0, 0, mScreenW, mScreenH);
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);
            mNormalVtxBuf.position(0);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBuf);
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            mCameraVertexCoordinatesBuffer.position(0);
            GLES20.glVertexAttribPointer(maTextCodeHandle, 2, GLES20.GL_FLOAT, false, 4 * 2, mCameraVertexCoordinatesBuffer);
            GLES20.glEnableVertexAttribArray(maTextCodeHandle);
            GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);
            GLES20.glUniform1i(muSamplerHandle, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            AndroidUntil.checkGlError("draw_E");
        }

    }


}
