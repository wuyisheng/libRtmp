package org.yeshen.video.librtmp.afix;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.yeshen.video.librtmp.R;
import org.yeshen.video.librtmp.afix.net.packer.Packer;
import org.yeshen.video.librtmp.afix.net.sender.Sender;
import org.yeshen.video.librtmp.core.ILivingView;
import org.yeshen.video.librtmp.core.delegate.CameraOpenDelegate;
import org.yeshen.video.librtmp.core.delegate.ILivingDelegate;
import org.yeshen.video.librtmp.tools.AndroidUntil;
import org.yeshen.video.librtmp.tools.AndroidWakeLock;
import org.yeshen.video.librtmp.tools.Lg;
import org.yeshen.video.librtmp.tools.Options;
import org.yeshen.video.librtmp.tools.WeakHandler;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class LivingView extends FrameLayout implements ILivingView {
    public static final int NO_ERROR = 0;
    public static final int VIDEO_TYPE_ERROR = 1;
    public static final int AUDIO_TYPE_ERROR = 2;
    public static final int VIDEO_CONFIGURATION_ERROR = 3;
    public static final int AUDIO_CONFIGURATION_ERROR = 4;
    public static final int CAMERA_ERROR = 5;
    public static final int AUDIO_ERROR = 6;
    public static final int AUDIO_AEC_ERROR = 7;
    public static final int SDK_VERSION_ERROR = 8;

    private static final String TAG = LivingView.class.getSimpleName();
    private StreamController mStreamController;
    private AndroidWakeLock mWakeLock;
    private ILivingDelegate mLivingDelegate;

    private WeakHandler mHandler = new WeakHandler();
    private boolean isRenderSurfaceViewShowing = true;
    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Lg.d("SurfaceView destroy");
            Cameras.instance().stopPreview();
            Cameras.instance().releaseCamera();
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Lg.d("SurfaceView created");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Lg.d("SurfaceView width:" + width + " height:" + height);
        }
    };

    protected GLSurfaceView mGLSurfaceView;
    protected MyRenderer mRenderer;

    public LivingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public LivingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public LivingView(Context context) {
        super(context);
        initView(context);
    }

    @Override
    public void setVisibility(int visibility) {
        int currentVisibility = getVisibility();
        if (visibility == currentVisibility) {
            return;
        }
        switch (visibility) {
            case VISIBLE:
                addRenderSurfaceView();
                break;
            case GONE:
                removeRenderSurfaceView();
                break;
            case INVISIBLE:
                removeRenderSurfaceView();
                break;
        }
        super.setVisibility(visibility);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        float mAspectRatio = 9.0f / 16;
        if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.AT_MOST) {
            heightSpecSize = (int) (widthSpecSize / mAspectRatio);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSpecSize,
                    MeasureSpec.EXACTLY);
        } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
            widthSpecSize = (int) (heightSpecSize * mAspectRatio);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
                    MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setDelegate(@NonNull ILivingDelegate delegate) {
        mLivingDelegate = delegate;
    }

    @Override
    public void setPacker(@NonNull Packer packer) {
        mStreamController.setPacker(packer);
    }

    @Override
    public void setSender(@NonNull Sender sender) {
        mStreamController.setSender(sender);
    }

    @Override
    public void syncConfig() {
        Cameras.instance().syncConfig();
        mStreamController.syncVideoConfig();
    }

    @Override
    public void start() {
        ControlThread.get().post(new Runnable() {
            @Override
            public void run() {
                final int check = check();
                if (check == NO_ERROR) {
                    chooseVoiceMode();
                    mWakeLock.acquire();
                    mStreamController.start();
                }
                if (mLivingDelegate == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (check == NO_ERROR) {
                            mLivingDelegate.livingSuccess();
                        } else {
                            mLivingDelegate.livingFail(check);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void stop() {
        mWakeLock.release();
        mStreamController.stop();
        setAudioNormal();
    }

    @Override
    public void pause() {
        mStreamController.pause();
    }

    @Override
    public void resume() {
        mStreamController.resume();
    }

    @Override
    public boolean setVideoBps(int bps) {
        return mStreamController.setVideoBps(bps);
    }

    @Override
    public void destroy() {
        if (mWakeLock != null) mWakeLock.release();
        mWakeLock = null;
        Cameras.instance().releaseCamera();
        Cameras.instance().release();
        setAudioNormal();
    }

    private void initView(Context context) {
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.layout_camera_view, this, true);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
        mGLSurfaceView.setZOrderMediaOverlay(false);
        initRenderer();

        ControllerCameraVideo videoController = new ControllerCameraVideo(mRenderer);
        ControllerAudio audioController = new ControllerAudio();
        mStreamController = new StreamController(videoController, audioController);
        mRenderer.setCameraOpenListener(mCameraOpenListener);
        mWakeLock = new AndroidWakeLock(context);
    }

    @SuppressWarnings("deprecation")
    private void initRenderer() {
        mRenderer = new MyRenderer(mGLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        SurfaceHolder surfaceHolder = mGLSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    private void addRenderSurfaceView() {
        if (!isRenderSurfaceViewShowing) {
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            addView(mGLSurfaceView, 0, layoutParams);
            isRenderSurfaceViewShowing = true;
        }
    }

    private void removeRenderSurfaceView() {
        if (isRenderSurfaceViewShowing) {
            removeView(mGLSurfaceView);
            isRenderSurfaceViewShowing = false;
        }
    }

    private int check() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Lg.d("Android sdk version error");
            return SDK_VERSION_ERROR;
        }
        if (!checkAec()) {
            Lg.d("Doesn't support audio aec");
            return AUDIO_AEC_ERROR;
        }
        if (!isCameraOpen()) {
            Lg.d("The camera have not open");
            return CAMERA_ERROR;
        }
        MediaCodecInfo videoMediaCodecInfo = AndroidUntil.selectCodec(Options.getInstance().video.mime);
        if (videoMediaCodecInfo == null) {
            Lg.d("Video type error");
            return VIDEO_TYPE_ERROR;
        }
        MediaCodecInfo audioMediaCodecInfo = AndroidUntil.selectCodec(Options.getInstance().audio.mime);
        if (audioMediaCodecInfo == null) {
            Lg.d("Audio type error");
            return AUDIO_TYPE_ERROR;
        }
        MediaCodec videoMediaCodec = AndroidUntil.getVideoMediaCodec();
        if (videoMediaCodec == null) {
            Lg.d("Video mediacodec configuration error");
            return VIDEO_CONFIGURATION_ERROR;
        }
        MediaCodec audioMediaCodec = AndroidUntil.getAudioMediaCodec();
        if (audioMediaCodec == null) {
            Lg.d("Audio mediacodec configuration error");
            return AUDIO_CONFIGURATION_ERROR;
        }
        if (!AndroidUntil.checkMicSupport()) {
            Lg.d("Can not record the audio");
            return AUDIO_ERROR;
        }
        return NO_ERROR;
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

    private void chooseVoiceMode() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (Options.getInstance().audio.aec) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        } else {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
    }

    private boolean isCameraOpen() {
        return mRenderer.isCameraOpen();
    }

    private CameraOpenDelegate mCameraOpenListener = new CameraOpenDelegate() {
        @Override
        public void onOpenSuccess() {
            if (mLivingDelegate != null) mLivingDelegate.cameraSuccess();
        }

        @Override
        public void onOpenFail(int error) {
            if (mLivingDelegate != null) mLivingDelegate.cameraError(error);
        }
    };

    private void setAudioNormal() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
    }
}