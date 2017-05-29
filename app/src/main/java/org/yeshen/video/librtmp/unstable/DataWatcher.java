package org.yeshen.video.librtmp.unstable;

import android.media.MediaCodec;

import org.yeshen.video.librtmp.App;
import org.yeshen.video.librtmp.unstable.net.packer.Packer;
import org.yeshen.video.librtmp.unstable.net.sender.Sender;
import org.yeshen.video.librtmp.core.IAudio;
import org.yeshen.video.librtmp.core.IRenderer;
import org.yeshen.video.librtmp.core.IVideo;
import org.yeshen.video.librtmp.core.delegate.IAudioEncodeDelegate;
import org.yeshen.video.librtmp.core.delegate.ILivingStartDelegate;
import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;
import org.yeshen.video.librtmp.unstable.tools.AndroidUntil;
import org.yeshen.video.librtmp.unstable.tools.AndroidWakeLock;
import org.yeshen.video.librtmp.unstable.tools.GlobalAsyncThread;

import java.nio.ByteBuffer;

import static org.yeshen.video.librtmp.core.IRenderer.NO_ERROR;

public class DataWatcher implements IAudioEncodeDelegate, IVideoEncodeDelegate, Packer.OnPacketListener {
    private IRenderer mRenderer;
    private Packer mPacker;
    private Sender mSender;
    private IVideo mVideoController;
    private IAudio mAudioController;
    public ILivingStartDelegate delegate;
    private AndroidWakeLock mWakeLock;

    public DataWatcher(IRenderer mRenderer) {
        this.mRenderer = mRenderer;
        this.mAudioController = new ControllerAudio();
        this.mVideoController = new ControllerCameraVideo(mRenderer);
        this.mWakeLock = new AndroidWakeLock(App.getInstance());
    }

    public void syncVideoConfig() {
        mVideoController.setVideoConfiguration();
    }

    public void setPacker(Packer packer) {
        mPacker = packer;
        mPacker.setPacketListener(this);
    }

    public void setSender(Sender sender) {
        mSender = sender;
    }

    public synchronized void start() {
        mWakeLock.acquire();
        GlobalAsyncThread.post(new Runnable() {
            @Override
            public void run() {
                final int check = mRenderer.checkStatus();
                if (check != NO_ERROR) {
                    if (delegate != null) {
                        delegate.error(check);
                    }
                }

                if (mPacker == null) return;
                if (mSender == null) return;

                mPacker.start();
                mSender.start();
                mVideoController.setVideoEncoderListener(DataWatcher.this);
                mAudioController.setAudioEncodeListener(DataWatcher.this);
                mAudioController.start();
                mVideoController.start();

                if (delegate != null) {
                    delegate.success();
                }
            }
        });
    }

    public synchronized void stop() {
        mWakeLock.release();
        GlobalAsyncThread.post(new Runnable() {
            @Override
            public void run() {
                mVideoController.setVideoEncoderListener(null);
                mAudioController.setAudioEncodeListener(null);
                mAudioController.stop();
                mVideoController.stop();
                if (mSender != null) {
                    mSender.stop();
                }
                if (mPacker != null) {
                    mPacker.stop();
                }
            }
        });
    }

    public synchronized void pause() {
        GlobalAsyncThread.post(new Runnable() {
            @Override
            public void run() {
                mAudioController.pause();
                mVideoController.pause();
            }
        });
    }

    public synchronized void resume() {
        GlobalAsyncThread.post(new Runnable() {
            @Override
            public void run() {
                mAudioController.resume();
                mVideoController.resume();
            }
        });
    }

    public synchronized void destroy() {
        if (mWakeLock != null) mWakeLock.release();
        mWakeLock = null;
        Cameras.instance().releaseCamera();
        Cameras.instance().release();
        AndroidUntil.disableVoiceMode();
    }

    public boolean setVideoBps(int bps) {
        return mVideoController.setVideoBps(bps);
    }

    @Override
    public void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mPacker != null) {
            mPacker.onAudioData(bb, bi);
        }
    }

    @Override
    public void onVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mPacker != null) {
            mPacker.onVideoData(bb, bi);
        }
    }

    @Override
    public void onPacket(byte[] data, int packetType) {
        if (mSender != null) {
            mSender.onData(data, packetType);
        }
    }
}
