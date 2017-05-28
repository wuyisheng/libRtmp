package org.yeshen.video.librtmp.android;

import android.annotation.TargetApi;
import android.media.MediaCodec;

import org.yeshen.video.librtmp.afix.AndroidUntil;
import org.yeshen.video.librtmp.afix.interfaces.OnAudioEncodeListener;

import java.nio.ByteBuffer;

/**
 * @Title: AudioEncoder
 * @Package com.laifeng.sopcastsdk.audio
 * @Description:
 * @Author Jim
 * @Date 16/9/19
 * @Time 上午9:57
 * @Version
 */
@TargetApi(18)
public class AudioEncoder {
    private MediaCodec mMediaCodec;
    private OnAudioEncodeListener mListener;
    MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    public void setOnAudioEncodeListener(OnAudioEncodeListener listener) {
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