package org.yeshen.video.librtmp.afix;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.yeshen.video.librtmp.R;
import org.yeshen.video.librtmp.afix.interfaces.CameraListener;
import org.yeshen.video.librtmp.afix.interfaces.LivingStartListener;
import org.yeshen.video.librtmp.android.ControllerAudio;
import org.yeshen.video.librtmp.android.ControllerCameraVideo;
import org.yeshen.video.librtmp.android.MyRenderer;
import org.yeshen.video.librtmp.android.ControllerStream;
import org.yeshen.video.librtmp.net.packer.Packer;
import org.yeshen.video.librtmp.net.sender.Sender;
import org.yeshen.video.librtmp.tools.Lg;
import org.yeshen.video.librtmp.tools.Options;
import org.yeshen.video.librtmp.tools.WeakHandler;

/*********************************************************************
 * Created by yeshen on 2017/05/21.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class VideoLivingView extends FrameLayout {
    public static final int NO_ERROR = 0;
    public static final int VIDEO_TYPE_ERROR = 1;
    public static final int AUDIO_TYPE_ERROR = 2;
    public static final int VIDEO_CONFIGURATION_ERROR = 3;
    public static final int AUDIO_CONFIGURATION_ERROR = 4;
    public static final int CAMERA_ERROR = 5;
    public static final int AUDIO_ERROR = 6;
    public static final int AUDIO_AEC_ERROR = 7;
    public static final int SDK_VERSION_ERROR = 8;

    private static final String TAG = VideoLivingView.class.getSimpleName();
    private ControllerStream mControllerStream;
    private PowerManager.WakeLock mWakeLock;
    private CameraListener mOutCameraOpenListener;
    private LivingStartListener mLivingStartListener;
    private WeakHandler mHandler = new WeakHandler();

    protected GLSurfaceView mGLSurfaceView;
    protected MyRenderer mRenderer;
    private boolean isRenderSurfaceViewShowing = true;

    public VideoLivingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VideoLivingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public VideoLivingView(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.layout_camera_view, this, true);
        mGLSurfaceView = (GLSurfaceView)findViewById(R.id.surface_view);
        mGLSurfaceView.setZOrderMediaOverlay(false);
        initRenderer();

        ControllerCameraVideo videoController = new ControllerCameraVideo(mRenderer);
        ControllerAudio audioController = new ControllerAudio();
        mControllerStream = new ControllerStream(videoController, audioController);
        mRenderer.setCameraOpenListener(mCameraOpenListener);
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

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();

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

    public void init() {
        PowerManager mPowerManager = ((PowerManager) getContext().getSystemService(Context.POWER_SERVICE));
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE, TAG);
    }

    public void setLivingStartListener(LivingStartListener listener) {
        mLivingStartListener = listener;
    }

    public void setPacker(Packer packer) {
        mControllerStream.setPacker(packer);
    }

    public void setSender(Sender sender) {
        mControllerStream.setSender(sender);
    }

    public void setVideoConfiguration() {
        mControllerStream.setVideoConfiguration();
    }

    public void setCameraConfiguration() {
        Cameras.instance().setConfiguration();
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

    public void start() {
        ControlThread.get().post(new Runnable() {
            @Override
            public void run() {
                final int result = check();
                if (result == NO_ERROR) {
                    if (mLivingStartListener != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mLivingStartListener.startSuccess();
                            }
                        });
                    }
                    chooseVoiceMode();
                    screenOn();
                    mControllerStream.start();
                } else {
                    if (mLivingStartListener != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mLivingStartListener.startError(result);
                            }
                        });
                    }
                }
            }
        });
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

    public void stop() {
        screenOff();
        mControllerStream.stop();
        setAudioNormal();
    }

    private void screenOn() {
        if (mWakeLock != null) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        }
    }

    private void screenOff() {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    public void pause() {
        mControllerStream.pause();
    }

    public void resume() {
        mControllerStream.resume();
    }

    public boolean setVideoBps(int bps) {
        return mControllerStream.setVideoBps(bps);
    }

    private boolean isCameraOpen() {
        return mRenderer.isCameraOpen();
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        mOutCameraOpenListener = cameraOpenListener;
    }

    public void release() {
        screenOff();
        mWakeLock = null;
        Cameras.instance().releaseCamera();
        Cameras.instance().release();
        setAudioNormal();
    }

    private CameraListener mCameraOpenListener = new CameraListener() {
        @Override
        public void onOpenSuccess() {
            if (mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onOpenSuccess();
            }
        }

        @Override
        public void onOpenFail(int error) {
            if (mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onOpenFail(error);
            }
        }

        @Override
        public void onCameraChange() {
            // Won't Happen
        }
    };

    private void setAudioNormal() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
    }
}
