package org.yeshen.video.librtmp.afix;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.text.TextUtils;

import org.yeshen.video.librtmp.core.IEffect;
import org.yeshen.video.librtmp.tools.AndroidUntil;

import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * @Title: Effect
 * @Package com.laifeng.sopcastsdk.video.effert
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:10
 * @Version
 */

abstract class Effect implements IEffect {
    private final FloatBuffer mVtxBuf = AndroidUntil.createSquareVtx();
    private final float[] mPosMtx = AndroidUntil.createIdentityMtx();

    private int mTextureId = -1;
    private int mProgram = -1;
    private int mPositionHandle = -1;
    private int mTexCoordinatesHandle = -1;
    private int mPosMtxHandle = -1;
    private int mTexMtxHandle = -1;

    private final int[] mFboId = new int[]{0};
    private final int[] mRboId = new int[]{0};
    private final int[] mTexId = new int[]{0};

    private int mWidth = -1;
    private int mHeight = -1;

    private final LinkedList<Runnable> mRunOnDraw;
    private String mVertex;
    private String mFragment;

    private Effect() {
        mRunOnDraw = new LinkedList<>();
    }

    @Override
    public void prepare() {
        loadShaderAndParams(mVertex, mFragment);
        initSize();
        createEffectTexture();
    }

    @Override
    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    @Override
    public int getEffectedTextureId() {
        return mTexId[0];
    }

    @Override
    public void drawFromCameraPreview(final float[] tex_mtx) {
        if (-1 == mProgram || mTextureId == -1 || mWidth == -1) {
            return;
        }

        AndroidUntil.checkGlError("draw_S");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);

        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        runPendingOnDrawTasks();

        mVtxBuf.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        mVtxBuf.position(3);
        GLES20.glVertexAttribPointer(mTexCoordinatesHandle,
                2, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(mTexCoordinatesHandle);

        if (mPosMtxHandle >= 0)
            GLES20.glUniformMatrix4fv(mPosMtxHandle, 1, false, mPosMtx, 0);

        if (mTexMtxHandle >= 0)
            GLES20.glUniformMatrix4fv(mTexMtxHandle, 1, false, tex_mtx, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        AndroidUntil.checkGlError("draw_E");
    }

    private void setShader(String vertex, String fragment) {
        mVertex = vertex;
        mFragment = fragment;
    }

    private void loadShaderAndParams(String vertex, String fragment) {
        if (TextUtils.isEmpty(vertex) || TextUtils.isEmpty(fragment)) {
            vertex = AndroidUntil.SHARDE_NULL_VERTEX;
            fragment = AndroidUntil.SHARDE_NULL_FRAGMENT;
        }
        AndroidUntil.checkGlError("initSH_S");
        mProgram = AndroidUntil.createProgram(vertex, fragment);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        mTexCoordinatesHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");

        mPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        mTexMtxHandle = GLES20.glGetUniformLocation(mProgram, "uTexMtx");
        AndroidUntil.checkGlError("initSH_E");
    }

    private void initSize() {
        if (Cameras.instance().getState() != Cameras.State.PREVIEW) {
            return;
        }
        Cameras.CameraMessage cameraData = Cameras.instance().getCurrentCamera();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        if (Cameras.instance().isLandscape()) {
            mWidth = Math.max(width, height);
            mHeight = Math.min(width, height);
        } else {
            mWidth = Math.min(width, height);
            mHeight = Math.max(width, height);
        }
    }

    private void createEffectTexture() {
        AndroidUntil.checkGlError("initFBO_S");
        GLES20.glGenFramebuffers(1, mFboId, 0);
        GLES20.glGenRenderbuffers(1, mRboId, 0);
        GLES20.glGenTextures(1, mTexId, 0);

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRboId[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRboId[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexId[0], 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("glCheckFramebufferStatus()");
        }
        AndroidUntil.checkGlError("initFBO_E");
    }

    private void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    static Effect getDefault(Context context) {
        return new NullEffect(context);
    }

    private static class NullEffect extends Effect {
        private static final String NULL_EFFECT_VERTEX = "null/vertexshader.glsl";
        private static final String NULL_EFFECT_FRAGMENT = "null/fragmentshader.glsl";

        NullEffect(Context context) {
            super();
            String vertexShader = AndroidUntil.getFileContextFromAssets(context, NULL_EFFECT_VERTEX);
            String fragmentShader = AndroidUntil.getFileContextFromAssets(context, NULL_EFFECT_FRAGMENT);
            super.setShader(vertexShader, fragmentShader);
        }
    }
}

