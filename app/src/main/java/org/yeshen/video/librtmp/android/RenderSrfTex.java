package org.yeshen.video.librtmp.android;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;

import org.yeshen.video.librtmp.afix.AndroidUntil;
import org.yeshen.video.librtmp.afix.Cameras;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @Title: RenderSrfTex
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:17
 * @Version
 */
@TargetApi(18)
public class RenderSrfTex {
    private final FloatBuffer mNormalVtxBuf = AndroidUntil.createVertexBuffer();

    private int mFboTexId;
    private final MyRecorder mRecorder;

    private final float[] mSymmetryMtx = AndroidUntil.createIdentityMtx();
    private final float[] mNormalMtx = AndroidUntil.createIdentityMtx();

    private int mProgram = -1;
    private int maPositionHandle = -1;
    private int maTexCoordHandle = -1;
    private int muSamplerHandle = -1;
    private int muPosMtxHandle = -1;

    private EGLDisplay mSavedEglDisplay = null;
    private EGLSurface mSavedEglDrawSurface = null;
    private EGLSurface mSavedEglReadSurface = null;
    private EGLContext mSavedEglContext = null;

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private FloatBuffer mCameraTexCoordBuffer;

    public RenderSrfTex(int id, MyRecorder recorder) {
        mFboTexId = id;
        mRecorder = recorder;
    }

    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        initCameraTexCoordBuffer();
    }

    private void initCameraTexCoordBuffer() {
        int cameraWidth;
        int cameraHeight;
        Cameras.CameraMessage cameraData = Cameras.instance().getCameraData();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        if (Cameras.instance().isLandscape()) {
            cameraWidth = Math.max(width, height);
            cameraHeight = Math.min(width, height);
        } else {
            cameraWidth = Math.min(width, height);
            cameraHeight = Math.max(width, height);
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
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
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
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        }
    }

    public void draw() {
        saveRenderState();
        {
            AndroidUntil.checkGlError("draw_S");

            if (mRecorder.firstTimeSetup()) {
                mRecorder.startSwapData();
                mRecorder.makeCurrent();
                initGL();
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

            mCameraTexCoordBuffer.position(0);
            GLES20.glVertexAttribPointer(maTexCoordHandle,
                    2, GLES20.GL_FLOAT, false, 4 * 2, mCameraTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(maTexCoordHandle);

            GLES20.glUniform1i(muSamplerHandle, 0);

            GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mNormalMtx, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            mRecorder.swapBuffers();

            AndroidUntil.checkGlError("draw_E");
        }
        restoreRenderState();
    }

    private void initGL() {
        AndroidUntil.checkGlError("initGL_S");

        final String vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n";
        final String fragmentShader =
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";
        mProgram = AndroidUntil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");
        muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");

        Matrix.scaleM(mSymmetryMtx, 0, -1, 1, 1);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);

        AndroidUntil.checkGlError("initGL_E");
    }

    private void saveRenderState() {
        mSavedEglDisplay = EGL14.eglGetCurrentDisplay();
        mSavedEglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        mSavedEglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        mSavedEglContext = EGL14.eglGetCurrentContext();
    }

    private void restoreRenderState() {
        if (!EGL14.eglMakeCurrent(
                mSavedEglDisplay,
                mSavedEglDrawSurface,
                mSavedEglReadSurface,
                mSavedEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

}
