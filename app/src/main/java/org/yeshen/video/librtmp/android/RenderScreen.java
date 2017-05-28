package org.yeshen.video.librtmp.android;

import android.opengl.GLES20;

import org.yeshen.video.librtmp.afix.AndroidUntil;
import org.yeshen.video.librtmp.afix.Cameras;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @Title: RenderScreen
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:15
 * @Version
 */
public class RenderScreen {
    private final FloatBuffer mNormalVtxBuf = AndroidUntil.createVertexBuffer();
    private final float[] mPosMtx = AndroidUntil.createIdentityMtx();

    private int mFboTexId;

    private int mProgram = -1;
    private int maPositionHandle = -1;
    private int maTexCoordHandle = -1;
    private int muPosMtxHandle = -1;
    private int muSamplerHandle = -1;

    private int mScreenW = -1;
    private int mScreenH = -1;

    private FloatBuffer mCameraTexCoordBuffer;

    public RenderScreen(int id) {
        mFboTexId = id;
        initGL();
    }

    public void setScreenSize(int width, int height) {
        mScreenW = width;
        mScreenH = height;

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
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
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
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        }

    }

    public void draw() {
        if (mScreenW <= 0 || mScreenH <= 0) {
            return;
        }
        AndroidUntil.checkGlError("draw_S");

        GLES20.glViewport(0, 0, mScreenW, mScreenH);

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f);
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

        GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);
        GLES20.glUniform1i(muSamplerHandle, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        AndroidUntil.checkGlError("draw_E");
    }

    private void initGL() {
        AndroidUntil.checkGlError("initGL_S");

        final String vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n";
        final String fragmentShader =
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2  textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";
        mProgram = AndroidUntil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");

        AndroidUntil.checkGlError("initGL_E");
    }

}
