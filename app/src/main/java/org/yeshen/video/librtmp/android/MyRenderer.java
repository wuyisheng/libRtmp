package org.yeshen.video.librtmp.android;

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
import android.os.Looper;

import org.yeshen.video.librtmp.afix.AndroidUntil;
import org.yeshen.video.librtmp.afix.Cameras;
import org.yeshen.video.librtmp.afix.interfaces.CameraListener;
import org.yeshen.video.librtmp.exception.CameraDisabledException;
import org.yeshen.video.librtmp.exception.CameraHardwareException;
import org.yeshen.video.librtmp.exception.CameraNotSupportException;
import org.yeshen.video.librtmp.exception.NoCameraException;
import org.yeshen.video.librtmp.tools.Options;
import org.yeshen.video.librtmp.tools.WeakHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @Title: MyRenderer
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:06
 * @Version
 */
public class MyRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private int mSurfaceTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private RenderScreen mRenderScreen;
    private RenderRecord mRenderRecord;
    private CameraListener mCameraOpenListener;
    private WeakHandler mHandler = new WeakHandler(Looper.getMainLooper());
    private GLSurfaceView mView;
    private boolean isCameraOpen;
    private Effect mEffect;
    private int mEffectTextureId;
    private boolean updateSurface = false;
    private final float[] mTexMtx = AndroidUntil.createIdentityMtx();
    private int mVideoWidth;
    private int mVideoHeight;

    public MyRenderer(GLSurfaceView view) {
        mView = view;
        mEffect = Effect.getDefault(mView.getContext());
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        this.mCameraOpenListener = cameraOpenListener;
    }

    void setVideoConfiguration() {
        mVideoWidth = AndroidUntil.getVideoSize(Options.getInstance().video.width);
        mVideoHeight = AndroidUntil.getVideoSize(Options.getInstance().video.height);
        Options.getInstance().video.width = mVideoWidth;
        Options.getInstance().video.height = mVideoHeight;
    }

    void setRecorder(MyRecorder recorder) {
        synchronized(this) {
            if (recorder != null) {
                mRenderRecord = new RenderRecord(mEffectTextureId, recorder);
                mRenderRecord.setVideoSize(mVideoWidth, mVideoHeight);
            } else {
                mRenderRecord = null;
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized(this) {
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
        if(isCameraOpen) {
            if (mRenderScreen == null) {
                initScreenTexture();
            }
            mRenderScreen.setScreenSize(width, height);
            Options.getInstance().video.width = mVideoWidth;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized(this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTexMtx);
                updateSurface = false;
            }
        }
        mEffect.draw(mTexMtx);
        if(mRenderScreen != null) mRenderScreen.draw();
        if (mRenderRecord != null) mRenderRecord.draw();
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
        mEffectTextureId = mEffect.getEffertedTextureId();
        mRenderScreen = new RenderScreen(mEffectTextureId);
    }

    private void startCameraPreview() {
        try {
            AndroidUntil.checkCameraService(mView.getContext());
        } catch (CameraDisabledException e) {
            postOpenCameraError(CameraListener.CAMERA_DISABLED);
            e.printStackTrace();
            return;
        } catch (NoCameraException e) {
            postOpenCameraError(CameraListener.NO_CAMERA);
            e.printStackTrace();
            return;
        }
        Cameras.State state = Cameras.instance().getState();
        Cameras.instance().setSurfaceTexture(mSurfaceTexture);
        if (state != Cameras.State.PREVIEW) {
            try {
                Cameras.instance().openCamera();
                Cameras.instance().startPreview();
                if(mCameraOpenListener != null) {
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
                postOpenCameraError(CameraListener.CAMERA_OPEN_FAILED);
            } catch (CameraNotSupportException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_NOT_SUPPORT);
            }
        }
    }

    private void postOpenCameraError(final int error) {
        if(mCameraOpenListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mCameraOpenListener != null) {
                        mCameraOpenListener.onOpenFail(error);
                    }
                }
            });
        }
    }

    public boolean isCameraOpen() {
        return isCameraOpen;
    }

    @TargetApi(18)
    private static class RenderRecord {
        private final FloatBuffer mNormalVtxBuf = AndroidUntil.createVertexBuffer();

        private int mFboTexId;
        private final MyRecorder mRecorder;
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

        RenderRecord(int id, MyRecorder recorder) {
            mFboTexId = id;
            mRecorder = recorder;
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
                if (mRecorder.firstTimeSetup()) {
                    mRecorder.startSwapData();
                    mRecorder.makeCurrent();
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
                    mRecorder.makeCurrent();
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

                mRecorder.swapBuffers();
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
