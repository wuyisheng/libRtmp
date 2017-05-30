package org.yeshen.video.librtmp.core;

import android.support.annotation.WorkerThread;

import org.yeshen.video.librtmp.unstable.net.sender.rtmp.io.RtmpConnectListener;
import org.yeshen.video.librtmp.unstable.net.sender.sendqueue.ISendQueue;

/*********************************************************************
 * Created by yeshen on 2017/05/30.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public interface IRtmpConnection {

    //base function
    @WorkerThread
    void connect(String url);

    void publishAudioData(byte[] data, int type);

    void publishVideoData(byte[] data, int type);

    void stop();


    //setting
    void setConnectListener(RtmpConnectListener listener);

    void setSendQueue(ISendQueue sendQueue);

    void setVideoParams(int width, int height);

    void setAudioParams(int sampleRate, int sampleSize, boolean isStereo);

    enum State {
        INIT,
        HANDSHAKE,
        CONNECTING,
        CREATE_STREAM,
        PUBLISHING,
        LIVING
    }
}
