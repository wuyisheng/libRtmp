package org.yeshen.video.librtmp.android;

import android.media.MediaCodec;

import org.yeshen.video.librtmp.afix.ControlThread;
import org.yeshen.video.librtmp.afix.interfaces.IAudioController;
import org.yeshen.video.librtmp.afix.interfaces.IVideoController;
import org.yeshen.video.librtmp.afix.interfaces.OnAudioEncodeListener;
import org.yeshen.video.librtmp.afix.interfaces.OnVideoEncodeListener;
import org.yeshen.video.librtmp.net.packer.Packer;
import org.yeshen.video.librtmp.net.sender.Sender;

import java.nio.ByteBuffer;

/**
 * @Title: StreamController
 * @Package com.laifeng.sopcastsdk.controller
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 上午11:44
 * @Version
 */

public class StreamController implements OnAudioEncodeListener, OnVideoEncodeListener, Packer.OnPacketListener{
    private Packer mPacker;
    private Sender mSender;
    private IVideoController mVideoController;
    private IAudioController mAudioController;

    public StreamController(IVideoController videoProcessor, IAudioController audioProcessor) {
        mAudioController = audioProcessor;
        mVideoController = videoProcessor;
    }

    public void setVideoConfiguration() {
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
        ControlThread.get().post(new Runnable() {
            @Override
            public void run() {
                if(mPacker == null) {
                    return;
                }
                if(mSender == null) {
                    return;
                }
                mPacker.start();
                mSender.start();
                mVideoController.setVideoEncoderListener(StreamController.this);
                mAudioController.setAudioEncodeListener(StreamController.this);
                mAudioController.start();
                mVideoController.start();
            }
        });
    }

    public synchronized void stop() {
        ControlThread.get().post(new Runnable() {
            @Override
            public void run() {
                mVideoController.setVideoEncoderListener(null);
                mAudioController.setAudioEncodeListener(null);
                mAudioController.stop();
                mVideoController.stop();
                if(mSender != null) {
                    mSender.stop();
                }
                if(mPacker != null) {
                    mPacker.stop();
                }
            }
        });
    }

    public synchronized void pause() {
        ControlThread.get().post(new Runnable() {
            @Override
            public void run() {
                mAudioController.pause();
                mVideoController.pause();
            }
        });
    }

    public synchronized void resume() {
        ControlThread.get().post(new Runnable() {
            @Override
            public void run() {
                mAudioController.resume();
                mVideoController.resume();
            }
        });
    }

    void mute(boolean mute) {
        mAudioController.mute(mute);
    }

    int getSessionId() {
        return mAudioController.getSessionId();
    }

    public boolean setVideoBps(int bps) {
        return mVideoController.setVideoBps(bps);
    }

    @Override
    public void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if(mPacker != null) {
            mPacker.onAudioData(bb, bi);
        }
    }

    @Override
    public void onVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if(mPacker != null) {
            mPacker.onVideoData(bb, bi);
        }
    }

    @Override
    public void onPacket(byte[] data, int packetType) {
        if(mSender != null) {
            mSender.onData(data, packetType);
        }
    }
}
