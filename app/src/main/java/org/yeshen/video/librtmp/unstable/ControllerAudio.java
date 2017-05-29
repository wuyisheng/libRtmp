package org.yeshen.video.librtmp.unstable;

import android.annotation.TargetApi;
import android.media.AudioRecord;
import android.media.MediaCodec;

import org.yeshen.video.librtmp.core.IAudio;
import org.yeshen.video.librtmp.core.delegate.IAudioEncodeDelegate;
import org.yeshen.video.librtmp.unstable.tools.AndroidUntil;
import org.yeshen.video.librtmp.unstable.tools.Lg;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ControllerAudio implements IAudio {
    private IAudioEncodeDelegate mListener;
    private AudioRecord mAudioRecord;
    private AudioProcessor mAudioProcessor;
    private boolean mMute;

    public ControllerAudio() {
    }

    public void setAudioEncodeListener(IAudioEncodeDelegate listener) {
        mListener = listener;
    }

    public void start() {
        AndroidUntil.enableVocieMode();
        mAudioRecord = AndroidUntil.getAudioRecord();
        try {
            mAudioRecord.startRecording();
        } catch (Exception e) {
            Lg.e(e);
        }
        mAudioProcessor = new AudioProcessor(mAudioRecord);
        mAudioProcessor.setAudioHEncodeListener(mListener);
        mAudioProcessor.start();
        mAudioProcessor.setMute(mMute);
    }

    public void stop() {
        if(mAudioProcessor != null) {
            mAudioProcessor.stopEncode();
        }
        if(mAudioRecord != null) {
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        AndroidUntil.disableVoiceMode();
    }

    public void pause() {
        if(mAudioRecord != null) {
            mAudioRecord.stop();
        }
        if (mAudioProcessor != null) {
            mAudioProcessor.pauseEncode(true);
        }
    }

    public void resume() {
        if(mAudioRecord != null) {
            mAudioRecord.startRecording();
        }
        if (mAudioProcessor != null) {
            mAudioProcessor.pauseEncode(false);
        }
    }

    @Override
    public void mute(boolean mute) {
        mMute = mute;
        if(mAudioProcessor != null) {
            mAudioProcessor.setMute(mMute);
        }
    }

    @Override
    @TargetApi(16)
    public int getSessionId() {
        if(mAudioRecord != null) {
            return mAudioRecord.getAudioSessionId();
        } else {
            return -1;
        }
    }

    private static class AudioProcessor extends Thread {
        private volatile boolean mPauseFlag;
        private volatile boolean mStopFlag;
        private volatile boolean mMute;
        private AudioRecord mAudioRecord;
        private AudioEncoder mAudioEncoder;
        private byte[] mRecordBuffer;
        private int mRecordBufferSize;

        AudioProcessor(AudioRecord audioRecord) {
            mRecordBufferSize = AndroidUntil.getRecordBufferSize();
            mRecordBuffer =  new byte[mRecordBufferSize];
            mAudioRecord = audioRecord;
            mAudioEncoder = new AudioEncoder();
            mAudioEncoder.prepareEncoder();
        }

        void setAudioHEncodeListener(IAudioEncodeDelegate listener) {
            mAudioEncoder.setOnAudioEncodeListener(listener);
        }

        void stopEncode() {
            mStopFlag = true;
            if(mAudioEncoder != null) {
                mAudioEncoder.stop();
                mAudioEncoder = null;
            }
        }

        void pauseEncode(boolean pause) {
            mPauseFlag = pause;
        }

        void setMute(boolean mute) {
            mMute = mute;
        }

        public void run() {
            while (!mStopFlag) {
                while (mPauseFlag) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int readLen = mAudioRecord.read(mRecordBuffer, 0, mRecordBufferSize);
                if (readLen > 0) {
                    if (mMute) {
                        byte clearM = 0;
                        Arrays.fill(mRecordBuffer, clearM);
                    }
                    if(mAudioEncoder != null) {
                        mAudioEncoder.offerEncoder(mRecordBuffer);
                    }
                }
            }
        }
    }

    @TargetApi(18)
    private static class AudioEncoder {
        private MediaCodec mMediaCodec;
        private IAudioEncodeDelegate mListener;
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

        public void setOnAudioEncodeListener(IAudioEncodeDelegate listener) {
            mListener = listener;
        }

        public AudioEncoder() {
        }

        void prepareEncoder() {
            mMediaCodec = AndroidUntil.getAudioMediaCodec();
            mMediaCodec.start();
        }

        synchronized public void stop() {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
        }

        @SuppressWarnings("deprecation")
        synchronized void offerEncoder(byte[] input) {
            if(mMediaCodec == null) {
                return;
            }
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(12000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
            }

            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if(mListener != null) {
                    mListener.onAudioEncode(outputBuffer, mBufferInfo);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
            }
        }
    }
}
