package org.yeshen.video.librtmp.android;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
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
    private RenderSrfTex mRenderSrfTex;

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
        mEffect = new NullEffect(mView.getContext());
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        this.mCameraOpenListener = cameraOpenListener;
    }

    public void setVideoConfiguration() {
        mVideoWidth = AndroidUntil.getVideoSize(Options.getInstance().video.width);
        mVideoHeight = AndroidUntil.getVideoSize(Options.getInstance().video.height);
        Options.getInstance().width = mVideoWidth;
        Options.getInstance().height = mVideoHeight;
    }

    public void setRecorder(MyRecorder recorder) {
        synchronized(this) {
            if (recorder != null) {
                mRenderSrfTex = new RenderSrfTex(mEffectTextureId, recorder);
                mRenderSrfTex.setVideoSize(mVideoWidth, mVideoHeight);
            } else {
                mRenderSrfTex = null;
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
            Options.getInstance().width = mVideoWidth;
            Options.getInstance().height = mVideoHeight;
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
        if(mRenderScreen != null) {
            mRenderScreen.draw();
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.draw();
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


    public void setEffect(Effect effect) {
        mEffect.release();
        mEffect = effect;
        effect.setTextureId(mSurfaceTextureId);
        effect.prepare();
        mEffectTextureId = effect.getEffertedTextureId();
        if(mRenderScreen != null) {
            mRenderScreen.setTextureId(mEffectTextureId);
        }
        if(mRenderSrfTex != null) {
            mRenderSrfTex.setTextureId(mEffectTextureId);
        }
    }
}
