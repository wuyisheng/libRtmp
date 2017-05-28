package org.yeshen.video.librtmp.afix;

import android.media.MediaCodec;

import org.yeshen.video.librtmp.core.IAudio;
import org.yeshen.video.librtmp.core.IVideo;
import org.yeshen.video.librtmp.core.delegate.IAudioEncodeDelegate;
import org.yeshen.video.librtmp.core.delegate.IVideoEncodeDelegate;
import org.yeshen.video.librtmp.afix.net.packer.Packer;
import org.yeshen.video.librtmp.afix.net.sender.Sender;

import java.nio.ByteBuffer;

public class StreamController implements IAudioEncodeDelegate, IVideoEncodeDelegate, Packer.OnPacketListener{
    private Packer mPacker;
    private Sender mSender;
    private IVideo mVideoController;
    private IAudio mAudioController;

    public StreamController(IVideo videoProcessor, IAudio audioProcessor) {
        mAudioController = audioProcessor;
        mVideoController = videoProcessor;
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
